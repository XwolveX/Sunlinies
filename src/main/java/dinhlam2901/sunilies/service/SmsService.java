package dinhlam2901.sunilies.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * SmsService – gửi SMS OTP qua SpeedSMS API
 * Dựa theo docs chính thức: field "brandname", response "status":"success"
 */
@Service
public class SmsService {

    @Value("${stringee.access-token}")
    private String accessToken;

    private static final String API_URL = "https://api.stringee.com/v1/sms";

    public void sendOtp(String phone, String otp) throws IOException {
        String message = "[SUNILIES] Ma OTP xac minh so dien thoai: "
                + otp + ". Het han sau 5 phut. Khong chia se ma nay.";
        // Thường Stringee dùng một BrandName hoặc số điện thoại tổng đài (Stringee)
        // Nếu bạn chưa đăng ký BrandName, hãy dùng "Stringee" hoặc tên người gửi được Stringee cấp.
        sendSMS(phone, message, "Stringee");
    }

    /**
     * Gửi SMS theo đúng format docs Stringee
     * @param to      SĐT người nhận (VD: 849xxxxxxxx)
     * @param content Nội dung tin nhắn
     * @param from    Tên thương hiệu hoặc số gửi
     */
    public void sendSMS(String to, String content, String from) throws IOException {
        String normalizedPhone = normalizePhone(to);

        // Build JSON đúng theo docs Stringee
        String json = "{"
                + "\"sms\": ["
                + "  {"
                + "    \"from\": \"" + from + "\","
                + "    \"to\": \"" + normalizedPhone + "\","
                + "    \"text\": \"" + content + "\""
                + "  }"
                + "]"
                + "}";

        System.out.println("📤 Stringee request: " + json);

        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("X-STRINGEE-AUTH", accessToken);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setDoOutput(true);

        // Ghi body
        try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
            // Stringee yêu cầu encode UTF-8
            wr.write(json.getBytes(StandardCharsets.UTF_8));
            wr.flush();
        }

        // Đọc response
        StringBuilder buffer = new StringBuilder();
        int responseCode = conn.getResponseCode();
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(
                        (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream(),
                        StandardCharsets.UTF_8))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                buffer.append(inputLine);
            }
        }

        String response = buffer.toString();
        System.out.println("📥 Stringee response: " + response);

        // Kiểm tra kết quả theo docs Stringee: "r": 0 là thành công
        if (response.contains("\"r\": 0") || response.contains("\"r\":0")) {
            System.out.println("✅ SMS sent to " + to);
        } else {
            System.err.println("❌ SMS failed: " + response);
            throw new RuntimeException("Gửi SMS thất bại: " + response);
        }
    }

    /**
     * Chuẩn hóa SĐT cho Stringee: Stringee yêu cầu format 84... không có dấu +
     * 0901... → 84901...
     */
    private String normalizePhone(String phone) {
        if (phone == null) throw new IllegalArgumentException("SĐT không được trống.");
        phone = phone.replaceAll("[^0-9]", ""); // Chỉ giữ lại số
        if (phone.startsWith("0")) {
            return "84" + phone.substring(1);
        }
        if (phone.startsWith("+84")) {
            return phone.substring(1);
        }
        if (phone.startsWith("84")) {
            return phone;
        }
        return phone; // Mặc định trả về chuỗi gốc nếu không xác định được
    }
}