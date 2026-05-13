package dinhlam2901.sunilies.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * SmsService – gửi SMS OTP qua SpeedSMS API v2
 * Docs: https://speedsms.vn/docs/
 */
@Service
public class SmsService {

    @Value("${speedsms.access-token}")
    private String accessToken;

    @Value("${speedsms.sender:}")
    private String sender; // Brandname hoặc để trống dùng số mặc định

    private static final String API_URL = "https://api.speedsms.vn/index.php/sms/send";

    /**
     * Gửi OTP xác minh SĐT
     */
    public void sendOtp(String phone, String otp) {
        String message = "[SUNILIES] Ma OTP xac minh so dien thoai cua ban la: "
                + otp + ". Het han sau 5 phut. Khong chia se ma nay cho bat ky ai.";
        send(phone, message, 2); // type 2 = CSKH/OTP
    }

    /**
     * Gửi SMS tùy ý
     * @param phone   Số điện thoại VN (0901234567)
     * @param message Nội dung
     * @param type    2=CSKH, 8=Quảng cáo
     */
    public void send(String phone, String message, int type) {
        try {
            // Chuẩn hóa số điện thoại: 0901... → +84901...
            String normalizedPhone = normalizePhone(phone);

            // Build JSON body
            String body = "{"
                    + "\"to\":[\"" + normalizedPhone + "\"],"
                    + "\"content\":\"" + escapeJson(message) + "\","
                    + "\"sms_type\":" + type
                    + (sender != null && !sender.isBlank()
                    ? ",\"sender\":\"" + sender + "\"" : "")
                    + "}";

            // Basic Auth: Base64(accessToken:x)
            String auth = Base64.getEncoder()
                    .encodeToString((accessToken + ":x").getBytes(StandardCharsets.UTF_8));

            // HTTP POST
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Basic " + auth);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            String response = readResponse(conn);

            if (responseCode == 200 && response.contains("\"status\":1")) {
                System.out.println("✅ SMS sent to " + phone + " | Response: " + response);
            } else {
                System.err.println("❌ SMS failed to " + phone
                        + " | Code: " + responseCode
                        + " | Response: " + response);
                throw new RuntimeException("Gửi SMS thất bại: " + response);
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("❌ SMS exception: " + e.getMessage());
            throw new RuntimeException("Không thể gửi SMS: " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────

    /**
     * Chuẩn hóa SĐT về dạng +84...
     * 0901234567 → +84901234567
     * 84901234567 → +84901234567
     */
    private String normalizePhone(String phone) {
        if (phone == null) throw new IllegalArgumentException("Số điện thoại không được để trống.");
        phone = phone.replaceAll("[^0-9+]", "");
        if (phone.startsWith("0"))      return "+84" + phone.substring(1);
        if (phone.startsWith("84"))     return "+"  + phone;
        if (phone.startsWith("+84"))    return phone;
        return phone;
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private String readResponse(HttpURLConnection conn) {
        try {
            var is = conn.getResponseCode() >= 400
                    ? conn.getErrorStream() : conn.getInputStream();
            if (is == null) return "";
            try (var br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                return sb.toString();
            }
        } catch (Exception e) {
            return "";
        }
    }
}