(ns cc.mindward.ui.pages.select-game
  "Game selection page - choose between available games."
  (:require [cc.mindward.ui.layout :as layout]
            [cc.mindward.ui.components :as c]))

(defn select-game-page
  "Render the game selection page.

   Options:
   - :session - Ring session map (may contain :flash for success messages)"
  [{:keys [session]}]
  (let [flash (:flash session)]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (layout/layout
            {:session session
             :title "Select Game"
             :content
             [:div {:class "max-w-4xl mx-auto fade-in"}
              ;; Flash message
              (when flash
                [:div {:class "mb-6"} (c/alert {:type (or (:type flash) :success)
                                                :message (:message flash)})])

              [:h1 {:class "text-3xl md:text-5xl font-black text-cyber-yellow mb-8 md:mb-12 text-center uppercase tracking-widest glitch-text"}
               "Choose Your Mission"]

              ;; Game Selection Cards
              [:div {:class "flex flex-col md:flex-row gap-6 md:gap-8 hover:gap-8 transition-all duration-300"}

               ;; Game 1: Space Shooter
               (c/card
                {:class "flex-1 min-w-[280px] hover:scale-[1.02] hover:shadow-[8px_8px_0px_0px_#00f0ff] transition-all duration-300"
                 :content
                 [:div {:class "text-center"}
                  [:div {:class "text-5xl mb-4"} "🚀"]
                  [:h2 {:class "text-2xl font-black text-cyber-yellow uppercase tracking-widest mb-2"}
                   "Space Shooter"]
                  [:p {:class "text-xs text-gray-400 uppercase tracking-wider mb-4"} "Action / Reflexes"]
                  [:p {:class "text-sm text-gray-300 mb-6 leading-relaxed"}
                   "Navigate hostile space. Destroy enemy ships. Ascend the ranks."]
                  (c/link-button {:href "/game/shooter"
                                  :text "Deploy"
                                  :type :primary
                                  :class "w-full"
                                  :aria-label "Play Space Shooter game"})]})

               ;; Game 2: Conway's Game of Life
               (c/card
                {:class "flex-1 min-w-[280px] hover:scale-[1.02] hover:shadow-[8px_8px_0px_0px_#00f0ff] transition-all duration-300"
                 :content
                 [:div {:class "text-center"}
                  [:div {:class "text-5xl mb-4"} "🧬"]
                  [:h2 {:class "text-2xl font-black text-cyber-yellow uppercase tracking-widest mb-2"}
                   "Conway's Life"]
                  [:p {:class "text-xs text-gray-400 uppercase tracking-wider mb-4"} "Strategy / Patterns"]
                  [:p {:class "text-sm text-gray-300 mb-6 leading-relaxed"}
                   "Build sustainable colonies. Watch patterns emerge. Cellular automata with music."]
                  (c/link-button {:href "/game/life"
                                  :text "Initialize"
                                  :type :secondary
                                  :class "w-full"
                                  :aria-label "Play Conway's Game of Life"})]})]

              ;; Leaderboard Link
              [:div {:class "mt-12 text-center"}
               [:a {:href "/leaderboard"
                    :class "text-cyber-cyan hover:text-cyber-yellow transition-colors text-sm uppercase tracking-widest border-b border-transparent hover:border-cyber-yellow pb-1"}
                "View Global Rankings →"]]]})}))