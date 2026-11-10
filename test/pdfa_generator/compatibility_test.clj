(ns pdfa-generator.compatibility-test
  (:require [clojure.java.io :as io]
            [midje.sweet :refer :all]
            [pdfa-generator.core :as pdfa])
  (:import [java.io ByteArrayOutputStream]
           [org.apache.pdfbox Loader]
           [org.apache.pdfbox.rendering PDFRenderer]
           [org.apache.pdfbox.text PDFTextStripper]))

(defn- generate [markup]
  (let [out (ByteArrayOutputStream.)]
    (pdfa/pdf markup out)
    (.toByteArray out)))

(defn- document-text [markup]
  (with-open [doc (Loader/loadPDF (generate markup))]
    (.getText (PDFTextStripper.) doc)))

(facts "Legacy table alignment uses the OpenPDF enum API"
  (doseq [[align expected] [[:left 0] [:center 1] [:right 2] ["right" 2] [nil 0]]]
    (let [table (#'pdfa/make-section [:table {:align align}
                                    [[:cell {:align align} "Cell"]]])]
      (fact "table alignment" (.getAlignment table) => expected)
      (fact "cell alignment" (.getHorizontalAlignment (.getElement table 0 0)) => expected)))
  (fact "single spanning header"
    (document-text [{:footer false}
                    [:table {:header ["Heading"] :num-cols 2}
                     ["Left" "Right"]]]) => (contains "Heading"))
  (fact "multiple styled headers and explicit cells"
    (document-text [{:footer false}
                    [:table {:header [[:phrase {:align :right} "First"] "Second"]
                             :widths [1 2] :color [230 230 230]}
                     [[:cell {:align :center} "Body"] nil]]]) => (contains "Body")))

(facts "Scalar content is converted to embedded text elements"
  (doseq [element [[:paragraph "Value " 42 nil]
                   [:list "Value" 42 nil]
                   [:chapter "Chapter" [:section "Section" 42 nil]]
                   [:table [[:cell 42] [:cell nil]]]
                   [:pdf-table {} [1 1] [[:pdf-cell 42] [:pdf-cell nil]]]]]
    (fact {:element element}
      (document-text [{:footer false} element]) => (contains "42"))))

(facts "PDF table backgrounds are applied to cells"
  (let [table (#'pdfa/make-section
                [:pdf-table {:color [255 0 0]} [1 1]
                 ["Default" [:pdf-cell {:color [0 0 255]} "Override"]]])
        cells (.getCells (.getRow table 0))]
    (fact "table background" (.getBackgroundColor (aget cells 0)) => java.awt.Color/RED)
    (fact "cell override" (.getBackgroundColor (aget cells 1)) => java.awt.Color/BLUE))
  (fact "colored table generates a readable PDF"
    (document-text [{:footer false}
                    [:pdf-table {:color [255 0 0]} [1] ["Colored cell"]]]) => (contains "Colored cell")))

(facts "Headers and Java2D text use embedded fonts"
  (doseq [[meta element expected]
          [[{:header "Header"} [:paragraph "Body"] "Header"]
           [{:footer false} [:graphics (fn [g] (.drawString g "Graphics text" 20 40))] "Graphics text"]]]
    (with-open [doc (Loader/loadPDF (generate [meta element]))]
      (fact "text survives rendering" (.getText (PDFTextStripper.) doc) => (contains expected))
      (let [resources (.getResources (.getPage doc 0))]
        (doseq [name (.getFontNames resources)]
          (fact "font is embedded" (.isEmbedded (.getFont resources name)) => true))))))

(facts "Chart rendering works with upgraded JFreeChart and OpenPDF"
  (doseq [[type data] [[:line-chart [["Series" [0 1] [1 2]]]]
                      [:bar-chart [[1 "First"] [2 "Second"]]]
                      [:pie-chart [["First" 1] ["Second" 2]]]]
          vector? [false true]]
    (let [chart (into [:chart {:type type :title "Chart title" :width 200 :height 100
                              :vector vector? :show-points true :point-labels true
                              :x-label "X" :y-label "Y"}] data)]
      (with-open [doc (Loader/loadPDF (generate [{:footer false} chart]))]
        (fact "one page" (.getNumberOfPages doc) => 1)
        (fact "PDFBox can render the chart" (.renderImage (PDFRenderer. doc) 0) => truthy)
        (when vector?
          (fact "vector text remains extractable"
            (.getText (PDFTextStripper.) doc) => (contains "Chart title")))))))

(facts "Vector charts are drawn on every page"
  (let [chart [:chart {:type :line-chart :vector true :title "Repeated chart"
                      :width 200 :height 100} ["Series" [0 1] [1 2]]]]
    (with-open [doc (Loader/loadPDF (generate [{:footer false} chart [:pagebreak] chart]))]
      (doseq [page [1 2]]
        (let [stripper (doto (PDFTextStripper.) (.setStartPage page) (.setEndPage page))]
          (fact "chart text exists on each page"
            (.getText stripper doc) => (contains "Repeated chart")))))))

(facts "Remaining element integrations generate readable PDFs"
  (doseq [element [[:list {:numbered true} "Content"]
                   [:list {:roman true} "Content"]
                   [:paragraph [:anchor {:target "https://example.com" :style :bold :leading 12} "Content"]]
                   [:paragraph [:chunk {:background [200 200 200]} "Content"]
                    [:superscript "2"] [:subscript "3"]]]]
    (fact "element text" (document-text [{:footer false} element]) => (contains "Content")))
  (fact "image annotation and scaling"
    (generate [{:footer false}
               [:image {:annotation ["Title" "Note"] :scale 25}
                (io/resource "logo-v2-flat.png")]]) => truthy)
  (fact "dotted lines"
    (document-text [{:footer false} [:paragraph "Content"] [:line {:dotted true :gap 2}]]) => (contains "Content")))

(facts "Custom Java2D font mappers are still honored"
  (let [calls (atom 0)
        mapper (reify com.lowagie.text.pdf.FontMapper
                 (awtToPdf [_ _]
                   (swap! calls inc)
                   (.getBaseFont (pdfa/font {})))
                 (pdfToAwt [_ _ size]
                   (java.awt.Font. "SansSerif" java.awt.Font/PLAIN (int size))))]
    (fact "custom mapper output"
      (document-text [{:footer false}
                      [:graphics {:font-mapper mapper}
                       (fn [g] (.drawString g "Custom font" 20 40))]]) => (contains "Custom font"))
    (fact "mapper was called" (pos? @calls) => true)))

;; These OpenPDF list types require unembedded built-in fonts. They were also
;; incompatible with PDF/A-1 in the old fork; keep the limitation explicit.
(facts "Symbol and dingbat lists cannot satisfy PDF/A font embedding"
  (doseq [options [{:greek true} {:dingbats true :dingbats-char-num 42}
                   {:dingbatsnumber true :dingbatsnumber-type 0}]]
    (fact "unembedded symbol fonts are rejected"
      (generate [{:footer false} [:list options "Item"]])
      => (throws Exception #"All the fonts must be embedded"))))
