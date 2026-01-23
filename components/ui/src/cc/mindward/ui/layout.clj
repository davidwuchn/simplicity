(ns cc.mindward.ui.layout
  "Page layout infrastructure: head, navigation, scripts, and main layout function.
   
   This namespace provides the foundational layout components used by all pages."
  (:require [hiccup2.core :as h]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [cc.mindward.ui.styles :as styles]
            [cc.mindward.ui.components :as c]
            [cc.mindward.ui.helpers :as helpers]))

;; === Configuration ===

(def ^:private app-version
  "Application version for cache-busting static assets.
   Read from VERSION file at project root (single source of truth)."
  (delay
    (try
      (-> (io/resource "VERSION")
          slurp
          str/trim)
      (catch Exception _
        ;; Fallback for development when resource not on classpath
        (try
          (-> (io/file "VERSION")
              slurp
              str/trim)
          (catch Exception _
            "dev"))))))

(def ^:private meta-config
  {:charset "UTF-8"
   :viewport "width=device-width, initial-scale=1.0"
   :description "Musical Game of Life - Connect to the grid"})

(def ^:private cdn-links
  {:tailwind "https://cdn.tailwindcss.com"
   :font "https://fonts.googleapis.com/css2?family=Orbitron:wght@400;700;900&display=swap"})

;; === Scripts ===

(defn- tailwind-config-script
  "Generate Tailwind configuration script.
   Injects the project's design system (colors, fonts) into Tailwind."
  []
  (let [config {:theme
                {:extend
                 {:colors styles/colors
                  :fontFamily {:sans ["Orbitron" "sans-serif"]}
                  :keyframes {:glitch {"0%" {:textShadow (str "2px 0 " (:cyber-red styles/colors) ", -2px 0 " (:cyber-cyan styles/colors))}
                                       "25%" {:textShadow (str "-2px 0 " (:cyber-red styles/colors) ", 2px 0 " (:cyber-cyan styles/colors))}
                                       "50%" {:textShadow (str "2px 0 " (:cyber-cyan styles/colors) ", -2px 0 " (:cyber-yellow styles/colors))}
                                       "100%" {:textShadow (str "-2px 0 " (:cyber-cyan styles/colors) ", 2px 0 " (:cyber-red styles/colors))}}
                              :spin {:to {:transform "rotate(360deg)"}}}
                  :animation {:glitch "glitch 1s infinite alternate-reverse"
                              :spin "spin 0.6s linear infinite"}}}}]
    (str "tailwind.config = " (json/write-str config) ";")))

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
   [:script (h/raw (tailwind-config-script))]
   [:link {:href (:font cdn-links) :rel "stylesheet"}]
   [:style (styles/stylesheet)]])

;; === Navigation Component ===

(defn- navigation
  "Render the main navigation.
   
   session - Ring session map"
  [session]
  (let [user (helpers/current-user session)]
    [:nav {:class "bg-black border-b-2 border-cyber-cyan p-4 mb-8 relative z-20"
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
          (c/nav-link {:href "/login?force=true"
                       :text "LOGIN"
                       :aria-label "Login"})])]]]))

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
       [:script {:src (str "/js/ui.js?v=" @app-version)}]]])))


;; === Public Getters (for use in page namespaces) ===

(defn app-version-string
  "Get the current app version for cache busting."
  []
  @app-version)

(defn cdn-links-map
  "Get CDN links configuration."
  []
  cdn-links)
