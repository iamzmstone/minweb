(ns minweb.utils.oss-sniff
  "图片 magic byte 嗅探 —— 给「multipart Content-Type 撒谎」类 bug 用。

   现象(2026-07-11 现场):钉钉 WebView / iOS Safari 在 input[type=file] capture 时,
   把相册里的 `.png` 文件 multipart Content-Type 设为 image/png,但实际字节是 JPEG
   (ContentResolver 按后缀给 MIME,不看 magic byte)。后端信任 multipart,OSS 存了错
   MIME → 浏览器按 PNG 解码 JPEG 字节 → img.onerror → 红框。

   解法:用 magic byte 嗅真实格式,在「写」和「读」两处 override 错的 MIME。新对象从
   源头修对;已在 OSS 里带错 MIME 的老对象也能被读路径救回来(嗅 response body 后
   override Content-Type)。

   三方都不可信(浏览器 multipart / 后端信任 multipart / OSS PUT 头),只有字节本身可信。")

(defn detect-image-content-type
  "Sniff first bytes of image data;返回 canonical MIME,识别不出返 nil。
   支持:JPEG (FF D8 FF) / PNG (89 50 4E 47 0D 0A 1A 0A) / GIF (47 49 46 38 37|39 61) /
         WebP (RIFF....WEBP)。HEIC / AVIF / BMP 没纳入(出现再加)。
   ^bytes → ^String or nil

   注意:aget 返 signed byte(0xff → -1),跟 Long literal 0xff(255) 用 = 跨类型
   不等。所有 byte 用 (bit-and b 0xff) 转 unsigned Long 再比较。"
  ^String [^bytes data]
  (when (and data (>= (alength data) 12))
    (let [b0 (bit-and (aget data 0) 0xff)
          b1 (bit-and (aget data 1) 0xff)
          b2 (bit-and (aget data 2) 0xff)
          b3 (bit-and (aget data 3) 0xff)
          b4 (bit-and (aget data 4) 0xff)
          b5 (bit-and (aget data 5) 0xff)
          b6 (bit-and (aget data 6) 0xff)
          b7 (bit-and (aget data 7) 0xff)
          b8 (bit-and (aget data 8) 0xff)
          b9 (bit-and (aget data 9) 0xff)
          b10 (bit-and (aget data 10) 0xff)
          b11 (bit-and (aget data 11) 0xff)]
      (cond
        ;; JPEG: FF D8 FF (第 3 字节是 marker:E0=APP0/E1=APP1-EXIF/DB=DQT/FE=comment/C0=SOF0/C4=DHT)
        (and (= 0xff b0) (= 0xd8 b1) (= 0xff b2)) "image/jpeg"
        ;; PNG: 89 50 4E 47 0D 0A 1A 0A
        (and (= 0x89 b0) (= 0x50 b1) (= 0x4e b2) (= 0x47 b3)
             (= 0x0d b4) (= 0x0a b5) (= 0x1a b6) (= 0x0a b7)) "image/png"
        ;; GIF87a / GIF89a: 47 49 46 38 (37|39) 61
        (and (= 0x47 b0) (= 0x49 b1) (= 0x46 b2) (= 0x38 b3)
             (or (= 0x37 b4) (= 0x39 b4))
             (= 0x61 b5)) "image/gif"
        ;; WebP: 52 49 46 46 ?? ?? ?? ?? 57 45 42 50 (RIFF + WEBP at offset 8)
        (and (= 0x52 b0) (= 0x49 b1) (= 0x46 b2) (= 0x46 b3)
             (= 0x57 b8) (= 0x45 b9) (= 0x42 b10) (= 0x50 b11)) "image/webp"
        :else nil))))
