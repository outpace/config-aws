(ns outpace.config-aws.spec
  (:require [clojure.spec.alpha :as s]))

(s/def ::service-endpoint string?)
(s/def ::signing-region string?)

(s/def ::endpoint
  (s/keys :req-un [::serviceendpoint ::signing-region]))

(s/def ::ssm-client-args
  (s/keys :opt-un [::endpoint]))

(defn validate-client-args
  "Validates the given client arguments using spec."
  [client-args]
  (s/valid? ::ssm-client-args client-args))
