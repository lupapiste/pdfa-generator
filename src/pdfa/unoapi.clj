(ns pdfa.unoapi)

#_(defn uno-connection
  (let [xContext (com.sun.star.comp.helper.Bootstrap/bootstrap)
        _ (println "Connected to a running office ...")
        xMCF (.getServiceManager xContext)]
    (println "remote ServiceManager is " (not (nil? xMCF))))))

