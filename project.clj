(defproject lupapiste/pdfa-generator "1.1.3"
  :description "Wrapper for clj-pdf for PDF/A document generation"
  :url "http://www.lupapiste.fi"
  :license {:name         "GNU Lesser General Public License - v 3"
            :url          "http://www.gnu.org/licenses/lgpl.html"
            :distribution :repo}
  :scm {:url "https://github.com/lupapiste/pdfa-generator.git"}
  :dependencies [[org.clojure/clojure "1.12.6"]
                 [com.taoensso/timbre "6.8.0"]
                 ;; clj-pdf 2.8.2 uses the com.lowagie API, removed in OpenPDF 3.
                 [com.github.librepdf/openpdf "2.2.5" :upgrade false]
                 [commons-codec "1.23.0"]
                 [org.jfree/jfreechart "1.5.6"]
                 [org.apache.xmlgraphics/batik-bridge "1.20"]
                 [org.apache.xmlgraphics/batik-anim "1.20"]
                 [org.apache.xmlgraphics/batik-codec "1.20"]
                 [org.apache.xmlgraphics/xmlgraphics-commons "2.12"]
                 [clj-pdf "2.8.2"]]
  :plugins [[lein-midje "3.2.2"]]
  :profiles {:dev      {:dependencies [[midje "1.10.10"]
                                       [flare "0.2.9"]
                                       [org.apache.pdfbox/pdfbox "3.0.8"]]
                        :injections   [(require 'flare.clojure-test)
                                       (flare.clojure-test/install!)]}})
