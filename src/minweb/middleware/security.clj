(ns minweb.middleware.security)

(def security-headers
  "Security headers to add to all responses.
   img-src 加 *.aliyuncs.com 通配 OSS 桶(上传后的 photo URL 都是 oss-cn-*.<id>.aliyuncs.com 之类),
   不加的话工人上传完照片,slot 里的 <img src> 会被 CSP 拦截,看不到缩略图。"
  {"X-Frame-Options" "DENY"
   "X-Content-Type-Options" "nosniff"
   "X-XSS-Protection" "1; mode=block"
   "Content-Security-Policy"
   (str "default-src 'self'; script-src 'self' 'unsafe-inline'; "
        "style-src 'self' 'unsafe-inline'; "
        "img-src 'self' data: https://*.aliyuncs.com; "
        "font-src 'self'; connect-src 'self'; frame-ancestors 'none'")
   "Cache-Control" "no-cache, no-store, must-revalidate"
   "Pragma" "no-cache"})

(defn wrap-security-headers
  "Middleware that adds security headers to responses"
  [handler]
  (fn [req]
    (let [resp (handler req)]
      (if (map? resp)
        (let [existing-headers (or (:headers resp) {})
              merged-headers (merge security-headers existing-headers)]
          (assoc resp :headers merged-headers))
        resp))))