# UI Component

The UI component generates HTML pages using Hiccup. It provides a consistent design system and handles all server-side rendering for the Simplicity application.

## Architecture

**Interface**: `cc.mindward.ui.interface`  
**Implementation**: `cc.mindward.ui.core`

This component follows Polylith encapsulation - all UI rendering logic is accessed through the `interface` namespace.

## Features

- Server-side rendering with Hiccup
- Consistent design system (colors, typography, spacing)
- CSRF token integration
- Session-aware rendering
- Responsive design
- Semantic HTML5
- Accessibility features

## API Reference

### `landing-page`
Render the landing page (home/welcome page).

**Parameters**: `session` - Ring session map  
**Returns**: Hiccup vector (HTML data structure)

```clojure
(require '[cc.mindward.ui.interface :as ui])

(ui/landing-page {})
;; => [:html [:head ...] [:body ...]]

;; With authenticated user
(ui/landing-page {:user {:username "alice" :name "Alice"}})
;; => [:html [:head ...] [:body "Welcome back, Alice!" ...]]
```

### `login-page`
Render the login page.

**Parameters**:
- `session` - Ring session map
- `params` - Request parameters (for error messages)
- `anti-forgery-token` - CSRF token

**Returns**: Hiccup vector

```clojure
(ui/login-page {} {} "csrf-token-123")
;; => [:html [:head ...] 
;;     [:body 
;;      [:form {:method "POST" :action "/login"}
;;       [:input {:type "hidden" :name "__anti-forgery-token" :value "csrf-token-123"}]
;;       ...]]]

;; With error message
(ui/login-page {} {:error "Invalid credentials"} "token")
;; => [... error message displayed ...]
```

### `signup-page`
Render the signup/registration page.

**Parameters**:
- `session` - Ring session map
- `params` - Request parameters (for validation errors)
- `anti-forgery-token` - CSRF token

**Returns**: Hiccup vector

```clojure
(ui/signup-page {} {} "csrf-token-123")
;; => [:html [:head ...] 
;;     [:body 
;;      [:form {:method "POST" :action "/signup"}
;;       [:input {:type "hidden" :name "__anti-forgery-token" :value "csrf-token-123"}]
;;       [:input {:name "username" :placeholder "Username"}]
;;       [:input {:name "password" :type "password" :placeholder "Password"}]
;;       [:input {:name "name" :placeholder "Full Name"}]
;;       ...]]]
```

### `game-page`
Render the main game page (Game of Life interface).

**Parameters**:
- `session` - Ring session map (must contain `:user`)
- `anti-forgery-token` - CSRF token
- `high-score` - User's current high score

**Returns**: Hiccup vector

```clojure
(ui/game-page {:user {:username "alice" :name "Alice"}} 
              "csrf-token-123" 
              5000)
;; => [:html [:head 
;;      [:script "// Game of Life JavaScript"]
;;      [:style "/* Canvas styles */"]
;;      ...]
;;     [:body
;;      [:canvas {:id "game-canvas"}]
;;      [:div {:id "score"} "High Score: 5000"]
;;      ...]]
```

### `leaderboard-page`
Render the leaderboard page.

**Parameters**:
- `session` - Ring session map
- `leaderboard` - Vector of user maps with scores

**Returns**: Hiccup vector

```clojure
(ui/leaderboard-page 
  {:user {:username "alice"}}
  [{:username "bob" :name "Bob Jones" :high_score 10000}
   {:username "alice" :name "Alice Smith" :high_score 8500}
   {:username "charlie" :name "Charlie Brown" :high_score 7200}])
;; => [:html [:head ...]
;;     [:body
;;      [:table
;;       [:tr [:td "1."] [:td "bob"] [:td "Bob Jones"] [:td "10000"]]
;;       [:tr [:td "2."] [:td "alice"] [:td "Alice Smith"] [:td "8500"]]
;;       [:tr [:td "3."] [:td "charlie"] [:td "Charlie Brown"] [:td "7200"]]
;;       ...]]]
```

## Usage Examples

### Basic Page Rendering

