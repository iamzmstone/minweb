(ns middleware.security)

(def security-headers
  "Security headers to add to all responses"
  {"X-Frame-Options" "DENY"
   "X-Content-Type-Options" "nosniff"
   "X-XSS-Protection" "1; mode=block"
   "Cache-Control" "no-cache, no-store, must-revalidate"
   "Pragma" "no-cache"})

(defn wrap-security-headers
  "Middleware that adds security headers to responses"
  [handler]
  (fn [req]
    (let [resp (handler req)]
      (update resp :headers #(merge security-headers %)))))