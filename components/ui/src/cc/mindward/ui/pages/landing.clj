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
              "Connect to the grid. Engage hostile protocols. Ascend the hierarchy."]

             [:div {:class "flex flex-col sm:flex-row gap-4 sm:gap-8 pointer-events-auto w-full sm:w-auto fade-in"}
              (c/link-button {:href "/login"
                              :text "Login"
                              :type :secondary
                              :class "min-w-[150px]"
                              :aria-label "Login to existing account"})
              (c/link-button {:href "/signup"
                              :text "Initiate"
                              :type :primary
                              :class "min-w-[150px]"
                              :aria-label "Create new account"})]]]
           :extra-footer [:script {:src "/js/life.js"}]})})
