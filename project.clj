(defproject lupapiste/pdfa-generator "1.1.1"
  :description "Wrapper for clj-pdf for PDF/A document generation"
  :url "http://www.lupapiste.fi"
  :license {:name         "GNU Lesser General Public License - v 3"
            :url          "http://www.gnu.org/licenses/lgpl.html"
            :distribution :repo}
  :scm {:url "https://github.com/lupapiste/pdfa-generator.git"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [com.taoensso/timbre "5.2.1"]
                 [lupapiste/openpdf "1.0.6"]
                 [clj-pdf "2.2.8" :exclusions [itext com.lowagie/itext]]]
  :plugins [[lein-midje "3.2.2"]]
  :profiles {:dev      {:dependencies [[midje "1.10.10"]
                                       [flare "0.2.9"]
                                       [org.apache.pdfbox/pdfbox "2.0.3"]]
                        :injections   [(require 'flare.clojure-test)
                                       (flare.clojure-test/install!)]}})