```clojure
(require '[cc.mindward.ui.interface :as ui]
         '[hiccup.page :as page])

(defn render-html [hiccup-data]
  (page/html5 hiccup-data))

;; Render landing page
(def html (render-html (ui/landing-page {})))
;; => "<!DOCTYPE html><html><head>...</head><body>...</body></html>"
```

### Integration with Ring Handler

```clojure
(require '[cc.mindward.ui.interface :as ui]
         '[ring.util.anti-forgery :refer [anti-forgery-field]])

(defn login-handler [request]
  (let [session (:session request)
        params (:params request)
        csrf-token (anti-forgery-field)]
    {:status 200
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body (page/html5 (ui/login-page session params csrf-token))}))
```

### Error Message Display

```clojure
(defn login-post-handler [request]
  (let [{:keys [username password]} (:params request)]
    (if-let [user (auth/authenticate username password)]
      ;; Success: redirect to game
      {:status 302
       :headers {"Location" "/game"}
       :session {:user user}}
      ;; Failure: show error
      {:status 401
       :body (page/html5 
               (ui/login-page 
                 (:session request)
                 {:error "Invalid username or password"}
                 (anti-forgery-field)))})))
```

### Dynamic Content

```clojure
(defn game-handler [request]
  (let [user (get-in request [:session :user])
        high-score (user/get-high-score (:username user))
        csrf-token (anti-forgery-field)]
    {:status 200
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body (page/html5 (ui/game-page (:session request) csrf-token high-score))}))
```

## Design System

### Color Palette

```clojure
;; Primary colors
:background "#1a1a1a"      ;; Dark background
:foreground "#e0e0e0"      ;; Light text
:accent     "#00ff00"      ;; Green (musical/life)
:secondary  "#ff00ff"      ;; Magenta (musical/accent)
:error      "#ff4444"      ;; Red (errors)
:success    "#00ff00"      ;; Green (success)

;; Grays
:gray-100   "#f5f5f5"
:gray-200   "#e0e0e0"
:gray-700   "#333333"
:gray-900   "#1a1a1a"
```

### Typography

```clojure
;; Font families
:sans-serif "system-ui, -apple-system, 'Segoe UI', sans-serif"
:monospace  "'Fira Code', 'Courier New', monospace"

;; Font sizes
:text-xs    "0.75rem"   ;; 12px
:text-sm    "0.875rem"  ;; 14px
:text-base  "1rem"      ;; 16px
:text-lg    "1.125rem"  ;; 18px
:text-xl    "1.25rem"   ;; 20px
:text-2xl   "1.5rem"    ;; 24px
:text-3xl   "1.875rem"  ;; 30px
```

### Spacing

```clojure
;; Spacing scale (rem units)
:space-1  "0.25rem"   ;; 4px
:space-2  "0.5rem"    ;; 8px
:space-3  "0.75rem"   ;; 12px
:space-4  "1rem"      ;; 16px
:space-6  "1.5rem"    ;; 24px
:space-8  "2rem"      ;; 32px
:space-12 "3rem"      ;; 48px
```

### Responsive Breakpoints

```css
/* Mobile first */
@media (min-width: 640px)  { /* sm */ }
@media (min-width: 768px)  { /* md */ }
@media (min-width: 1024px) { /* lg */ }
@media (min-width: 1280px) { /* xl */ }
```

## Hiccup Patterns

### Common HTML Elements

```clojure
;; Button
[:button.primary {:type "submit"} "Click Me"]

;; Input field
[:input {:type "text" 
         :name "username" 
         :placeholder "Enter username"
         :required true}]

;; Form with CSRF
[:form {:method "POST" :action "/submit"}
 [:input {:type "hidden" :name "__anti-forgery-token" :value csrf-token}]
 ;; ... form fields ...
 [:button {:type "submit"} "Submit"]]

;; Link
[:a {:href "/page"} "Go to page"]

;; Div with class
[:div.container
 [:h1 "Title"]
 [:p "Content"]]

;; Conditional rendering
(when logged-in?
  [:div.user-info
   [:span "Welcome, " (:name user)]])
```

### Layout Components

```clojure
(defn page-layout [title & content]
  [:html
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    [:title title]]
   [:body
    [:header [:h1 title]]
    [:main content]
    [:footer "© 2026 Simplicity"]]])

;; Usage
(page-layout "My Page"
  [:p "This is my content"]
  [:p "More content here"])
```

