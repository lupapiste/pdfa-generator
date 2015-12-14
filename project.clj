(defproject pdfa-core "0.0.4"
  :description "Port of clj-pdf"
  :url "http://www.solita.fi"
  :license {:name "L GPL 3"
            :url ""
            :distribution :repo}
  :scm {:url "https://github.com/lupapiste/pdfa-core.git"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [com.taoensso/timbre "4.1.1"]
                 [org.clojure/tools.nrepl "0.2.10"]
                 [clj-pdf "1.11.21" :exclusions [xalan org.apache.xmlgraphics/batik-js com.lowagie/itext]]
                 [org.slf4j/slf4j-log4j12 "1.7.7"]
                 [itext "4.2.1" :exclusions [org.bouncycastle/bctsp-jdk14 xml-apis]]
                 [org.apache.pdfbox/pdfbox "1.8.9"]]
  :plugins [[lein-midje "3.1.1"]
            [com.jakemccrary/lein-test-refresh "0.8.0"]]

  :profiles {:dev {:dependencies [[midje "1.7.0" :exclusions [org.clojure/tools.namespace]]
                                  [flare "0.2.9"]]
                   :injections [(require 'flare.clojure-test)
                                (flare.clojure-test/install!)]}}
  :cljsbuild {:builds {:dev {:source-paths ["src"]}}}
)