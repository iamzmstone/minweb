(ns minweb.view.config
  (:require [clojure.string :as str]))

;; view.config - Component configuration maps

(def badge-variant-classes
  {:primary "bg-purple-200 text-purple-800"
   :info "bg-blue-200 text-blue-800"
   :success "bg-green-200 text-green-800"
   :warning "bg-yellow-200 text-yellow-800"
   :danger "bg-red-200 text-red-500"
   :secondary "bg-gray-200 text-gray-800"})

(def badge-size-classes
  {:xs "text-xs px-2 py-0.5"
   :sm "text-xs px-2.5 py-0.5"
   :md "text-sm px-3 py-1"})

(def input-size-classes
  {:xs "px-2 py-1 text-xs"
   :sm "px-3 py-1.5 text-sm"
   :md "px-4 py-2 text-base"
   :lg "px-5 py-2.5 text-lg"})

(defn merge-classes
  "Join classes, filtering out blank strings."
  [& classes]
  (str/join " " (remove str/blank? classes)))