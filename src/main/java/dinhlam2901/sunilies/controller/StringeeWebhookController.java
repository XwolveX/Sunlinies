package dinhlam2901.sunilies.controller;

import org.springframework.web.bind.annotation.*;

/**
 * StringeeWebhookController
 *
 * Stringee sẽ gọi GET đến endpoint này khi có cuộc gọi đi
 * Trả về SCCO (Stringee Call Control Object) để robot đọc OTP
 *
 * URL: GET /api/stringee/scco?otp=123456
 */
@RestController
@RequestMapping("/api/stringee")
public class StringeeWebhookController {

    /**
     * Stringee gọi đến đây để lấy kịch bản cuộc gọi
     * Trả về SCCO JSON với action "say" để đọc OTP
     */
    @GetMapping("/scco")
    public String getScco(@RequestParam(required = false) String otp) {
        if (otp == null || otp.isBlank()) {
            otp = "Xin loi, ma xac thuc khong hop le.";
        }

        // Đọc từng chữ số để dễ nghe hơn (1 2 3 4 5 6 thay vì 123456)
        String otpSpaced = otp.chars()
                .mapToObj(c -> String.valueOf((char) c))
                .reduce("", (a, b) -> a.isEmpty() ? b : a + " " + b);

        // SCCO: đọc OTP 3 lần với giọng Việt
        String scco = "["
                + "{"
                + "\"action\": \"say\","
                + "\"text\": \"Xin chao. Day la cuoc goi tu SUNILIES. Ma OTP cua ban la: "
                + otpSpaced + ". Nhac lai: " + otpSpaced + ". Ma se het han sau 5 phut.\","
                + "\"voice\": \"male\","
                + "\"language\": \"vi-VN\""
                + "}"
                + "]";

        System.out.println("📋 SCCO served for OTP: " + otp);
        return scco;
    }
}