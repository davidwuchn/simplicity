(ns cc.mindward.ui.components
  "Reusable UI components following Clojure best practices.
   
   All functions are pure and return Hiccup data structures.
   Components are composable and data-driven.")

;; === Alert/Error Components ===

(defn alert
  "Render an alert message.
   
   Options:
   - :type - :error, :success, :warning, :info (default: :info)
   - :message - String message to display
   - :dismissible? - Whether to show close button (default: false)"
  [{:keys [type message dismissible?]
    :or {type :info dismissible? false}}]
  (let [type-classes {:error "bg-red-900/50 border-cyber-red text-red-200"
                      :success "bg-green-900/50 border-green-500 text-green-200"
                      :warning "bg-yellow-900/50 border-cyber-yellow text-yellow-200"
                      :info "bg-blue-900/50 border-cyber-cyan text-blue-200"}
        class (get type-classes type (type-classes :info))]
    [:div {:class (str class " border-l-4 px-4 py-3 font-mono text-xs md:text-sm slide-in")
           :role "alert"
           :aria-live "assertive"}
     [:span {:class "sr-only"} (str (name type) ": ")]
     message
     (when dismissible?
       [:button {:class "float-right font-bold" :aria-label "Close"} "×"])]))

;; === Form Components ===

(defn form-input
  "Render a form input with label and help text.
   
   Options:
   - :id - Input ID (required)
   - :name - Input name (required)
   - :label - Label text (required)
   - :type - Input type (default: text)
   - :placeholder - Placeholder text
   - :required? - Whether input is required
   - :help-text - Help text below input
   - :autocomplete - Autocomplete attribute
   - :minlength - Minimum length
   - :maxlength - Maximum length
   - :pattern - Validation pattern"
  [{:keys [id name label type placeholder required? help-text autocomplete
           minlength maxlength pattern]
    :or {type "text" required? false}}]
  (let [help-id (str id "-help")]
    [:div {:class "mb-6"}
     [:label {:for id
              :class "block text-cyber-cyan text-xs font-bold mb-2 uppercase tracking-widest"}
      label]
     [:input (cond-> {:type type
                      :id id
                      :name name
                      :class "w-full px-4 py-3 cyber-input"
                      :placeholder placeholder}
               required? (assoc :required true :aria-required "true")
               autocomplete (assoc :autocomplete autocomplete)
               minlength (assoc :minlength minlength)
               maxlength (assoc :maxlength maxlength)
               pattern (assoc :pattern pattern)
               help-text (assoc :aria-describedby help-id))]
     (when help-text
       [:p {:id help-id :class "text-xs text-gray-500 mt-1"} help-text])]))

(defn button
  "Render a button component.
   
   Options:
   - :text - Button text (required)
   - :type - :primary or :secondary (default: :primary)
   - :submit? - Whether type=submit (default: false)
   - :disabled? - Whether disabled (default: false)
   - :class - Additional CSS classes
   - :aria-label - ARIA label
   - :onclick - onClick handler"
  [{:keys [text type submit? disabled? class aria-label onclick]
    :or {type :primary submit? false disabled? false}}]
  (let [base-class (if (= type :secondary) "cyber-btn-secondary" "cyber-btn")
        full-class (str base-class " " class)]
    [:button (cond-> {:class full-class}
               submit? (assoc :type "submit")
               disabled? (assoc :disabled true)
               aria-label (assoc :aria-label aria-label)
               onclick (assoc :onclick onclick))
     text]))

(defn link-button
  "Render a link styled as a button.
   
   Options:
   - :href - Link destination (required)
   - :text - Link text (required)
   - :type - :primary or :secondary (default: :primary)
   - :class - Additional CSS classes
   - :aria-label - ARIA label"
  [{:keys [href text type class aria-label]
    :or {type :primary}}]
  (let [base-class (if (= type :secondary) "cyber-btn-secondary" "cyber-btn")
        full-class (str base-class " inline-block text-center " class)]
    [:a (cond-> {:href href :class full-class}
          aria-label (assoc :aria-label aria-label))
     text]))

;; === Layout Components ===

(defn card
  "Render a cyber-themed card with optional corner decorations.
   
   Options:
   - :title - Card title
   - :content - Card content (Hiccup)
   - :corners? - Show corner decorations (default: false)
   - :class - Additional CSS classes"
  [{:keys [title content corners? class]
    :or {corners? false}}]
  [:div {:class (str "cyber-card " class)}
   (when corners?
     [:<>
      [:div {:class "absolute -top-1 -left-1 w-4 h-4 border-t-2 border-l-2 border-cyber-yellow"}]
      [:div {:class "absolute -top-1 -right-1 w-4 h-4 border-t-2 border-r-2 border-cyber-yellow"}]
      [:div {:class "absolute -bottom-1 -left-1 w-4 h-4 border-b-2 border-l-2 border-cyber-yellow"}]
      [:div {:class "absolute -bottom-1 -right-1 w-4 h-4 border-b-2 border-r-2 border-cyber-yellow"}]])
   (when title
     [:h2 {:class "text-2xl md:text-3xl font-black mb-6 md:mb-8 text-center text-cyber-yellow uppercase tracking-widest"}
      title])
   content])

(defn nav-link
  "Render a navigation link.
   
   Options:
   - :href - Link destination (required)
   - :text - Link text (required)
   - :active? - Whether link is active (default: false)
   - :aria-label - ARIA label"
  [{:keys [href text active? aria-label]
    :or {active? false}}]
  (let [base-class "hover:text-cyber-cyan transition-colors"
        active-class "text-cyber-cyan font-bold"
        inactive-class "text-gray-400"
        class (str base-class " " (if active? active-class inactive-class))]
    [:a (cond-> {:href href :class class}
          aria-label (assoc :aria-label aria-label))
     text]))

;; === Table Components ===

(defn table-header
  "Render a table header row.
   
   columns - Vector of column names"
  [columns]
  [:thead {:class "bg-zinc-900 text-cyber-cyan uppercase text-xs md:text-sm border-b-2 border-cyber-red"}
   [:tr {:role "row"}
    (for [col columns]
      [:th {:key col
            :class "px-3 md:px-6 py-3 md:py-4 font-bold tracking-wider"
            :role "columnheader"}
       col])]])

(defn table-row
  "Render a table row.
   
   cells - Vector of cell values
   Options:
   - :highlight? - Whether to highlight this row
   - :class - Additional CSS classes"
  [cells {:keys [highlight? class]
          :or {highlight? false}}]
  (let [row-class (str "border-b border-zinc-800 hover:bg-zinc-900 transition-colors "
                      (when highlight? "text-cyber-yellow font-bold ")
                      class)]
    [:tr {:class row-class :role "row"}
     (for [[idx cell] (map-indexed vector cells)]
       [:td {:key idx
             :class "px-3 md:px-6 py-3 md:py-4 text-sm md:text-base"
             :role "cell"}
        cell])]))

(defn table
  "Render a complete table.
   
   Options:
   - :columns - Vector of column names (required)
   - :rows - Vector of row data (required)
   - :empty-message - Message when no rows (default: 'No data')
   - :corners? - Show corner decorations (default: true)"
  [{:keys [columns rows empty-message corners?]
    :or {empty-message "No data available." corners? true}}]
  [:div {:class "bg-black border border-cyber-cyan relative overflow-hidden"}
   (when corners?
     [:<>
      [:div {:class "absolute -top-1 -left-1 w-4 h-4 border-t-2 border-l-2 border-cyber-yellow"}]
      [:div {:class "absolute -top-1 -right-1 w-4 h-4 border-t-2 border-r-2 border-cyber-yellow"}]
      [:div {:class "absolute -bottom-1 -left-1 w-4 h-4 border-b-2 border-l-2 border-cyber-yellow"}]
      [:div {:class "absolute -bottom-1 -right-1 w-4 h-4 border-b-2 border-r-2 border-cyber-yellow"}]])
   [:div {:class "overflow-x-auto"}
    [:table {:class "w-full text-left border-collapse" :role "table"}
     (table-header columns)
     [:tbody
      (if (empty? rows)
        [:tr {:role "row"}
         [:td {:colspan (count columns)
               :class "px-6 py-10 text-center text-gray-500 uppercase tracking-widest text-xs md:text-sm"}
          empty-message]]
        (for [[idx row-data] (map-indexed vector rows)]
          ^{:key idx}
          (table-row (:cells row-data) (:options row-data))))]]]])

;; === Utility Components ===

(defn spinner
  "Render a loading spinner.
   
   Options:
   - :size - :sm, :md, :lg (default: :md)
   - :class - Additional CSS classes"
  [{:keys [size class]
    :or {size :md}}]
  (let [size-class (case size
                     :sm "w-4 h-4"
                     :lg "w-8 h-8"
                     "w-6 h-6")]
    [:div {:class (str "loading " size-class " " class)
           :role "status"
           :aria-label "Loading"}
     [:span {:class "sr-only"} "Loading..."]]))

(defn badge
  "Render a badge/tag.
   
   Options:
   - :text - Badge text (required)
   - :type - :primary, :secondary, :success, :error (default: :primary)"
  [{:keys [text type]
    :or {type :primary}}]
  (let [type-classes {:primary "bg-cyber-cyan text-black"
                      :secondary "bg-cyber-yellow text-black"
                      :success "bg-green-500 text-black"
                      :error "bg-cyber-red text-white"}
        class (get type-classes type (type-classes :primary))]
    [:span {:class (str class " px-2 py-1 text-xs font-bold uppercase tracking-wider")}
     text]))
