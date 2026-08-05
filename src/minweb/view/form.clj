;; minweb.view.form - Form components

(ns minweb.view.form
  (:require
   [minweb.view.config
    :refer [input-size-classes merge-classes]]))

(defn form-input
  "Form input with consistent API.

   Options:
   - :type - input type (text, email, password, number, textarea, autocomplete, base64-upload, etc.)
   - :label - label text
   - :name - input name
   - :size - :xs, :sm, :md, :lg (default :md)
   - :variant - :default, :error (adds red border)
   - :disabled - boolean
   - :required - boolean
   - :placeholder - placeholder text
   - :value - default value
   - :class - extra CSS classes
   - :error - error message text
   - :id - input id
   - :autocomplete - autocomplete value
   - :list - for autocomplete type, provides datalist id"
  [{:as opts :keys [type label name size variant disabled
                    required placeholder value id error
                    autocomplete]
    :or {type "text" size :md
         variant :default required false}}]
  (let [list-options (:list opts)
        size-c (get input-size-classes size)
        autocomplete-c
        (or autocomplete
            (case type
              "email" "email"
              "password" "current-password"
              "tel" "tel"
              "url" "url"
              "search" "search"
              "username" "username"
              nil)
            (case name
              "username" "username"
              "email" "email"
              "password" "current-password"
              "tel" "tel"
              nil))
        variant-c
        (case variant
          :error "border-red-500 focus:ring-red-500"
          :default "border-gray-300 focus:ring-blue-500")
        base "w-full bg-white border rounded-md focus:outline-none focus:ring-2 transition"
        disabled-c (when disabled
                     "opacity-50 cursor-not-allowed")
        id-c (or id name)
        label-c (str "block text-sm font-medium mb-1"
                     (if (= variant :error)
                       " text-red-600" " text-gray-700"))]
    (cond
      (= type "textarea")
      [:div.my-2
       (when label [:label {:for id-c :class label-c} label])
       [:textarea
        {:type type :name name :id id-c
         :class (merge-classes base "py-2 px-3"
                               variant-c disabled-c)
         :placeholder placeholder
         :required required :disabled disabled}
        value]
       (when error [:span.text-sm.text-red-600.mt-1 error])]

      (= type "autocomplete")
      [:div.my-2
       (when label [:label {:for id-c :class label-c} label])
       [:input
        {:type "text" :name name
         :id id-c :list (str id-c "-list")
         :class (merge-classes base size-c variant-c disabled-c)
         :placeholder placeholder
         :required required
         :disabled disabled :autocomplete "off"}
        value]
       [:datalist {:id (str id-c "-list")}
        (for [e list-options]
          [:option {:value e}])]
       (when error [:span.text-sm.text-red-600.mt-1 error])]

      (= type "base64-upload")
      [:div.my-2
       (when label [:label {:for id-c :class label-c} label])
       [:input.form-control
        {:type "file" :required required
         :onchange (str "base64_upload('" id-c "', this)")}]
       [:input {:type "hidden" :name name :id id-c}]
       (when error [:span.text-sm.text-red-600.mt-1 error])]

      :else
      [:div.my-2
       (when label [:label {:for id-c :class label-c} label])
       [:input
        {:type type
         :name name
         :id id-c
         :autocomplete autocomplete-c
         :class (merge-classes base size-c
                               variant-c disabled-c)
         :placeholder placeholder
         :value value
         :required required
         :disabled disabled}]
       (when error [:span.text-sm.text-red-600.mt-1 error])])))

(defn form-submit-btn
  [v]
  (let [cls "w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2 px-4 rounded-md cursor-pointer"]
    [:div.my-2
     [:input {:class cls :type "submit" :value v}]]))

