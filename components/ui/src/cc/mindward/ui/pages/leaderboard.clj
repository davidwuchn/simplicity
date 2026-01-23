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
           [:div {:class "max-w-4xl mx-auto fade-in"}
            [:h1 {:class "text-3xl md:text-5xl font-black text-cyber-yellow mb-8 md:mb-12 text-center uppercase tracking-widest glitch-text"}
             "Netrunner Legends"]
            
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
              [:div {:class "mt-8 text-center"}
               (c/link-button {:href "/game"
                               :text "Return to Combat"
                               :type :secondary})])]})})
