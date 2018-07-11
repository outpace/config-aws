(defproject com.outpace/config-aws "0.1.0-SNAPSHOT"
  :description "Setting configuration vars from AWS."
  :url "https://github.com/outpace/config-aws"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [[com.amazonaws/aws-java-sdk-ssm "1.11.365"]
                 [com.outpace/config "0.12.0"]
                 [org.clojure/clojure "1.9.0" :scope "provided"]

                 [cloud.localstack/localstack-utils "0.1.14" :scope "test"]]
  :profiles {:test {:global-vars {*warn-on-reflection* true}
                    :java-source-paths ["test/"]}}
  :plugins [[lein-codox "0.10.4"]]
  :codox {:src-dir-uri "http://github.com/outpace/config/blob/master/"
          :src-linenum-anchor-prefix "L"}
  :deploy-repositories [["releases" :clojars]])
