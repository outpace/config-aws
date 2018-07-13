(ns outpace.config-aws-test
  (:require [clojure.test :as t :refer [deftest is testing]]
            [outpace.config :as config]
            [outpace.config-aws :as config-aws])
  (:import (cloud.localstack Localstack OutpaceUtil)
           (com.amazonaws.services.simplesystemsmanagement.model DeleteParameterRequest
                                                                 PutParameterRequest)
           (outpace.config_aws SsmVal)))

(defn ^:private with-localstack*
  "Runs the given function within the scope of a localstack infrastructure."
  [f]
  (try
    (OutpaceUtil/setup)
    (let [endpoint {:service-endpoint (Localstack/getEndpointSSM)
                    :signing-region (Localstack/getDefaultRegion)}]
      (with-redefs [config-aws/ssm-client-args {:endpoint endpoint}]
        (f)))
    (finally
      (OutpaceUtil/teardown))))

(defmacro ^:private with-localstack
  [& body]
  `(with-localstack*
     (fn [] (do ~@body))))

(defn ^:private with-ssm-params*
  [params f]
  (try
    (reduce-kv (fn [_ k v]
                 (let [request (.. (PutParameterRequest.)
                                   (withName k)
                                   (withValue v))]
                   (.putParameter @@#'config-aws/ssm-client request)))
               nil
               params)
    (f)
    (finally
      (reduce (fn [_ k]
                (let [request (.. (DeleteParameterRequest.)
                                  (withName k))]
                   (.deleteParameter @@#'config-aws/ssm-client request)))
              nil
              (keys params)))))

(defmacro ^:private with-ssm-params
  [params & body]
  `(with-ssm-params* ~params (fn [] (do ~@body))))

(deftest test-read-ssm
  (with-localstack
    (with-ssm-params {"/foo/bar" "blah"}
      (testing "reading a name string"
        (testing "in AWS"
          (let [v (config-aws/read-ssm "/foo/bar")]
            (is (true? (config/provided? v)))
            (is (= "blah" (config/extract v)))
            (is (= "#config-aws/ssm \"/foo/bar\""
                   (pr-str v)))))
        (testing "missing from AWS"
          (let [v (config-aws/read-ssm "/foo/baz")]
            (is (false? (config/provided? v)))
            (is (nil? (config/extract v)))
            (is (= "#config-aws/ssm \"/foo/baz\""
                   (pr-str v))))))
      (testing "reading a path vector"
        (testing "in AWS"
          (let [v (config-aws/read-ssm ["/foo" "/bar"])]
            (is (true? (config/provided? v)))
            (is (= "blah" (config/extract v)))
            (is (= "#config-aws/ssm [\"/foo\" \"/bar\"]"
                   (pr-str v)))))
        (testing "in AWS (using a #config/property)"
          (System/setProperty "foo.path" "/foo")
          (let [v (config-aws/read-ssm [#config/property "foo.path" "/bar"])]
            (is (true? (config/provided? v)))
            (is (= "blah" (config/extract v)))
            (is (= "#config-aws/ssm [#config/property \"foo.path\" \"/bar\"]"
                   (pr-str v)))))
        (testing "missing from AWS"
          (let [v (config-aws/read-ssm ["/foo" "/baz"])]
            (is (false? (config/provided? v)))
            (is (nil? (config/extract v)))
            (is (= "#config-aws/ssm [\"/foo\" \"/baz\"]"
                   (pr-str v))))))))
  (testing "invalid arguments"
    (is (thrown? IllegalArgumentException (config-aws/read-ssm 42)))))
