(ns cc.mindward.ui.layout
  "Page layout infrastructure: head, navigation, scripts, and main layout function.
   
   This namespace provides the foundational layout components used by all pages."
  (:require [hiccup2.core :as h]
            [cc.mindward.ui.styles :as styles]
            [cc.mindward.ui.components :as c]
            [cc.mindward.ui.helpers :as helpers]))

;; === Configuration ===

(def ^:private app-version
  "Application version for cache-busting static assets.
   Update this on each release to invalidate browser caches."
  "1.0.9")

(def ^:private meta-config
  {:charset "UTF-8"
   :viewport "width=device-width, initial-scale=1.0"
   :description "Musical Game of Life - Connect to the grid"})

(def ^:private cdn-links
  {:tailwind "https://cdn.tailwindcss.com"
   :font "https://fonts.googleapis.com/css2?family=Orbitron:wght@400;700;900&display=swap"})

;; === Scripts ===

(defn- toast-script 
  "JavaScript for toast notifications."
  []
  "
  function showToast(message, type = 'info', duration = 3000) {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type} fade-in`;
    toast.textContent = message;
    toast.setAttribute('role', 'status');
    container.appendChild(toast);
    setTimeout(() => {
      toast.style.animation = 'slideIn 0.3s ease-out reverse';
      setTimeout(() => toast.remove(), 300);
    }, duration);
  }")

(defn- form-loading-script 
  "JavaScript for form loading states."
  []
  "
  document.addEventListener('DOMContentLoaded', () => {
    const forms = document.querySelectorAll('form');
    forms.forEach(form => {
      form.addEventListener('submit', (e) => {
        const btn = form.querySelector('button[type=submit]');
        if (btn) {
          btn.classList.add('loading');
          btn.disabled = true;
        }
      });
    });
  });")

;; === Head Component ===

(defn- page-head
  "Render the HTML head section.
   
   Options:
   - :title - Page title (required)"
  [{:keys [title]}]
  [:head
   [:meta {:charset (:charset meta-config)}]
   [:meta {:name "viewport" :content (:viewport meta-config)}]
   [:meta {:name "description" :content (:description meta-config)}]
   [:title title]
   [:script {:src (:tailwind cdn-links)}]
   [:link {:href (:font cdn-links) :rel "stylesheet"}]
   [:style (styles/stylesheet)]])

;; === Navigation Component ===

(defn- navigation
  "Render the main navigation.
   
   session - Ring session map"
  [session]
  (let [user (helpers/current-user session)]
    [:nav {:class "bg-black border-b-2 border-cyber-cyan p-4 mb-8"
           :role "navigation"
           :aria-label "Main navigation"}
     [:div {:class "container mx-auto flex justify-between items-center flex-wrap gap-4"}
      ;; Brand
      [:a {:href "/"
           :class "text-xl md:text-2xl font-black italic tracking-tighter text-cyber-yellow drop-shadow-[2px_2px_0px_#ff003c]"
           :aria-label "Home - Mindward Simplicity"}
       "MINDWARD // SIMPLICITY"]
      
      ;; Navigation Links
      [:div {:class "flex items-center space-x-4 md:space-x-6 uppercase tracking-widest text-xs md:text-sm"}
       (c/nav-link {:href "/leaderboard"
                    :text "Leaderboard"
                    :aria-label "View leaderboard"})
       
       (if user
         ;; Authenticated user menu
         [:div {:class "flex items-center space-x-2 md:space-x-4"}
          [:span {:class "text-gray-500 hidden-mobile"}
           "PILOT: "
           [:span {:class "font-bold text-cyber-cyan"} (:username user)]]
          (c/nav-link {:href "/game"
                       :text "JACK IN"
                       :aria-label "Start game"
                       :active? true})
          (c/nav-link {:href "/logout"
                       :text "ABORT"
                       :aria-label "Logout"})]
         
         ;; Guest menu
         [:div {:class "space-x-2 md:space-x-4"}
          (c/nav-link {:href "/login"
                       :text "LOGIN"
                       :aria-label "Login"})
          (c/link-button {:href "/signup"
                          :text "INITIATE"
                          :class "text-xs px-3 md:px-4 py-2"
                          :aria-label "Sign up"})])]]]))

;; === Layout Component ===

(defn layout
  "Main page layout.
   
   Options:
   - :session - Ring session map
   - :title - Page title (required)
   - :content - Page content (Hiccup) (required)
   - :extra-footer - Optional footer content (Hiccup)"
  [{:keys [session title content extra-footer]}]
  (str
   "<!DOCTYPE html>\n"
   (h/html
    [:html {:lang "en"}
     (page-head {:title title})
     [:body
      ;; Toast container
      [:div {:id "toast-container" :role "alert" :aria-live "polite"}]
      
      ;; Navigation
      (navigation session)
      
      ;; Main content
      [:main {:class "container mx-auto px-4" :role "main"}
       content]
      
      ;; Footer/extra
      extra-footer
      
       ;; Scripts
       [:script (h/raw (toast-script))]
       [:script (h/raw (form-loading-script))]]])))


;; === Public Getters (for use in page namespaces) ===

(defn app-version-string
  "Get the current app version for cache busting."
  []
  app-version)

(defn cdn-links-map
  "Get CDN links configuration."
  []
  cdn-links)
