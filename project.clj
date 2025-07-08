(defproject vkteams-bot "0.1.0-SNAPSHOT"
  :description "VK Teams Bot API Client for Clojure"
  :url "https://github.com/bug-ops/vkteams-bot-clj"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [http-kit "2.7.0"]
                 [cheshire "5.12.0"]
                 [environ "1.2.0"]
                 [clojure.java-time "1.4.2"]
                 [org.clojure/tools.logging "1.3.0"]
                 [ch.qos.logback/logback-classic "1.4.14"]
                 [manifold "0.4.2"]
                 [clj-commons/clj-yaml "1.0.27"]]
  :main ^:skip-aot vkteams-bot.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
             :dev {:dependencies [[midje "1.10.10"]]
                   :plugins [[lein-midje "3.2.1"]]}})
