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
               [:script "
                 const form = document.getElementById('signup-form');
                 if (form) {
                   form.addEventListener('submit', (e) => {
                     const username = form.querySelector('[name=username]');
                     const password = form.querySelector('[name=password]');
                     const name = form.querySelector('[name=name]');
                     
                     if (username.value.length < 3 || username.value.length > 20) {
                       e.preventDefault();
                       showToast('Net ID must be 3-20 characters', 'error');
                       username.focus();
                       return;
                     }
                     if (!/^[a-zA-Z0-9_-]+$/.test(username.value)) {
                       e.preventDefault();
                       showToast('Net ID can only contain letters, numbers, _ and -', 'error');
                       username.focus();
                       return;
                     }
                     if (password.value.length < 8) {
                       e.preventDefault();
                       showToast('Access Key must be at least 8 characters', 'error');
                       password.focus();
                       return;
                     }
                     if (name.value.length < 3) {
                       e.preventDefault();
                       showToast('Handle must be at least 3 characters', 'error');
                       name.focus();
                       return;
                     }
                   });
                  }
                "]]})})})


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
             (c/card
             {:title "Net Access"
              :class "max-w-md mx-auto mt-8 md:mt-12 fade-in"
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
                 "Register here"]]]
               
               ;; Show toast on error
               (when (:error params)
                 [:script "setTimeout(() => showToast('Invalid credentials. Try again.', 'error'), 100);"])]})})})