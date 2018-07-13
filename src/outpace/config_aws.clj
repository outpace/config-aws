(ns outpace.config-aws
  (:require [clojure.string :as str]
            [outpace.config :as config :refer [defconfig]])
  (:import (com.amazonaws.client.builder AwsClientBuilder$EndpointConfiguration)
           (com.amazonaws.services.simplesystemsmanagement AWSSimpleSystemsManagement
                                                           AWSSimpleSystemsManagementClientBuilder)
           (com.amazonaws.services.simplesystemsmanagement.model GetParameterRequest
                                                                 ParameterNotFoundException)))

(defn ^:private valid-ssm-client-args?
  "Validates the client-args using spec, if available."
  [client-args]
  (try
    (require 'outpace.config-aws.spec)
    (when-let [validate-fn @(resolve 'outpace.config-aws.spec/validate-client-args)]
      (validate-fn client-args))
    (catch clojure.lang.Compiler$CompilerException _
      true)))

(defconfig
  ^{:validate [valid-ssm-client-args? "Must be valid SSM client configuration."]}
  ssm-client-args
  "A map used to configure the SSM client.  Current valid configuration includes:

    :endpoint {:service-endpoint \"\"
               :signing-region \"\"}"
  {})

(defn ^:private build-client
  "Builds an SSM client form the given client arguments."
  [{:keys [endpoint] :as client-args}]
  (.build
    (cond-> (AWSSimpleSystemsManagementClientBuilder/standard)
      endpoint (.withEndpointConfiguration
                 (AwsClientBuilder$EndpointConfiguration.
                   (:service-endpoint endpoint)
                   (:signing-region endpoint))))))

(def ^:private ssm-client
  "The SSM client used to retrieve parameters from SSM."
  (delay (build-client ssm-client-args)))

(defrecord SsmVal
  [config value]
  config/Extractable
  (extract [_]
    (when (not= ::not-found value)
      value))
  config/Optional
  (provided? [_]
    (not= ::not-found value)))

(defmethod print-method SsmVal [^SsmVal v ^java.io.Writer w]
  (.write w "#config-aws/ssm ")
  (.write w (pr-str (.config v))))

(defn ^:private get-parameter
  [^AWSSimpleSystemsManagement client name]
  (try
    (let [request (.. (GetParameterRequest.)
                      (withName name)
                      (withWithDecryption true))]
      (.. client
          (getParameter request)
          (getParameter)
          (getValue)))
    (catch ParameterNotFoundException _
      ::not-found)))

(defn read-ssm
  "Reads an SsmVal."
  [config]
  (cond
    (string? config)
    (->SsmVal config
              (get-parameter @ssm-client config))

    (vector? config)
    (->SsmVal config
              (get-parameter @ssm-client (str/join (into []
                                                         (map config/extract)
                                                         config))))

    :default
    (throw (IllegalArgumentException.
             (format "Argument to #config-aws/ssm must be a string or a vector: %s"
                     (pr-str config))))))
