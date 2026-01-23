(ns cc.mindward.ui.pages.leaderboard
  "Leaderboard page - displays global high scores.

   Security (∀ Vigilance): All user-generated content is escaped
   using helpers/escape-html to prevent XSS attacks."
  (:require [cc.mindward.ui.layout :as layout]
            [cc.mindward.ui.components :as c]
            [cc.mindward.ui.helpers :as helpers]))

(defn leaderboard-page
  "Render the leaderboard page.

   Options:
   - :session - Ring session map
   - :leaderboard - Vector of leaderboard entries"
  [{:keys [session leaderboard]}]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (layout/layout
          {:session session
           :title "Global Leaderboard"
           :content
           [:div
            ;; Background Canvas for Game of Life
            [:canvas {:id "bgCanvas"
                      :class "fixed top-0 left-0 w-full h-full z-0 pointer-events-auto"
                      :aria-hidden "true"}]
            [:input {:type "hidden" :id "csrf-token" :value (:csrf-token session "")}]

            [:div {:class "relative z-10 min-h-[80vh] flex flex-col justify-center items-center text-center pointer-events-none px-4"}
             [:div {:class "relative mb-8 md:mb-12 pointer-events-auto fade-in w-full max-w-4xl"}
              [:h1 {:class "text-4xl md:text-7xl font-black text-cyber-yellow mb-2 glitch-text uppercase tracking-tighter"}
               "MINDWARD"]
              [:div {:class "text-lg md:text-2xl font-bold text-cyber-cyan tracking-[0.5em] md:tracking-[1em] uppercase"}
               "Simplicity"]

              [:p {:class "text-base md:text-xl text-gray-400 mb-12 md:mb-16 max-w-lg font-mono border-l-4 border-cyber-red pl-4 md:pl-6 text-left bg-black/50 p-3 md:p-4 pointer-events-auto backdrop-blur-sm fade-in mx-auto"}
               "Connect to the grid. Engage hostile protocols. Ascend the hierarchy."]

              ;; Leaderboard Table
              (c/table
               {:columns ["Rank" "Netrunner" "Score"]
                :rows (map-indexed
                       (fn [idx {:keys [username name high_score]}]
                         {:cells [(cond
                                    (= idx 0) "👑 KING"
                                    (= idx 1) "🥈 02"
                                    (= idx 2) "🥉 03"
                                    :else (format "%02d" (inc idx)))
                                  ;; Security: Escape user-generated content (∀ Vigilance)
                                  (helpers/escape-html (or name username))
                                  (helpers/format-number high_score)]
                          :options {:highlight? (zero? idx)}})
                       leaderboard)
                :empty-message "No data in the net."
                :corners? true})

              ;; Back to game button (if logged in)
              (when (helpers/logged-in? session)
                [:div {:class "mt-8 text-center pointer-events-auto"}
                 (c/link-button {:href "/game"
                                 :text "Return to Combat"
                                 :type :secondary})])]]

            :extra-footer [:script {:src (str "/js/life.js?v=" (layout/app-version-string))}]]})})
