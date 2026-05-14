package dinhlam2901.sunilies.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;

/**
 * MomoPaymentService – toàn bộ logic giao tiếp với MoMo Payment Gateway v2
 *
 * Cấu hình trong application.properties:
 *   momo.partner-code=...
 *   momo.access-key=...
 *   momo.secret-key=...
 *   momo.endpoint=https://test-payment.momo.vn/v2/gateway/api/create
 *   momo.redirect-url=http://localhost:8080/payment/momo/return
 *   momo.ipn-url=http://localhost:8080/payment/momo/ipn
 */
@Service
public class MomoPaymentService {

    @Value("${momo.partner-code}")
    private String partnerCode;

    @Value("${momo.access-key}")
    private String accessKey;

    @Value("${momo.secret-key}")
    private String secretKey;

    @Value("${momo.endpoint}")
    private String endpoint;

    @Value("${momo.redirect-url}")
    private String redirectUrl;

    @Value("${momo.ipn-url}")
    private String ipnUrl;

    // ══════════════════════════════════════════════════════
    // TẠO REQUEST THANH TOÁN → trả về payUrl
    // ══════════════════════════════════════════════════════
    public String createPayment(String orderId, long amount, String orderInfo) throws Exception {

        String requestId   = partnerCode + "_" + UUID.randomUUID().toString().replace("-", "");
        String requestType = "captureWallet";
        String extraData   = "";  // base64 nếu cần truyền thêm data

        // Bước 1: build rawSignature ĐÚNG THỨ TỰ (theo tài liệu MoMo)
        String rawSignature = "accessKey="   + accessKey
                + "&amount="      + amount
                + "&extraData="   + extraData
                + "&ipnUrl="      + ipnUrl
                + "&orderId="     + orderId
                + "&orderInfo="   + orderInfo
                + "&partnerCode=" + partnerCode
                + "&redirectUrl=" + redirectUrl
                + "&requestId="   + requestId
                + "&requestType=" + requestType;

        String signature = hmacSha256(rawSignature, secretKey);

        // Bước 2: build JSON body
        String body = "{"
                + "\"partnerCode\":\""  + partnerCode  + "\","
                + "\"accessKey\":\""    + accessKey    + "\","
                + "\"requestId\":\""    + requestId    + "\","
                + "\"amount\":"         + amount       + ","
                + "\"orderId\":\""      + orderId      + "\","
                + "\"orderInfo\":\""    + orderInfo    + "\","
                + "\"redirectUrl\":\"" + redirectUrl  + "\","
                + "\"ipnUrl\":\""       + ipnUrl       + "\","
                + "\"extraData\":\""    + extraData    + "\","
                + "\"requestType\":\"" + requestType  + "\","
                + "\"signature\":\""   + signature    + "\","
                + "\"lang\":\"vi\""
                + "}";

        // Bước 3: gọi MoMo API
        String response = post(endpoint, body);
        System.out.println("✅ MoMo response: " + response);

        // Bước 4: parse payUrl (tránh dùng thêm thư viện JSON)
        return extractJson(response, "payUrl");
    }

    // ══════════════════════════════════════════════════════
    // VERIFY SIGNATURE (dùng cho cả IPN và Return URL)
    // ══════════════════════════════════════════════════════
    public boolean verifySignature(String receivedSignature,
                                   String requestId,
                                   String orderId,
                                   long   amount,
                                   String orderInfo,
                                   String orderType,
                                   long   transId,
                                   int    resultCode,
                                   String message,
                                   String payType,
                                   long   responseTime,
                                   String extraData) throws Exception {

        // Thứ tự trường theo tài liệu MoMo callback
        String rawSignature = "accessKey="    + accessKey
                + "&amount="       + amount
                + "&extraData="    + extraData
                + "&message="      + message
                + "&orderId="      + orderId
                + "&orderInfo="    + orderInfo
                + "&orderType="    + orderType
                + "&partnerCode="  + partnerCode
                + "&payType="      + payType
                + "&requestId="    + requestId
                + "&responseTime=" + responseTime
                + "&resultCode="   + resultCode
                + "&transId="      + transId;

        String expected = hmacSha256(rawSignature, secretKey);
        return expected.equals(receivedSignature);
    }

    // ══════════════════════════════════════════════════════
    // HELPER: HMAC SHA256
    // ══════════════════════════════════════════════════════
    private String hmacSha256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        // Java 17+: HexFormat; nếu dùng Java 11 thì thay bằng bytesToHex bên dưới
        return HexFormat.of().formatHex(hash);
    }

    // Fallback cho Java 11 (uncomment nếu cần):
    // private String bytesToHex(byte[] bytes) {
    //     StringBuilder sb = new StringBuilder();
    //     for (byte b : bytes) sb.append(String.format("%02x", b));
    //     return sb.toString();
    // }

    // ══════════════════════════════════════════════════════
    // HELPER: HTTP POST
    // ══════════════════════════════════════════════════════
    private String post(String urlStr, String jsonBody) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 400) ? conn.getErrorStream() : conn.getInputStream();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    // ══════════════════════════════════════════════════════
    // HELPER: parse một trường từ JSON string đơn giản
    // ══════════════════════════════════════════════════════
    private String extractJson(String json, String key) {
        // Tìm "key":"value" hoặc "key":value (số)
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) {
            // thử dạng số
            search = "\"" + key + "\":";
            start = json.indexOf(search);
            if (start == -1) return null;
            start += search.length();
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            return json.substring(start, end).trim();
        }
        start += search.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    // ── Getters cho Controller ─────────────────────────────
    public String getPartnerCode() { return partnerCode; }
    public String getAccessKey()   { return accessKey; }
}