(ns outpace.config-aws.spec
  (:require [clojure.spec.alpha :as s]))

(s/def ::service-endpoint string?)
(s/def ::signing-region string?)

(s/def ::aws-access-key-id string?)
(s/def ::aws-secret-key string?)
(s/def ::session-token string?)

(s/def ::credentials
  (s/keys :req-un [::aws-access-key-id ::aws-secret-key]
          :opt-un [::session-token]))

(s/def ::endpoint
  (s/keys :req-un [::service-endpoint ::signing-region]))

(s/def ::ssm-client-args
  (s/keys :opt-un [::credentials
                   ::endpoint]))

(defn validate-client-args
  "Validates the given client arguments using spec."
  [client-args]
  (s/valid? ::ssm-client-args client-args))
