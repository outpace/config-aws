(set-env!
  :source-paths #{"src"}
  :resource-paths #{"resources"}
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

(deftask build
  "Builds the project.  In particular, compiles the Java source."
  []
  (javac))

(deftask with-testing
  "Sets up environment for running tests."
  []
  (set-env! :source-paths #(conj % "test"))
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

(deftask dev
  "Runs in development mode, which includes a CIDER-enable nREPL server with
  automatic retesting and namespace refreshment."
  []
  (set-env! :dependencies #(into % '[[samestep/boot-refresh "0.1.0"]
                                     [tolitius/boot-check "0.1.9"]
                                     ]))
  (require 'samestep.boot-refresh)
  (require 'tolitius.boot-check)
  (let [refresh (resolve 'samestep.boot-refresh/refresh)
        with-bikeshed (resolve 'tolitius.boot-check/with-bikeshed)
        with-eastwood (resolve 'tolitius.boot-check/with-eastwood)
        with-kibit (resolve 'tolitius.boot-check/with-kibit)
        with-yagni (resolve 'tolitius.boot-check/with-yagni)
        ]
    (comp (with-dev)
          (repl :server true)
          (watch :verbose true)
          (test)
          #_(refresh :help true)
          #_(with-bikeshed)
          #_(with-eastwood)
          #_(with-kibit)
          #_(with-yagni)
          )))
