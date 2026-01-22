(ns cc.mindward.ui.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [cc.mindward.ui.core-refactored :as ui]
            [clojure.string :as str]))

;; ============================================================================
;; Test Helpers
;; ============================================================================

(defn- extract-html-body
  "Extract body content from response map"
  [response]
  (:body response))

(defn- contains-str?
  "Case-sensitive substring check"
  [s substr]
  (and (string? s) (str/includes? s substr)))

(defn- contains-tag?
  "Check if HTML contains a specific tag with optional attributes"
  [html tag]
  (contains-str? html (str "<" tag)))

(defn- contains-class?
  "Check if HTML contains a specific CSS class"
  [html css-class]
  (contains-str? html (str "class=\"" css-class)))

(defn- contains-input-name?
  "Check if HTML contains an input with specific name attribute"
  [html input-name]
  (contains-str? html (str "name=\"" input-name "\"")))

(defn- contains-csrf-token?
  "Check if HTML contains CSRF token input field"
  [html]
  (or (contains-str? html "name=\"__anti-forgery-token\"")
      (contains-str? html "id=\"csrf-token\"")))

;; ============================================================================
;; Layout Function Tests
;; ============================================================================

(deftest layout-basic-structure-test
  (testing "Layout generates valid HTML structure"
    (let [html (ui/layout {:session nil :title "Test Title" :content [:div "Content"]})]
      (is (string? html))
      (is (contains-tag? html "html") "Missing html tag")
      (is (contains-tag? html "head") "Missing head tag")
      (is (contains-tag? html "body") "Missing body tag")
      (is (contains-tag? html "title") "Missing title tag")
      (is (contains-str? html "Test Title") "Title not rendered"))))

(deftest layout-meta-and-charset-test
  (testing "Layout includes proper meta tags and charset"
    (let [html (ui/layout {:session nil :title "Test" :content [:div "Content"]})]
      (is (contains-str? html "charset=\"UTF-8\"") "Missing UTF-8 charset"))))

(deftest layout-external-dependencies-test
  (testing "Layout includes external CSS and JS dependencies"
    (let [html (ui/layout {:session nil :title "Test" :content [:div "Content"]})]
      (is (contains-str? html "cdn.tailwindcss.com") "Missing Tailwind CDN")
      (is (contains-str? html "fonts.googleapis.com") "Missing Google Fonts")
      (is (contains-str? html "Orbitron") "Missing Orbitron font family"))))

(deftest layout-cyber-styling-test
  (testing "Layout includes cyberpunk CSS classes and animations"
    (let [html (ui/layout {:session nil :title "Test" :content [:div "Content"]})]
      (is (contains-str? html ".cyber-card") "Missing cyber-card style")
      (is (contains-str? html ".cyber-input") "Missing cyber-input style")
      (is (contains-str? html ".cyber-btn") "Missing cyber-btn style")
      (is (contains-str? html ".glitch-text") "Missing glitch-text style")
      (is (contains-str? html "@keyframes glitch") "Missing glitch animation")
      (is (contains-str? html "text-cyber-yellow") "Missing cyber-yellow color")
      (is (contains-str? html "text-cyber-cyan") "Missing cyber-cyan color")
      (is (contains-str? html "text-cyber-red") "Missing cyber-red color"))))

(deftest layout-navigation-unauthenticated-test
  (testing "Layout shows login/signup links when user not authenticated"
    (let [html (ui/layout {:session nil :title "Test" :content [:div "Content"]})]
      (is (contains-str? html "LOGIN") "Missing LOGIN link")
      (is (contains-str? html "INITIATE") "Missing INITIATE (signup) link")
      (is (contains-str? html "href=\"/login\"") "Missing login href")
      (is (contains-str? html "href=\"/signup\"") "Missing signup href")
      (is (not (contains-str? html "PILOT:")) "Should not show PILOT when unauthenticated")
      (is (not (contains-str? html "ABORT")) "Should not show ABORT when unauthenticated"))))

