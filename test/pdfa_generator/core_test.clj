(ns pdfa-generator.core-test
  (:require [midje.sweet :refer :all]
            [taoensso.timbre :refer [trace tracef debug debugf info infof warn warnf error errorf fatal fatalf]]
            [pdfa-generator.core :as pdfa]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (org.apache.pdfbox Loader)
           (org.apache.pdfbox.rendering PDFRenderer)
           (org.apache.pdfbox.text PDFTextStripper)
           (java.io File ByteArrayOutputStream)
           (javax.imageio ImageIO)
           [java.util Base64]
           [java.awt Toolkit]
           [com.lowagie.text Image]))

;; Change this to leave the files after test for manual inspection
(def delete-files true)

(facts "Valid embedded Fonts "
       (fact "default text is opaque" (.getAlpha (.getColor (pdfa/font {}))) => 255)
       (fact "default Font loads  " (.getPostscriptFontName (.getBaseFont (pdfa/font {:size 22}))) => "LiberationSans")
;       (fact "Given existing font loads" (.getPostscriptFontName (.getBaseFont (pdfa/font {:size 22 :ttf-name "liberation-fonts-ttf-2.00.1/LiberationSans-Regular.ttf"}))) => "LiberationSans")
       (fact "Given non-existing font throws exception " (.getPostscriptFontName (.getBaseFont (pdfa/font {:size 22 :ttf-name "fonts/n019003lXXXX.afm"}))) => (throws IllegalArgumentException))
       (fact "Given illegal font-file extension throws exception " (.getPostscriptFontName (.getBaseFont (pdfa/font {:size 22 :ttf-name "fonts/n019003l.txt"}))) => (throws IllegalArgumentException))
       )

(facts "Valid PDF/A metadata "
       (let [pdf-markup [{:title  "Lupapiste.fi"
                          :size   "a4"
                          :footer {:text  (clojure.string/join " - " ["Lupapiste.fi"
                                                                      "dd.MM.yyyy HH:mm" (str (System/currentTimeMillis))
                                                                      "application.export.page"])
                                   :align :right}
                          :pages  true}
                         [:image {:xscale 1 :yscale 1} (ImageIO/read (io/resource "logo-v2-flat.png"))]
                         [:spacer]
                         [:heading {:style {:size 20}} "lang + tos"]
                         [:spacer]
                         [:line]
                         [:spacer]
                         [:list {:symbol "…"}
                          [:paragraph
                           "Rakentaminen, ylläpito ja käyttö (3)"
                           [:list
                            [:paragraph
                             "Rakennusvalvonta (0)"
                             [:list
                              [:paragraph
                               "Rakennuslupamenettely (1)"
                               [:list
                                [:paragraph
                                 "Neuvonta ()"
                                 [:list
                                  [:paragraph
                                   "Asiakkaan yhteydenotto asiointipalvelussa ()"
                                   [:list
                                    [:paragraph "todo localize: :ilmoitus" nil]]]]]
                                [:paragraph
                                 "Käsittelyssä ()"
                                 [:list
                                  [:paragraph
                                   "Hakemus jätetään käsittelyyn ()"
                                   [:list
                                    [:paragraph "todo localize: :hakemus" nil]
                                    [:paragraph
                                     "todo localize: :muut.muu"
                                     nil]]]]]]]]]]]]]
             file (File/createTempFile "test-pdf-core-" ".pdf")]
         (with-open [out (io/output-stream file)]
           (pdfa/pdf pdf-markup out))

         (with-open [doc (Loader/loadPDF file)]
           (let [info (.getDocumentInformation doc)]
             (fact "title" (.getTitle info) => "Lupapiste.fi")
             (fact "keywords" (.getKeywords info) => "Lupapiste.fi, XMP, Metadata")))
         (if delete-files
           (.delete file)
           (debug "**  Created PDF file: " (.getAbsolutePath file)))))

#_(facts "rtf to pdf/a"
         (debug "env: " (System/getenv "os.name"))
         (System/setProperty "SystemRoot" "/usr/share")
         (.exec (Runtime/getRuntime) "env --unset \"BASH_FUNC_mc%%\"") ;

         (let [file (File. "/home/michaelho/Documents/Lounaslista 2015 vk46.rtf")
               fis (FileInputStream. file)
               parser (RtfParser. nil)
               doc (Document.)
               output-stream (FileOutputStream. "/tmp/test-rtf-to-pdf.pdf")
               pdf-writer (PdfWriter/getInstance doc output-stream)
               ]
           (.open doc)
           (.convertRtfDocument parser fis doc)
           (.close doc)
           )
         )

(facts "base64"
  (let [logo (str/trim-newline (slurp (io/resource "lupapiste-logo-base64.txt")))
        as-bytes (.decode (Base64/getDecoder) logo)]
    (fact "image created"
      (Image/getInstance (.createImage (Toolkit/getDefaultToolkit) as-bytes) nil) => truthy)))

(facts "SVG rendering with the current clj-pdf API"
  (let [svg "<svg xmlns='http://www.w3.org/2000/svg' width='40' height='40'><rect width='40' height='40' fill='#ff0000'/></svg>"
        out (ByteArrayOutputStream.)]
    (pdfa/pdf [{:footer false}
               [:svg svg]
               [:pagebreak]
               [:svg svg]] out)
    (with-open [doc (Loader/loadPDF (.toByteArray out))]
      (fact "both pages exist" (.getNumberOfPages doc) => 2)
      (doseq [page [0 1]]
        (let [rendered (.renderImage (PDFRenderer. doc) page)]
          (fact "SVG is painted on each page" (.getRGB rendered 20 20) => (.getRGB java.awt.Color/RED)))))))

(facts "Page numbering survives PDFBox and OpenPDF upgrades"
  (let [out (ByteArrayOutputStream.)]
    (pdfa/pdf [{:pages true :footer "Page"}
               [:paragraph "First page"]
               [:pagebreak]
               [:paragraph "Second page"]] out)
    (with-open [doc (Loader/loadPDF (.toByteArray out))]
      (let [text (.getText (PDFTextStripper.) doc)]
        (fact "page count" (.getNumberOfPages doc) => 2)
        (fact "first page footer" text => (contains "Page 1 / 2"))
        (fact "second page footer" text => (contains "Page 2 / 2"))))))

(facts "Phrases accept text and elements with OpenPDF 2"
  (let [out (ByteArrayOutputStream.)]
    (pdfa/pdf [{:footer false}
               [:paragraph [:phrase "Plain " nil 42 " "
                            [:chunk {:style :bold} "bold"]
                            [:phrase " nested"]]]
               [:pdf-table {} [1]
                [[:pdf-table {} [1]
                  [[:pdf-cell "Nested cell text"]]]]]] out)
    (with-open [doc (Loader/loadPDF (.toByteArray out))]
      (let [text (.getText (PDFTextStripper.) doc)]
        (fact "mixed phrase content is preserved"
          text => (contains "Plain 42 bold nested"))
        (fact "explicit string cells in nested tables render"
          text => (contains "Nested cell text"))))))
