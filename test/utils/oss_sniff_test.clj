(ns utils.oss-sniff-test
  "minweb.utils.oss-sniff 单测 —— magic byte 嗅探。
   修产线 bug(2026-07-11):钉钉 WebView 对相册 .png 文件 multipart Content-Type 设
   成 image/png 但实际字节是 JPEG,后端信任 → OSS 存错 → photo-proxy 透传 → 浏览器
   按 PNG 解码 JPEG 失败 → img.onerror。这里 sniff 真实格式 override。"
  (:require
   [clojure.test :refer [deftest is testing]]
   [minweb.utils.oss-sniff :as sniff]))

(defn- jpeg-bytes []
  ;; JPEG magic: FF D8 FF E0 00 10 4A 46 49 46 00 01
  (byte-array [(unchecked-byte 0xff) (unchecked-byte 0xd8) (unchecked-byte 0xff)
               (unchecked-byte 0xe0) 0x00 0x10
               (unchecked-byte 0x4a) (unchecked-byte 0x46) (unchecked-byte 0x49) (unchecked-byte 0x46)
               0x00 0x01]))

(defn- png-bytes []
  ;; PNG magic: 89 50 4E 47 0D 0A 1A 0A
  (byte-array [(unchecked-byte 0x89) 0x50 (unchecked-byte 0x4e) 0x47
               0x0d 0x0a 0x1a 0x0a
               0x00 0x00 0x00 0x0d]))

(defn- gif87-bytes []
  ;; GIF87a: 47 49 46 38 37 61
  (byte-array [0x47 0x49 0x46 0x38 0x37 0x61 0x00 0x00 0x00 0x00 0x00 0x00]))

(defn- gif89-bytes []
  ;; GIF89a: 47 49 46 38 39 61
  (byte-array [0x47 0x49 0x46 0x38 0x39 0x61 0x00 0x00 0x00 0x00 0x00 0x00]))

(defn- webp-bytes []
  ;; WebP: 52 49 46 46 ?? ?? ?? ?? 57 45 42 50
  (byte-array [0x52 0x49 0x46 0x46 0x00 0x00 0x00 0x00
               0x57 0x45 0x42 0x50]))

(deftest detect-jpeg
  (is (= "image/jpeg" (sniff/detect-image-content-type (jpeg-bytes)))))

(deftest detect-png
  (is (= "image/png" (sniff/detect-image-content-type (png-bytes)))))

(deftest detect-gif87-and-89
  (is (= "image/gif" (sniff/detect-image-content-type (gif87-bytes))))
  (is (= "image/gif" (sniff/detect-image-content-type (gif89-bytes)))))

(deftest detect-webp
  (is (= "image/webp" (sniff/detect-image-content-type (webp-bytes)))))

(deftest unknown-returns-nil
  (testing "非图片 / 不够 12 字节 / 任意垃圾 → nil(让 caller 退回 multipart mime)"
    (is (nil? (sniff/detect-image-content-type (.getBytes "hello world" "UTF-8"))))
    (is (nil? (sniff/detect-image-content-type (byte-array []))))
    (is (nil? (sniff/detect-image-content-type (byte-array [0x00 0x01]))))
    (is (nil? (sniff/detect-image-content-type nil)))))

(deftest jpeg-with-different-third-byte
  (testing "JPEG 第 3 字节是 marker,可以是 FF D8 FF E0(APP0)/FF D8 FF E1(APP1/EXIF)
           /FF D8 FF DB(DQT)/FF D8 FF FE(comment) 等。嗅探只看前 3 字节。"
    (let [cases [[0xe0] [0xe1] [0xdb] [0xfe] [0xc0] [0xc4]]]
      (doseq [[b3] cases]
        (let [bs (byte-array [(unchecked-byte 0xff) (unchecked-byte 0xd8) (unchecked-byte 0xff)
                              b3 0 0 0 0 0 0 0 0])]
          (is (= "image/jpeg" (sniff/detect-image-content-type bs))
              (str "应识别 0xff 0xd8 0xff + " (format "0x%02x" b3))))))))