(deftest layout-navigation-authenticated-test
  (testing "Layout shows user-specific navigation when authenticated"
    (let [html (ui/layout {:session {:user {:username "testpilot"}} :title "Test" :content [:div "Content"]})]
      (is (contains-str? html "PILOT:") "Missing PILOT label")
      (is (contains-str? html "testpilot") "Missing username display")
      (is (contains-str? html "JACK IN") "Missing JACK IN link")
      (is (contains-str? html "ABORT") "Missing ABORT (logout) link")
      (is (contains-str? html "href=\"/game\"") "Missing game href")
      (is (contains-str? html "href=\"/logout\"") "Missing logout href")
      (is (not (contains-str? html "LOGIN")) "Should not show LOGIN when authenticated")
      (is (not (contains-str? html "INITIATE")) "Should not show INITIATE when authenticated"))))

(deftest layout-branding-test
  (testing "Layout includes MINDWARD branding"
    (let [html (ui/layout {:session nil :title "Test" :content [:div "Content"]})]
      (is (contains-str? html "MINDWARD // SIMPLICITY") "Missing brand text")
      (is (contains-str? html "href=\"/\"") "Missing home link"))))

(deftest layout-leaderboard-link-test
  (testing "Layout includes leaderboard link for all users"
    (let [html-unauth (ui/layout {:session nil :title "Test" :content [:div "Content"]})
          html-auth (ui/layout {:session {:user {:username "testpilot"}} :title "Test" :content [:div "Content"]})]
      (is (contains-str? html-unauth "Leaderboard") "Missing leaderboard link (unauth)")
      (is (contains-str? html-auth "Leaderboard") "Missing leaderboard link (auth)")
      (is (contains-str? html-unauth "href=\"/leaderboard\"") "Missing leaderboard href (unauth)")
      (is (contains-str? html-auth "href=\"/leaderboard\"") "Missing leaderboard href (auth)"))))

(deftest layout-content-injection-test
  (testing "Layout properly injects content parameter"
    (let [html (ui/layout {:session nil :title "Test" :content [:div {:class "test-content"} "My Custom Content"]})]
      (is (contains-str? html "My Custom Content") "Content not injected")
      (is (contains-class? html "test-content") "Content class not preserved"))))

(deftest layout-extra-footer-test
  (testing "Layout supports optional extra footer content"
    (let [html-no-footer (ui/layout {:session nil :title "Test" :content [:div "Content"]})
          html-with-footer (ui/layout {:session nil :title "Test" :content [:div "Content"] :extra-footer [:footer "Extra Footer"]})]
      (is (not (contains-str? html-no-footer "Extra Footer")) "Footer should not appear without param")
      (is (contains-str? html-with-footer "Extra Footer") "Extra footer not injected")
      (is (contains-tag? html-with-footer "footer") "Footer tag not rendered"))))

;; ============================================================================
;; Leaderboard Page Tests
;; ============================================================================

(deftest leaderboard-page-structure-test
  (testing "Leaderboard page returns proper response structure"
    (let [response (ui/leaderboard-page {:session nil :leaderboard []})]
      (is (map? response) "Response should be a map")
      (is (= 200 (:status response)) "Should return 200 status")
      (is (= "text/html" (get-in response [:headers "Content-Type"])) "Should have text/html content type")
      (is (string? (:body response)) "Body should be string")
      (is (not (str/blank? (:body response))) "Body should not be empty"))))

(deftest leaderboard-page-title-test
  (testing "Leaderboard page has correct title and heading"
    (let [html (extract-html-body (ui/leaderboard-page {:session nil :leaderboard []}))]
      (is (contains-str? html "Global Leaderboard") "Missing page title")
      (is (contains-str? html "Netrunner Legends") "Missing page heading"))))

(deftest leaderboard-page-empty-state-test
  (testing "Leaderboard shows empty state when no data"
    (let [html (extract-html-body (ui/leaderboard-page {:session nil :leaderboard []}))]
      (is (contains-str? html "No data in the net") "Missing empty state message"))))

(deftest leaderboard-page-table-structure-test
  (testing "Leaderboard has proper table structure"
    (let [html (extract-html-body (ui/leaderboard-page {:session nil :leaderboard []}))]
      (is (contains-tag? html "table") "Missing table tag")
      (is (contains-tag? html "thead") "Missing thead tag")
      (is (contains-tag? html "tbody") "Missing tbody tag")
      (is (contains-str? html "Rank") "Missing Rank header")
      (is (contains-str? html "Netrunner") "Missing Netrunner header")
      (is (contains-str? html "Score") "Missing Score header"))))

