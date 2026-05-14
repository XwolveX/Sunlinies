package dinhlam2901.sunilies.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * StringeeVoiceService – Gọi điện đọc OTP qua Stringee Voice API
 *
 * Cần cấu hình trong application.properties:
 *   stringee.api-key-sid=SKxxxxxxxxxx
 *   stringee.api-key-secret=xxxxxxxxxx
 *   stringee.from-number=+84xxxxxxxxx   (số Stringee của bạn)
 *   stringee.webhook-url=https://yourdomain.com/api/stringee/scco
 *   stringee.enabled=true               (false = dev mode, hiện OTP trên màn hình)
 */
@Service
public class StringeeVoiceService {

    @Value("${stringee.api-key-sid:}")
    private String apiKeySid;

    @Value("${stringee.api-key-secret:}")
    private String apiKeySecret;

    @Value("${stringee.from-number:}")
    private String fromNumber;

    @Value("${stringee.webhook-url:}")
    private String webhookUrl;

    @Value("${stringee.enabled:false}")
    private boolean enabled;

    private static final String CALL_API = "https://api.stringee.com/v1/call2/callout";

    /**
     * Gọi điện đọc OTP cho user
     * @return OTP string (để hiển thị dev mode khi enabled=false)
     */
    public void callOtp(String toPhone, String otp) throws Exception {
        if (!enabled) {
            // DEV MODE: không gọi Stringee, chỉ log
            System.out.println("🔧 DEV MODE - Stringee disabled");
            System.out.println("📞 Sẽ gọi đến: " + toPhone + " | OTP: " + otp);
            return;
        }

        if (apiKeySid.isBlank() || apiKeySecret.isBlank()) {
            throw new RuntimeException("Chưa cấu hình Stringee API Key trong application.properties");
        }
        if (fromNumber.isBlank()) {
            throw new RuntimeException("Chưa cấu hình stringee.from-number");
        }
        if (webhookUrl.isBlank()) {
            throw new RuntimeException("Chưa cấu hình stringee.webhook-url");
        }

        String normalizedTo = normalizePhone(toPhone);
        String jwt = generateJwt();

        // Build request body
        // answer_url: Stringee sẽ gọi đến URL này để lấy SCCO (kịch bản đọc OTP)
        // Truyền OTP qua query param để webhook đọc và trả SCCO
        String answerUrl = webhookUrl + "?otp=" + otp;

        String body = "{"
                + "\"from\": {\"type\": \"external\", \"number\": \"" + fromNumber + "\", \"alias\": \"SUNILIES\"},"
                + "\"to\": [{\"type\": \"external\", \"number\": \"" + normalizedTo + "\", \"alias\": \"user\"}],"
                + "\"answer_url\": \"" + answerUrl + "\","
                + "\"actions\": []"
                + "}";

        System.out.println("📞 Stringee callout to: " + normalizedTo);
        System.out.println("📤 Request body: " + body);

        // Gọi API
        URL url = new URL(CALL_API);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("X-STRINGEE-AUTH", jwt);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        String response = readResponse(conn);
        System.out.println("📥 Stringee response [" + code + "]: " + response);

        if (code != 200 || response.contains("\"r\":1")) {
            throw new RuntimeException("Stringee gọi thất bại: " + response);
        }

        System.out.println("✅ Stringee call initiated to: " + toPhone);
    }

    /**
     * Tạo JWT token theo chuẩn Stringee
     * Header: { "typ":"JWT", "alg":"HS256", "cty":"stringee-api;v=1" }
     * Payload: { "jti":"SKxxx-timestamp", "iss":"SKxxx", "exp":..., "rest_api":true }
     */
    private String generateJwt() {
        long now = System.currentTimeMillis() / 1000;
        long exp = now + 3600; // hết hạn sau 1 giờ

        Map<String, Object> header = new HashMap<>();
        header.put("typ", "JWT");
        header.put("alg", "HS256");
        header.put("cty", "stringee-api;v=1");

        SecretKey key = Keys.hmacShaKeyFor(apiKeySecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .setHeader(header)
                .claim("jti", apiKeySid + "-" + now)
                .claim("iss", apiKeySid)
                .claim("exp", exp)
                .claim("rest_api", true)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private String normalizePhone(String phone) {
        if (phone == null) throw new IllegalArgumentException("SĐT không được trống.");
        phone = phone.replaceAll("[^0-9+]", "");
        if (phone.startsWith("0"))   return "+84" + phone.substring(1);
        if (phone.startsWith("84") && !phone.startsWith("+84")) return "+" + phone;
        return phone;
    }

    private String readResponse(HttpURLConnection conn) {
        try {
            InputStream is = conn.getResponseCode() >= 400
                    ? conn.getErrorStream() : conn.getInputStream();
            if (is == null) return "";
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}