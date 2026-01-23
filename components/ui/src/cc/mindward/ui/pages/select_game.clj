(ns cc.mindward.ui.pages.select-game
  "Game selection page - choose between available games."
  (:require [cc.mindward.ui.layout :as layout]
            [cc.mindward.ui.components :as c]))

(defn select-game-page
  "Render the game selection page.

   Options:
   - :session - Ring session map"
  [{:keys [session]}]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (layout/layout
          {:session session
           :title "Select Game"
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
              "Select your mission protocol. Each challenge offers unique rewards."]

             ;; Game Selection Cards
             [:div {:class "flex flex-col md:flex-row gap-6 md:gap-8 pointer-events-auto w-full max-w-4xl fade-in"}

              ;; Game 1: Space Shooter
              [:div {:class "cyber-card flex-1 min-w-[280px] hover:border-cyber-cyan transition-all group"}
               [:div {:class "mb-4"}
                [:div {:class "text-4xl mb-2"} "🚀"]
                [:h2 {:class "text-2xl font-black text-cyber-yellow uppercase tracking-widest group-hover:text-cyber-cyan transition-colors"}
                 "Space Shooter"]
                [:p {:class "text-xs text-gray-400 mt-1 uppercase tracking-wider"} "Action / Reflexes"]]
               [:p {:class "text-sm text-gray-300 mb-6 leading-relaxed"}
                "Navigate hostile space. Destroy enemy ships. Ascend the ranks.
                 Features weapon switching, shields, and escalating difficulty."]
               [:div {:class "flex flex-wrap gap-2 mb-6"}
                [:span {:class "badge bg-cyber-cyan text-black"} "Fast-Paced"]
                [:span {:class "badge bg-gray-700 text-cyber-cyan"} "High Score"]
                [:span {:class "badge bg-gray-700 text-cyber-red"} "Action"]]
               (c/link-button {:href "/game/shooter"
                               :text "Deploy"
                               :type :primary
                               :class "w-full"
                               :aria-label "Play Space Shooter game"})]

              ;; Game 2: Conway's Game of Life
              [:div {:class "cyber-card flex-1 min-w-[280px] hover:border-cyber-cyan transition-all group"}
               [:div {:class "mb-4"}
                [:div {:class "text-4xl mb-2"} "🧬"]
                [:h2 {:class "text-2xl font-black text-cyber-yellow uppercase tracking-widest group-hover:text-cyber-cyan transition-colors"}
                 "Conway's Life"]
                [:p {:class "text-xs text-gray-400 mt-1 uppercase tracking-wider"} "Strategy / Patterns"]]
               [:p {:class "text-sm text-gray-300 mb-6 leading-relaxed"}
                "Build sustainable colonies. Watch patterns emerge. Evolve complex organisms.
                 Cellular automata with musical integration."]
               [:div {:class "flex flex-wrap gap-2 mb-6"}
                [:span {:class "badge bg-cyber-cyan text-black"} "Strategic"]
                [:span {:class "badge bg-gray-700 text-cyber-yellow"} "Music"]
                [:span {:class "badge bg-gray-700 text-cyber-cyan"} "Zen"]]
               (c/link-button {:href "/game/life"
                               :text "Initialize"
                               :type :secondary
                               :class "w-full"
                               :aria-label "Play Conway's Game of Life"})]]

             ;; Leaderboard Link
             [:div {:class "mt-12 pointer-events-auto fade-in"}
              [:a {:href "/leaderboard"
                   :class "text-cyber-cyan hover:text-cyber-yellow transition-colors text-sm uppercase tracking-widest border-b border-transparent hover:border-cyber-yellow pb-1"}
               "View Global Rankings →"]]]

            :extra-footer [:script {:src (str "/js/life.js?v=" (layout/app-version-string))}]]})})