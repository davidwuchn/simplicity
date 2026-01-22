(ns cc.mindward.ui.specs
  "Clojure spec definitions for UI component data structures.
   
   This namespace defines specs for validating:
   - Component options maps
   - Helper function inputs/outputs
   - Page component parameters
   
   Following Clojure best practices for data validation."
  (:require [clojure.spec.alpha :as s]))

;; === Basic Primitives ===

(s/def ::non-empty-string (s/and string? #(not (empty? %))))
(s/def ::css-class ::non-empty-string)
(s/def ::html-id ::non-empty-string)
(s/def ::url-path ::non-empty-string)

;; === Common UI Attributes ===

(s/def ::class (s/or :string ::css-class
                     :vector (s/coll-of ::css-class :kind vector?)))
(s/def ::id ::html-id)
(s/def ::href ::url-path)
(s/def ::text ::non-empty-string)
(s/def ::aria-label ::non-empty-string)

;; === Component Option Specs ===

;; Alert component
(s/def ::alert-type #{:success :error :info :warning})
(s/def ::alert-message ::non-empty-string)
(s/def ::alert-options (s/keys :req-un [::alert-type ::alert-message]
                                :opt-un [::class]))

;; Form input component
(s/def ::form-input-type #{"text" "password" "email" "number" "tel" "url"})
(s/def ::name ::non-empty-string)
(s/def ::label ::non-empty-string)
(s/def ::placeholder string?)
(s/def ::required? boolean?)
(s/def ::help-text string?)
(s/def ::error string?)
(s/def ::minlength pos-int?)
(s/def ::maxlength pos-int?)
(s/def ::pattern string?)
(s/def ::autocomplete string?)
(s/def ::form-input-options (s/keys :req-un [::id ::name ::label]
                                     :opt-un [::type ::placeholder ::required?
                                              ::help-text ::error ::class
                                              ::minlength ::maxlength ::pattern
                                              ::autocomplete]))

;; Button component
(s/def ::button-type #{:primary :secondary})
(s/def ::submit? boolean?)
(s/def ::disabled? boolean?)
(s/def ::button-options (s/keys :req-un [::text]
                                 :opt-un [::type ::submit? ::disabled?
                                          ::class ::aria-label]))

;; Link button component
(s/def ::link-button-options (s/keys :req-un [::href ::text]
                                      :opt-un [::type ::class ::aria-label]))

;; Card component
(s/def ::title ::non-empty-string)
(s/def ::content any?) ;; Hiccup vector
(s/def ::corners? boolean?)
(s/def ::card-options (s/keys :opt-un [::title ::content ::class ::corners?]))

;; Nav link component
(s/def ::active? boolean?)
(s/def ::nav-link-options (s/keys :req-un [::href ::text]
                                   :opt-un [::active? ::aria-label ::class]))

;; Table component
(s/def ::columns (s/coll-of ::non-empty-string :kind vector?))
(s/def ::cells (s/coll-of any? :kind vector?))
(s/def ::highlight? boolean?)
(s/def ::row-options (s/keys :opt-un [::highlight?]))
(s/def ::table-row (s/keys :req-un [::cells]
                            :opt-un [::options]))
(s/def ::rows (s/coll-of ::table-row :kind vector?))
(s/def ::empty-message string?)
(s/def ::table-options (s/keys :req-un [::columns ::rows]
                                :opt-un [::empty-message ::corners?]))

;; Badge component
(s/def ::badge-type #{:success :error :info :warning :default})
(s/def ::badge-options (s/keys :req-un [::text]
                                :opt-un [::type ::class]))

;; === Helper Function Specs ===

;; Class helpers
(s/def ::class-list (s/coll-of (s/or :string string? :nil nil?) :kind vector?))
(s/def ::condition boolean?)
(s/def ::true-class string?)
(s/def ::false-class (s/or :string string? :nil nil?))

;; Form helpers
(s/def ::errors (s/map-of keyword? string?))
(s/def ::field keyword?)

;; Validation helpers
(s/def ::username (s/and string? #(re-matches #"[a-zA-Z0-9_-]{3,20}" %)))
(s/def ::password (s/and string? #(>= (count %) 8)))
(s/def ::display-name (s/and string? #(<= 3 (count %) 50)))

;; Session helpers
(s/def ::session (s/nilable map?))
(s/def ::user-id pos-int?)
(s/def ::username-field ::username)
(s/def ::user (s/keys :req-un [::user-id ::username-field]
                      :opt-un [::name]))

;; === Page Component Specs ===

;; Leaderboard page
(s/def ::high-score nat-int?)
(s/def ::leaderboard-entry (s/keys :req-un [::username ::high-score]
                                    :opt-un [::name]))
(s/def ::leaderboard (s/coll-of ::leaderboard-entry :kind vector?))
(s/def ::leaderboard-page-options (s/keys :req-un [::session ::leaderboard]))

;; Signup/Login page
(s/def ::params (s/nilable map?))
(s/def ::anti-forgery-token ::non-empty-string)
(s/def ::signup-page-options (s/keys :req-un [::session ::params ::anti-forgery-token]))
(s/def ::login-page-options (s/keys :req-un [::session ::params ::anti-forgery-token]))

;; Landing page
(s/def ::landing-page-options (s/keys :req-un [::session]))

;; Game page
(s/def ::game-page-params (s/cat :session ::session
                                  :anti-forgery-token ::anti-forgery-token
                                  :high-score nat-int?))

;; === Response Specs ===

(s/def ::status #{200 201 302 400 401 403 404 500})
(s/def ::headers (s/map-of string? string?))
(s/def ::body string?)
(s/def ::http-response (s/keys :req-un [::status ::headers ::body]))