(deftest leaderboard-page-with-data-test
  (testing "Leaderboard renders user data correctly"
    (let [leaderboard [{:username "player1" :name "Alpha" :high_score 1000}
                       {:username "player2" :name "Beta" :high_score 500}
                       {:username "player3" :name nil :high_score 250}]
          html (extract-html-body (ui/leaderboard-page {:session nil :leaderboard leaderboard}))]
      (is (contains-str? html "Alpha") "Missing first player name")
      (is (contains-str? html "Beta") "Missing second player name")
      (is (contains-str? html "player3") "Missing third player username (fallback)")
      (is (contains-str? html "1,000") "Missing first player score (formatted)")
      (is (contains-str? html "500") "Missing second player score")
      (is (contains-str? html "250") "Missing third player score")
      (is (contains-str? html "KING") "Missing KING rank for first place"))))

(deftest leaderboard-page-rank-formatting-test
  (testing "Leaderboard formats ranks correctly"
    (let [leaderboard [{:username "p1" :name "First" :high_score 100}
                       {:username "p2" :name "Second" :high_score 50}]
          html (extract-html-body (ui/leaderboard-page {:session nil :leaderboard leaderboard}))]
      (is (contains-str? html "KING") "First place should be KING")
      (is (contains-str? html "02") "Second place should be formatted as 02"))))

;; ============================================================================
;; Signup Page Tests
;; ============================================================================

(deftest signup-page-structure-test
  (testing "Signup page returns proper response structure"
    (let [response (ui/signup-page {:session nil :params {} :anti-forgery-token "test-token"})]
      (is (map? response) "Response should be a map")
      (is (= 200 (:status response)) "Should return 200 status")
      (is (= "text/html" (get-in response [:headers "Content-Type"])) "Should have text/html content type")
      (is (string? (:body response)) "Body should be string"))))

(deftest signup-page-form-structure-test
  (testing "Signup page has proper form with required fields"
    (let [html (extract-html-body (ui/signup-page {:session nil :params {} :anti-forgery-token "test-token"}))]
      (is (contains-tag? html "form") "Missing form tag")
      (is (contains-str? html "method=\"POST\"") "Form should use POST method")
      (is (contains-str? html "action=\"/signup\"") "Form should post to /signup")
      (is (contains-input-name? html "name") "Missing name input")
      (is (contains-input-name? html "username") "Missing username input")
      (is (contains-input-name? html "password") "Missing password input"))))

(deftest signup-page-csrf-token-test
  (testing "Signup page includes CSRF token"
    (let [html (extract-html-body (ui/signup-page {:session nil :params {} :anti-forgery-token "my-csrf-token"}))]
      (is (contains-csrf-token? html) "Missing CSRF token field")
      (is (contains-str? html "my-csrf-token") "CSRF token value not rendered"))))

(deftest signup-page-error-display-test
  (testing "Signup page shows error message when present"
    (let [html-no-error (extract-html-body (ui/signup-page {:session nil :params {} :anti-forgery-token "token"}))
          html-with-error (extract-html-body (ui/signup-page {:session nil :params {:error true} :anti-forgery-token "token"}))]
      (is (not (contains-str? html-no-error "ERROR: IDENTITY CONFLICT")) "Should not show error without param")
      (is (contains-str? html-with-error "ERROR: IDENTITY CONFLICT") "Missing error message"))))

(deftest signup-page-field-labels-test
  (testing "Signup page has clear field labels"
    (let [html (extract-html-body (ui/signup-page {:session nil :params {} :anti-forgery-token "token"}))]
      (is (contains-str? html "Handle (Display)") "Missing handle label")
      (is (contains-str? html "Net ID (Login)") "Missing net ID label")
      (is (contains-str? html "Access Key (Password)") "Missing access key label"))))

(deftest signup-page-submit-button-test
  (testing "Signup page has submit button"
    (let [html (extract-html-body (ui/signup-page {:session nil :params {} :anti-forgery-token "token"}))]
      (is (contains-tag? html "button") "Missing button tag")
      (is (contains-str? html "type=\"submit\"") "Button should be submit type")
      (is (contains-str? html "ESTABLISH LINK") "Missing submit button text"))))

;; ============================================================================
;; Login Page Tests
;; ============================================================================

