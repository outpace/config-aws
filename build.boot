(def +version+ "0.1.1-SNAPSHOT")

(set-env!
  :source-paths #{}
  :resource-paths #{"src" "resources"}
  :dependencies '[[com.amazonaws/aws-java-sdk-ssm "1.11.366"]
                  [com.outpace/config "0.12.0" :exclusions [org.clojure/clojure]]
                  [org.clojure/clojure "1.9.0" :scope "provided"]

                  ;; test dependencies
                  [cloud.localstack/localstack-utils "0.1.14" :scope "test"
                   :exclusions [commons-logging
                                joda-time]]]
  :repl-options {:nrepl-middleware '[cider.nrep/wrap-apropos
                                     cider.nrep/wrap-classpath
                                     cider.nrep/wrap-complete
                                     cider.nrep/wrap-content-type
                                     cider.nrep/wrap-debug
                                     cider.nrep/wrap-enlighten
                                     cider.nrep/wrap-format
                                     cider.nrep/wrap-info
                                     cider.nrep/wrap-inspect
                                     cider.nrep/wrap-macroexpand
                                     cider.nrep/wrap-ns
                                     cider.nrep/wrap-out
                                     cider.nrep/wrap-pprint
                                     cider.nrep/wrap-pprint-fn
                                     cider.nrep/wrap-profile
                                     cider.nrep/wrap-refresh
                                     cider.nrep/wrap-resource
                                     cider.nrep/wrap-slurp
                                     cider.nrep/wrap-spec
                                     cider.nrep/wrap-stacktrace
                                     cider.nrep/wrap-test
                                     cider.nrep/wrap-trace
                                     cider.nrep/wrap-tracker
                                     cider.nrep/wrap-undef
                                     cider.nrep/wrap-version]})

(task-options!
  push {:repo "clojars"}
  pom {:project 'com.outpace/config-aws
       :version +version+
       :description "AWS extensions to com.outpace/config"
       :url "https://github.com/outpace/config-aws"
       :scm {:url "https://github.com/outpace/config-aws"}
       :license {"EPL" "http://www.eclipse.org/legal/epl-v10.html"}})

(deftask build
  "Builds the project and installs it to the local repository."
  []
  (comp (pom)
        (jar)
        (install)))

(deftask with-testing
  "Sets up environment for running tests."
  []
  (set-env! :resource-paths #(conj % "test"))
  identity)

(ns-unmap *ns* 'test)

(deftask test
  "Runs the tests in the project."
  []
  (set-env! :dependencies #(conj % '[adzerk/boot-test "1.2.0"]))
  (require 'adzerk.boot-test)
  (let [test (resolve 'adzerk.boot-test/test)]
    (comp (build)
          (with-testing)
          (test))))

(deftask with-dev
  "Sets up the environment for developement by adding CIDER support and
  including the testing environment.."
  []
  (set-env! :dependencies #(into % '[[org.clojure/tools.nrepl "0.2.13"]
                                     [cider/cider-nrepl "0.17.0"]]))
  (require 'cider.tasks)
  (let [add-middleware (resolve 'cider.tasks/add-middleware)]
    (comp (add-middleware)
          (with-testing))))

(deftask with-localstack
  "Runs the boot tasks within the context of localstack running via
  docker-compose."
  []
  (comp (with-pre-wrap fileset
          (dosh "docker-compose" "up" "-d")
          (commit! fileset))
        (with-post-wrap fileset
          (dosh "docker-compose" "stop"))))

(deftask dev
  "Runs in development mode, which includes a CIDER-enable nREPL server with
  automatic retesting and namespace refreshment."
  []
  (comp (with-localstack)
        (with-dev)
        (repl :server true)
        (watch :verbose true)
        (test)))

(deftask deploy
  "Deploys the library to Clojars."
  []
  (comp (build)
        (push :tag true
              :ensure-branch "master"
              :ensure-clean true
              :ensure-release true
              :ensure-version +version+)))
