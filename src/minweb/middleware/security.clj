(ns minweb.middleware.security)

(def ^:dynamic *csp-nonce*
  "Per-request CSP nonce. Bound by `wrap-csp` and read by view helpers
   to stamp inline event handlers / scripts."
  nil)

(defn generate-nonce
  "16 random bytes from SecureRandom, base64-encoded. ~24 chars."
  []
  (let [buf (byte-array 16)
        rng (java.security.SecureRandom.)]
    (.nextBytes rng buf)
    (.encodeToString (java.util.Base64/getEncoder) buf)))

(defn csp-policy
  "Build the CSP header value with the given nonce. script-src uses nonce
   (no 'unsafe-inline'); style-src keeps 'unsafe-inline' for the one dynamic
   :style attribute (progress-bar width); other directives are tightened."
  [nonce]
  (str "default-src 'self'; "
       "script-src 'self' 'nonce-" nonce "'; "
       "style-src 'self' 'unsafe-inline'; "
       "img-src 'self' data: https://*.aliyuncs.com; "
       "font-src 'self'; connect-src 'self'; "
       "frame-ancestors 'none'; "
       "base-uri 'self'; form-action 'self'; object-src 'none'"))

(def security-headers
  "Headers added by `wrap-security-headers`. CSP is intentionally absent —
   it is set by `wrap-csp` (deeper in the chain) so the nonce can match
   the body's stamped handlers."
  {"X-Frame-Options" "DENY"
   "X-Content-Type-Options" "nosniff"
   "X-XSS-Protection" "1; mode=block"
   "Cache-Control" "no-cache, no-store, must-revalidate"
   "Pragma" "no-cache"})

(defn wrap-security-headers
  "Add the static security headers to every response. CSP is added
   separately by `wrap-csp` so the nonce can be threaded into the body."
  [handler]
  (fn [req]
    (let [resp (handler req)]
      (if (map? resp)
        (let [existing-headers (or (:headers resp) {})
              merged-headers (merge security-headers existing-headers)]
          (assoc resp :headers merged-headers))
        resp))))

(defn wrap-csp
  "Generate a per-request nonce, bind it to *csp-nonce* for the duration of
   the handler (so views can stamp the same nonce on inline handlers), and
   set the Content-Security-Policy response header. Must be installed
   INSIDE wrap-security-headers so its header survives the outer merge."
  [handler]
  (fn [req]
    (let [nonce (generate-nonce)]
      (binding [*csp-nonce* nonce]
        (let [resp (handler req)]
          (if (map? resp)
            (assoc-in resp [:headers "Content-Security-Policy"] (csp-policy nonce))
            resp))))))