(deftest login-page-structure-test
  (testing "Login page returns proper response structure"
    (let [response (ui/login-page {:session nil :params {} :anti-forgery-token "test-token"})]
      (is (map? response) "Response should be a map")
      (is (= 200 (:status response)) "Should return 200 status")
      (is (= "text/html" (get-in response [:headers "Content-Type"])) "Should have text/html content type")
      (is (string? (:body response)) "Body should be string"))))

(deftest login-page-form-structure-test
  (testing "Login page has proper form with required fields"
    (let [html (extract-html-body (ui/login-page {:session nil :params {} :anti-forgery-token "test-token"}))]
      (is (contains-tag? html "form") "Missing form tag")
      (is (contains-str? html "method=\"POST\"") "Form should use POST method")
      (is (contains-str? html "action=\"/login\"") "Form should post to /login")
      (is (contains-input-name? html "username") "Missing username input")
      (is (contains-input-name? html "password") "Missing password input"))))

(deftest login-page-csrf-token-test
  (testing "Login page includes CSRF token"
    (let [html (extract-html-body (ui/login-page {:session nil :params {} :anti-forgery-token "login-csrf-token"}))]
      (is (contains-csrf-token? html) "Missing CSRF token field")
      (is (contains-str? html "login-csrf-token") "CSRF token value not rendered"))))

(deftest login-page-error-display-test
  (testing "Login page shows error message when present"
    (let [html-no-error (extract-html-body (ui/login-page {:session nil :params {} :anti-forgery-token "token"}))
          html-with-error (extract-html-body (ui/login-page {:session nil :params {:error true} :anti-forgery-token "token"}))]
      (is (not (contains-str? html-no-error "ACCESS DENIED")) "Should not show error without param")
      (is (contains-str? html-with-error "ACCESS DENIED") "Missing error message")
      (is (contains-str? html-with-error "INVALID CREDENTIALS") "Missing credentials error"))))

(deftest login-page-submit-button-test
  (testing "Login page has submit button"
    (let [html (extract-html-body (ui/login-page {:session nil :params {} :anti-forgery-token "token"}))]
      (is (contains-tag? html "button") "Missing button tag")
      (is (contains-str? html "type=\"submit\"") "Button should be submit type")
      (is (contains-str? html "JACK IN") "Missing submit button text"))))

;; ============================================================================
;; Game Page Tests
;; ============================================================================

(deftest game-page-structure-test
  (testing "Game page returns proper response structure"
    (let [response (ui/game-page nil "game-token" 1000)]
      (is (map? response) "Response should be a map")
      (is (= 200 (:status response)) "Should return 200 status")
      (is (= "text/html" (get-in response [:headers "Content-Type"])) "Should have text/html content type")
      (is (string? (:body response)) "Body should be string"))))

(deftest game-page-canvas-test
  (testing "Game page includes game canvas"
    (let [html (extract-html-body (ui/game-page nil "token" 0))]
      (is (contains-tag? html "canvas") "Missing canvas tag")
      (is (contains-str? html "id=\"gameCanvas\"") "Missing gameCanvas ID"))))

(deftest game-page-csrf-token-test
  (testing "Game page includes CSRF token for API calls"
    (let [html (extract-html-body (ui/game-page nil "game-csrf-token" 0))]
      (is (contains-str? html "id=\"csrf-token\"") "Missing CSRF token element")
      (is (contains-str? html "game-csrf-token") "CSRF token value not rendered"))))

(deftest game-page-high-score-display-test
  (testing "Game page displays high score"
    (let [html (extract-html-body (ui/game-page nil "token" 5000))]
      (is (contains-str? html "BEST:") "Missing BEST label")
      (is (contains-str? html "5000") "High score value not rendered")
      (is (contains-str? html "id=\"high-score\"") "Missing high-score element ID"))))

(deftest game-page-controls-overlay-test
  (testing "Game page shows control instructions"
    (let [html (extract-html-body (ui/game-page nil "token" 0))]
      (is (contains-str? html "ARROWS to Move") "Missing arrow keys instruction")
      (is (contains-str? html "SPACE to Switch Weapon") "Missing space key instruction")
      (is (contains-str? html "R to Reboot") "Missing R key instruction")
      (is (contains-str? html "ABORT MISSION") "Missing abort link"))))

