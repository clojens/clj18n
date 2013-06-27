(ns clj18n.localization
  "Localization functionality implemented via the Localization protocol and
  closures created by create-fmt."
  (:require [clojure.string :refer [split]])
  (:import [java.lang Number]
           [java.text DateFormat NumberFormat]
           [java.util Date]))

(defprotocol Localization
  "Localized formatting for objects."
  (localize [obj loc] [obj loc style]
    "Returns a localized string corresponding to obj. Can optionally be given
    a style to control how the object is formatted. Normally used via
    functions returned by create-fmt."))

(def ^:private time-format
  {:default DateFormat/DEFAULT
   :short   DateFormat/SHORT
   :medium  DateFormat/MEDIUM
   :long    DateFormat/LONG
   :full    DateFormat/FULL})

(defn- date-formatter
  "Returns a date formatter corresponding to the arguments."
  [loc style]
  (let [[type length] (map keyword (split (name style) #"-"))
        format (time-format (or length :default))]
    (case type
      :date (DateFormat/getDateInstance format loc)
      :datetime (DateFormat/getDateTimeInstance
                  format format loc)
      :time (DateFormat/getTimeInstance format loc))))

(defn- num-formatter
  "Returns a number formatter corresponding to the arguments."
  [loc style]
  (case style
    :number (NumberFormat/getNumberInstance loc)
    :integer (NumberFormat/getIntegerInstance loc)
    :percentage (NumberFormat/getPercentInstance loc)
    :currency (NumberFormat/getCurrencyInstance loc)))

(extend-protocol Localization
  Date
  (localize
    ([date loc] (localize date loc :date))
    ([date loc style]
       (.format (date-formatter loc style) date)))
  Number
  (localize
    ([n loc] (localize n loc :number))
    ([n loc style]
       (.format (num-formatter loc style) n)))
  nil
  (localize
    ([obj loc] "")
    ([obj loc style] "")))

(defn parse-date
  "Attempts to parse s as a java.util.Date according to the format indicated by
  loc and an optional style."
  ([s loc] (parse-date s loc :date))
  ([s loc style]
     (.parse (date-formatter loc style) s)))

(defn parse-number
  "Attempts to parse s as a java.Util.Number according to the format indicated by
  loc and an optional style."
  ([s loc] (parse-number s loc :number))
  ([s loc style]
     (.parse (num-formatter loc style) s)))

(defn create-fmt
  "Creates a formatter function for loc."
  [loc]
  (fn
    ([obj] (localize obj loc))
    ([obj style] (localize obj loc style))))