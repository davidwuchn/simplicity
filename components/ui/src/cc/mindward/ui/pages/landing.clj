(ns cc.mindward.ui.pages.landing
  "Landing page - the entry point for unauthenticated users."
  (:require [cc.mindward.ui.layout :as layout]
            [cc.mindward.ui.components :as c]))

(defn landing-page
  "Render the landing page.

   Options:
   - :session - Ring session map"
  [{:keys [session]}]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (layout/layout
          {:session session
           :title "Welcome"
           :content
           [:div {:class "max-w-4xl mx-auto fade-in"}
            [:h1 {:class "text-4xl md:text-7xl font-black text-cyber-yellow mb-2 glitch-text uppercase tracking-tighter text-center"}
             "MINDWARD"]
            [:div {:class "text-lg md:text-2xl font-bold text-cyber-cyan tracking-[0.5em] md:tracking-[1em] uppercase text-center mb-8"}
             "Simplicity"]

            [:p {:class "text-base md:text-xl text-gray-400 mb-12 max-w-lg mx-auto font-mono border-l-4 border-cyber-red pl-4 md:pl-6 text-left bg-black/50 p-3 md:p-4 backdrop-blur-sm"}
             "Connect to the grid. Engage hostile protocols. Ascend the hierarchy."]

            [:div {:class "flex flex-col sm:flex-row gap-4 sm:gap-8 justify-center fade-in"}
             (c/link-button {:href "/login"
                             :text "Login"
                             :type :secondary
                             :class "min-w-[150px]"
                             :aria-label "Login to existing account"})
             (c/link-button {:href "/signup"
                             :text "Initiate"
                             :type :primary
                             :class "min-w-[150px]"
                             :aria-label "Create new account"})]]})})
