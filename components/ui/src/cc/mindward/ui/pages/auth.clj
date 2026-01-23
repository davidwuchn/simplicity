(ns cc.mindward.ui.pages.auth
  "Authentication pages: login and signup."
  (:require [cc.mindward.ui.layout :as layout]
            [cc.mindward.ui.components :as c]))

(defn signup-page
  "Render the signup page.

   Options:
   - :session - Ring session map
   - :params - Request params (may contain errors)
   - :anti-forgery-token - CSRF token"
  [{:keys [_session params anti-forgery-token]}]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (layout/layout
          {:session nil  ; Signup is for unauthenticated users
           :title "Join the Fleet"
           :content
           (c/card
            {:title "Identity Reg"
             :class "max-w-md mx-auto mt-8 md:mt-12 fade-in"
             :content
             [:div
                ;; Error alert
              (when (:error params)
                (c/alert {:type :error
                          :message "ERROR: IDENTITY CONFLICT DETECTED."}))

                ;; Signup form
              [:form {:method "POST"
                      :action "/signup"
                      :id "signup-form"
                      :aria-label "Sign up form"
                      :novalidate true}
               [:input {:type "hidden" :name "__anti-forgery-token" :value anti-forgery-token}]

                 ;; Display Name
               (c/form-input {:id "name"
                              :name "name"
                              :label "Handle (Display)"
                              :placeholder "ENTER_HANDLE"
                              :required? true
                              :minlength 3
                              :maxlength 50
                              :autocomplete "name"
                              :help-text "3-50 characters"})

                 ;; Username
               (c/form-input {:id "username"
                              :name "username"
                              :label "Net ID (Login)"
                              :placeholder "ENTER_ID"
                              :required? true
                              :minlength 3
                              :maxlength 20
                              :pattern "[a-zA-Z0-9_-]+"
                              :autocomplete "username"
                              :help-text "3-20 characters, alphanumeric only"})

                 ;; Password
               (c/form-input {:id "password"
                              :name "password"
                              :label "Access Key (Password)"
                              :type "password"
                              :placeholder "********"
                              :required? true
                              :minlength 8
                              :autocomplete "new-password"
                              :help-text "Minimum 8 characters"})

                 ;; Submit Button
               (c/button {:text "ESTABLISH LINK"
                          :type :primary
                          :submit? true
                          :aria-label "Create account"
                          :class "w-full"})

                 ;; Login Link
               [:div {:class "mt-6 text-center text-xs md:text-sm"}
                [:span {:class "text-gray-500"} "Already registered? "]
                [:a {:href "/login"
                     :class "text-cyber-cyan hover:text-cyber-yellow transition-colors"}
                 "Login here"]]]

                ;; Client-side validation script
              [:script (str "const form = document.getElementById('signup-form');"
                            "if (form) { form.addEventListener('submit', (e) => { "
                            "const username = form.querySelector('[name=username]');"
                            "const password = form.querySelector('[name=password]');"
                            "const name = form.querySelector('[name=name]');"
                            "if (username.value.length < 3 || username.value.length > 20) {"
                            "e.preventDefault(); showToast('Net ID must be 3-20 characters', 'error');"
                            "username.focus(); return; }"
                            "if (!/^[a-zA-Z0-9_-]+$/.test(username.value)) {"
                            "e.preventDefault(); showToast('Net ID can only contain letters, numbers, _ and -', 'error');"
                            "username.focus(); return; }"
                            "if (password.value.length < 8) {"
                            "e.preventDefault(); showToast('Access Key must be at least 8 characters', 'error');"
                            "password.focus(); return; }"
                            "if (name.value.length < 3) {"
                            "e.preventDefault(); showToast('Handle must be at least 3 characters', 'error');"
                            "name.focus(); return; }"
                            "}); }")]]})})})

(defn login-page
  "Render the login page.

   Options:
   - :session - Ring session map
   - :params - Request params (may contain errors)
   - :anti-forgery-token - CSRF token"
  [{:keys [session params anti-forgery-token]}]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (layout/layout
          {:session session
           :title "Login"
           :content
           [:div
            ;; Background Canvas for Game of Life
            [:canvas {:id "bgCanvas"
                      :class "fixed top-0 left-0 w-full h-full z-0 pointer-events-auto"
                      :aria-hidden "true"}]
            [:input {:type "hidden" :id "csrf-token" :value (:csrf-token session "")}]

            [:div {:class "relative z-10 min-h-[80vh] flex flex-col justify-center items-center text-center pointer-events-none px-4"}
             [:div {:class "relative mb-8 md:mb-12 pointer-events-auto fade-in"}
              [:h1 {:class "text-4xl md:text-7xl font-black text-cyber-yellow mb-2 glitch-text uppercase tracking-tighter"}
               "MINDWARD"]
              [:div {:class "text-lg md:text-2xl font-bold text-cyber-cyan tracking-[0.5em] md:tracking-[1em] uppercase"}
               "Simplicity"]]

             [:p {:class "text-base md:text-xl text-gray-400 mb-12 md:mb-16 max-w-lg font-mono border-l-4 border-cyber-red pl-4 md:pl-6 text-left bg-black/50 p-3 md:p-4 pointer-events-auto backdrop-blur-sm fade-in"}
              "Authenticate to access the grid. Enter your credentials to establish a secure connection."]

             ;; Login Card
             (c/card
              {:title "Net Access"
               :class "max-w-md mx-auto pointer-events-auto"
               :content
               [:div
                ;; Error alert
                (when (:error params)
                  (c/alert {:type :error
                            :message "ACCESS DENIED. INVALID CREDENTIALS."}))

                ;; Login form
                [:form {:method "POST"
                        :action "/login"
                        :id "login-form"
                        :aria-label "Login form"}
                 [:input {:type "hidden" :name "__anti-forgery-token" :value anti-forgery-token}]

                 ;; Username
                 (c/form-input {:id "username-input"
                                :name "username"
                                :label "Net ID"
                                :placeholder "ID_REQUIRED"
                                :required? true
                                :autocomplete "username"})

                 ;; Password
                 (c/form-input {:id "password-input"
                                :name "password"
                                :label "Access Key"
                                :type "password"
                                :placeholder "KEY_REQUIRED"
                                :required? true
                                :autocomplete "current-password"})

                 ;; Submit Button
                 (c/button {:text "JACK IN"
                            :type :primary
                            :submit? true
                            :aria-label "Login"
                            :class "w-full"})

                 ;; Signup Link
                 [:div {:class "mt-6 text-center text-xs md:text-sm"}
                  [:span {:class "text-gray-500"} "New to the grid? "]
                  [:a {:href "/signup"
                       :class "text-cyber-cyan hover:text-cyber-yellow transition-colors"}
                   "Initiate"]]]

                ;; Show toast on error
                (when (:error params)
                  [:script "setTimeout(() => showToast('Invalid credentials. Try again.', 'error'), 100);"])]})]

            :extra-footer [:script {:src (str "/js/life.js?v=" (layout/app-version-string))}]]})})