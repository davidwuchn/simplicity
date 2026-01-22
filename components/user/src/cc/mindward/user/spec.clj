(ns cc.mindward.user.spec
  "Domain specifications for the user component.
   
   These specs define the contract for user data.
   Use for validation at component boundaries."
  (:require [clojure.spec.alpha :as s]))

;; ------------------------------------------------------------
;; Primitive Specs
;; ------------------------------------------------------------

(s/def :user/id pos-int?)
(s/def :user/username (s/and string? #(re-matches #"[a-zA-Z0-9_-]{3,32}" %)))
(s/def :user/password (s/and string? #(>= (count %) 6)))
(s/def :user/password_hash string?)
(s/def :user/name (s/and string? #(<= 1 (count %) 64)))
(s/def :user/high_score nat-int?)

;; ------------------------------------------------------------
;; Composite Specs
;; ------------------------------------------------------------

(s/def ::create-user-request
  (s/keys :req-un [:user/username :user/password :user/name]))

(s/def ::user-record
  (s/keys :req-un [:user/id :user/username :user/password_hash :user/name :user/high_score]))

(s/def ::user-public
  (s/keys :req-un [:user/username :user/name]
          :opt-un [:user/high_score]))

(s/def ::leaderboard-entry
  (s/keys :req-un [:user/username :user/name :user/high_score]))

(s/def ::leaderboard
  (s/coll-of ::leaderboard-entry :kind vector?))

;; ------------------------------------------------------------
;; Validation Helpers
;; ------------------------------------------------------------

(defn valid-create-request?
  "Validate a user creation request map."
  [request]
  (s/valid? ::create-user-request request))

(defn explain-create-request
  "Return human-readable explanation for invalid create request."
  [request]
  (s/explain-str ::create-user-request request))
