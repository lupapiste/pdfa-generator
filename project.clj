(defproject pdfa-core "0.0.4"
  :description "Wrapper for clj-pdf for PDF/A document generation"
  :url "http://www.lupapiste.fi"
  :license {:name "LGPL 3"
            :url ""
            :distribution :repo}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.taoensso/timbre "4.2.1"]
                 [org.clojure/tools.nrepl "0.2.12"]
                 [lupapiste/openpdf "1.0.2"]
                 [clj-pdf "2.0.9" :exclusions [itext]]
                 [org.slf4j/slf4j-log4j12 "1.7.16"]]
  :plugins [[lein-midje "3.2"]]
  :profiles {:dev {:dependencies [[midje "1.8.3"]
                                  [flare "0.2.9"]
                                  [org.apache.pdfbox/pdfbox "1.8.11"]]
                   :injections [(require 'flare.clojure-test)
                                (flare.clojure-test/install!)]}})