### Navigation Component

```clojure
(defn nav-bar [user]
  [:nav
   [:a {:href "/"} "Home"]
   (if user
     [:<>
      [:a {:href "/game"} "Game"]
      [:a {:href "/leaderboard"} "Leaderboard"]
      [:a {:href "/logout"} "Logout"]]
     [:<>
      [:a {:href "/login"} "Login"]
      [:a {:href "/signup"} "Sign Up"]])])
```

## Client-Side Integration

### JavaScript Embedding

```clojure
(defn game-page [session csrf-token high-score]
  [:html
   [:head
    [:script {:src "/js/game.js"}]
    ;; Or inline JavaScript
    [:script "
      const HIGH_SCORE = " high-score ";
      const CSRF_TOKEN = '" csrf-token "';
    "]]
   [:body
    [:canvas#game-canvas]]])
```

### CSS Embedding

```clojure
[:head
 [:style "
   body { margin: 0; padding: 0; }
   .container { max-width: 1200px; margin: 0 auto; }
 "]
 ;; Or external stylesheet
 [:link {:rel "stylesheet" :href "/css/style.css"}]]
```

## Accessibility

### Semantic HTML
```clojure
;; Use semantic elements
[:header [...]]  ;; Not [:div.header]
[:nav [...]]     ;; Not [:div.nav]
[:main [...]]    ;; Not [:div.main]
[:footer [...]]  ;; Not [:div.footer]
```

### ARIA Labels
```clojure
[:button {:aria-label "Close dialog"} "×"]
[:input {:aria-describedby "username-help"}]
[:div#username-help "Username must be 3-20 characters"]
```

### Form Labels
```clojure
[:label {:for "username"} "Username"]
[:input#username {:type "text" :name "username"}]
```

## Performance

- **Server-side rendering**: ~1-5ms per page
- **HTML size**: 5-20KB (before compression)
- **Gzip compression**: ~70% size reduction

## Testing

```bash
# Run UI component tests
clojure -M:poly test brick:ui

# Expected: 149 assertions, all passing
```

Tests cover:
- Page generation for all routes
- CSRF token inclusion
- Session data rendering
- Error message display
- XSS prevention (65 assertions)
- HTML structure validation

## Security

### XSS Prevention
Hiccup automatically escapes HTML in strings:
```clojure
(let [user-input "<script>alert('XSS')</script>"]
  [:div user-input])
;; => <div>&lt;script&gt;alert('XSS')&lt;/script&gt;</div>
```

### CSRF Protection
All forms include anti-forgery tokens:
```clojure
[:form {:method "POST"}
 [:input {:type "hidden" :name "__anti-forgery-token" :value token}]
 ...]
```

See [docs/security.md](../../docs/security.md) for complete security details (65 XSS test assertions).

## Common Patterns

### Flash Messages
```clojure
(defn show-flash [session]
  (when-let [flash (:flash session)]
    [:div.flash {:class (:type flash)}
     (:message flash)]))
```

### Pagination
```clojure
(defn pagination [current-page total-pages]
  [:div.pagination
   (when (> current-page 1)
     [:a {:href (str "?page=" (dec current-page))} "Previous"])
   [:span "Page " current-page " of " total-pages]
   (when (< current-page total-pages)
     [:a {:href (str "?page=" (inc current-page))} "Next"])])
```

### Form Validation Errors
```clojure
(defn form-errors [errors]
  (when (seq errors)
    [:div.errors
     [:ul
      (for [error errors]
        [:li error])]]))
```

## See Also

- [Web Server](../../bases/web-server/README.md) - HTTP routing and rendering
- [Auth Component](../auth/README.md) - Authentication for protected pages
- [User Component](../user/README.md) - User data for rendering
- [Game Component](../game/README.md) - Game state for rendering
- [Hiccup Documentation](https://github.com/weavejester/hiccup) - Hiccup library

---

**Location**: `components/ui/src/cc/mindward/ui/`  
**Tests**: `components/ui/test/cc/mindward/ui/`  
**Lines**: ~400 (core implementation)  
**Template Engine**: Hiccup 1.0.5
