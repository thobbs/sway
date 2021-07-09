(defproject sway "1.0"
  :description "Artwork for the GitHub Bellevue Office"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [genartlib/genartlib "0.1.22"]]
  :jvm-opts ["-XX:-OmitStackTraceInFastThrow"
             "-Dsun.java2d.uiScale=1.0"]
  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :main sway.runcore
  :aot [sway.dynamic])