(defn form-select
  [{:as opts :keys [options prompt disabled size error id name label autocomplete]
    :or {size :md}}]
  (let [klass (:class opts)
        size-c (get {:xs "text-xs"
                     :sm "text-sm"
                     :md "text-base"
                     :lg "text-lg"} size)
        base "w-full bg-white border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 transition appearance-none pr-8"
        id-c (or id name)
        disabled-c (when disabled
                     "opacity-50 cursor-not-allowed")
        label-c "block text-sm font-medium text-gray-700 mb-1"
        autocomplete-c (or autocomplete
                           (case name
                             "role" "organization-title"
                             "country" "country"
                             "timezone" "timezone"
                             nil))]
    [:div.my-2
     (when label
       [:label {:for id-c :class label-c} label])
     [:div.relative
      [:select
       {:name name
        :id id-c
        :disabled disabled
        :autocomplete autocomplete-c
        :class (str base " " size-c " "
                    disabled-c " " (or klass ""))}
       (when prompt [:option {:value ""} prompt])
       (for [[v l] options]
         [:option {:value v} l])]
      [:div.absolute.inset-y-0.right-0.flex.items-center.pr-3.pointer-events-none
       [:svg {:class "w-4 h-4 text-gray-400"
              :fill "none" :stroke "currentColor"
              :viewBox "0 0 24 24"}
        [:path {:stroke-linecap "round"
                :stroke-linejoin "round"
                :stroke-width "2" :d "M19 9l-7 7-7-7"}]]]]
     (when error [:span.text-sm.text-red-600.mt-1 error])]))

(defn form-checkbox
  [{:as opts :keys [label name id value checked disabled]
    :or {checked false}}]
  (let [klass (:class opts)
        disabled-c (when disabled
                     "opacity-50 cursor-not-allowed")
        id-c (or id name)]
    [:div.my-2.flex.items-center
     [:input
      {:type "checkbox"
       :name name
       :id id-c
       :value (or value "on")
       :checked checked
       :disabled disabled
       :class (str "h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500 "
                   disabled-c " " (or klass ""))}]
     (when label
       [:label {:for id-c
                :class "ml-2 block text-sm text-gray-700"}
        label])]))

(defn form-radio
  [{:as opts :keys [label name id value checked disabled group]
    :or {checked false}}]
  (let [klass (:class opts)
        disabled-c (when disabled
                     "opacity-50 cursor-not-allowed")
        radio-name (or group name)
        id-c (or id name)]
    [:div.my-2.flex.items-center
     [:input
      {:type "radio"
       :name radio-name
       :id id-c
       :value (or value "on")
       :checked checked
       :disabled disabled
       :class (str "h-4 w-4 rounded-full border-gray-300 text-blue-600 focus:ring-blue-500 "
                   disabled-c " " (or klass ""))}]
     (when label
       [:label {:for id-c
                :class "ml-2 block text-sm text-gray-700"}
        label])]))

(defn form-toggle
  [{:keys [label name id value checked disabled]
    :or {checked false}}]
  (let [disabled-c (when disabled
                     "opacity-50 cursor-not-allowed")
        id-c (or id name)]
    [:label {:for id-c
             :class (str
                     "my-2 flex items-center cursor-pointer "
                     disabled-c)}
     [:div.relative.inline-block.w-11.h-6.flex-shrink-0
      [:input
       {:type "checkbox"
        :name name
        :id id-c
        :value (or value "on")
        :checked checked
        :disabled disabled
        :class (str "sr-only peer " disabled-c)}]
      [:div
       {:class (str "cursor-pointer absolute inset-0 w-full h-full bg-gray-300 rounded-full transition-colors duration-200 ease-in-out before:absolute before:content-[''] before:h-4 before:w-4 before:bg-white before:rounded-full before:left-0.5 before:top-1 before:transition-transform before:duration-200 before:ease-in-out before:translate-x-0 peer-checked:bg-blue-600 peer-checked:before:translate-x-5 "
                    disabled-c)}]]
     (when label [:span
                  {:class "ml-3 text-sm text-gray-700"}
                  label])]))