(deftest game-page-script-loading-test
  (testing "Game page loads game JavaScript module"
    (let [html (extract-html-body (ui/game-page nil "token" 0))]
      (is (contains-str? html "type=\"module\"") "Missing module script type")
      (is (contains-str? html "/js/game.js") "Missing game.js script src")
      (is (contains-str? html "?v=") "Missing cache-busting query param"))))

(deftest game-page-fullscreen-styling-test
  (testing "Game page has fullscreen styling"
    (let [html (extract-html-body (ui/game-page nil "token" 0))]
      (is (contains-str? html "overflow: hidden") "Missing overflow hidden")
      (is (contains-str? html "width: 100%") "Missing width 100%")
      (is (contains-str? html "height: 100%") "Missing height 100%"))))

;; ============================================================================
;; Landing Page Tests
;; ============================================================================

(deftest landing-page-structure-test
  (testing "Landing page returns proper response structure"
    (let [response (ui/landing-page {:session nil})]
      (is (map? response) "Response should be a map")
      (is (= 200 (:status response)) "Should return 200 status")
      (is (= "text/html" (get-in response [:headers "Content-Type"])) "Should have text/html content type")
      (is (string? (:body response)) "Body should be string"))))

(deftest landing-page-branding-test
  (testing "Landing page displays MINDWARD branding"
    (let [html (extract-html-body (ui/landing-page {:session nil}))]
      (is (contains-str? html "MINDWARD") "Missing MINDWARD brand")
      (is (contains-str? html "Simplicity") "Missing Simplicity subtitle"))))

(deftest landing-page-tagline-test
  (testing "Landing page includes tagline"
    (let [html (extract-html-body (ui/landing-page {:session nil}))]
      (is (contains-str? html "Connect to the grid") "Missing tagline"))))

(deftest landing-page-cta-buttons-test
  (testing "Landing page has call-to-action buttons"
    (let [html (extract-html-body (ui/landing-page {:session nil}))]
      (is (contains-str? html "href=\"/login\"") "Missing login link")
      (is (contains-str? html "href=\"/signup\"") "Missing signup link")
      (is (contains-str? html "Login") "Missing Login button text")
      (is (contains-str? html "Initiate") "Missing Initiate button text"))))

(deftest landing-page-background-canvas-test
  (testing "Landing page includes background Game of Life canvas"
    (let [html (extract-html-body (ui/landing-page {:session nil}))]
      (is (contains-str? html "id=\"bgCanvas\"") "Missing background canvas")
      (is (contains-str? html "/js/life.js") "Missing life.js script"))))

;; ============================================================================
;; Cross-Cutting Concerns Tests
;; ============================================================================

(deftest all-pages-are-responsive-test
  (testing "All pages include Tailwind for responsive design"
    (let [pages [(ui/landing-page {:session nil})
                 (ui/login-page {:session nil :params {} :anti-forgery-token "t"})
                 (ui/signup-page {:session nil :params {} :anti-forgery-token "t"})
                 (ui/leaderboard-page {:session nil :leaderboard []})
                 (ui/game-page nil "t" 0)]]
      (doseq [page pages]
        (is (contains-str? (:body page) "tailwindcss.com")
            "Page missing Tailwind CSS")))))

(deftest all-forms-have-csrf-protection-test
  (testing "All forms include CSRF token"
    (let [login-html (extract-html-body (ui/login-page {:session nil :params {} :anti-forgery-token "login-t"}))
          signup-html (extract-html-body (ui/signup-page {:session nil :params {} :anti-forgery-token "signup-t"}))
          game-html (extract-html-body (ui/game-page nil "game-t" 0))]
      (is (contains-csrf-token? login-html) "Login form missing CSRF token")
      (is (contains-csrf-token? signup-html) "Signup form missing CSRF token")
      (is (contains-csrf-token? game-html) "Game page missing CSRF token"))))

(deftest all-pages-use-cyber-theme-test
  (testing "All pages use cyberpunk theme classes"
    (let [pages [(ui/landing-page {:session nil})
                 (ui/login-page {:session nil :params {} :anti-forgery-token "t"})
                 (ui/signup-page {:session nil :params {} :anti-forgery-token "t"})
                 (ui/leaderboard-page {:session nil :leaderboard []})]]
      (doseq [page pages]
        (let [html (:body page)]
          (is (or (contains-str? html "cyber-")
                  (contains-str? html "text-cyber"))
              "Page missing cyber theme classes"))))))
