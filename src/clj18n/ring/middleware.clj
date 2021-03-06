(ns clj18n.ring.middleware
  "Ring middleware for loading dictionaries and setting locales."
  (:require [clj18n.core :refer [to-locale]]
            [clj18n.translation :refer [translate]]))

(defn locale-from-header
  "Gets the locale from the request Accept-Language header."
  [{:keys [headers]}]
  (when headers (headers "Accept-Language")))

(defn locale-from-param
  "Gets the locale based on the :locale param."
  [{:keys [params]}]
  (when params (params :locale)))

(defn locale-from-uri
  "Gets the locale from the request uri, using the first part of the URI as the
  locale."
  [{:keys [uri] :as req}]
  (last (re-find #"^/([^/]+)" uri)))

(defn locale-from-domain
  "Gets the locale from the request server-name, by looking for a two-character
  language subdomain, avoiding capture of www and main domain."
  [{:keys [server-name]}]
  (last (re-find  #"^([^\.]{2})\..+\..+" server-name)))

(defn wrap-locale
  "Sets request :locale to the first value returned by :locale-fns or to
  :default. :locale-fns are functions that take the request as argument and
  return a locale string or nil."
  [app & {:keys [default locale-fns]
          :or {default :en locale-fns [locale-from-header locale-from-param]}}]
  (let [loc-str-fn (apply some-fn locale-fns)]
    (fn [req]
      (let [loc-str (or (loc-str-fn req) default)]
        (app (assoc req :locale (to-locale loc-str)))))))

(defn wrap-translation
  "Sets request :t to be a function of translations for dict and request
  :locale."
  [app dict]
  (fn [{:keys [locale] :as req}]
    (let [t (partial translate dict locale)]
      (app (assoc req :t t)))))

(defn wrap-i18n
  "Calls both wrap-locale and wrap-translation with the given arguments."
  [app dict & args]
  (apply wrap-locale (wrap-translation app dict) args))
