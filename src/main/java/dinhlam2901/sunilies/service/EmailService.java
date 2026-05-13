package dinhlam2901.sunilies.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Gửi OTP xác minh tài khoản
     */
    public void sendVerificationOtp(String toEmail, String fullName, String otp) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");

            helper.setFrom(fromEmail, "SUNILIES");
            helper.setTo(toEmail);
            helper.setSubject("Xác minh tài khoản SUNILIES — Mã OTP của bạn");
            helper.setText(buildOtpEmail(fullName, otp, "xác minh tài khoản", 10), true);

            mailSender.send(msg);
            System.out.println("✅ OTP email sent to: " + toEmail);

        } catch (Exception e) {
            System.err.println("❌ Failed to send email to " + toEmail + ": " + e.getMessage());
            throw new RuntimeException("Không thể gửi email xác minh: " + e.getMessage());
        }
    }

    /**
     * Gửi OTP đặt lại mật khẩu
     */
    public void sendPasswordResetOtp(String toEmail, String fullName, String otp) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");

            helper.setFrom(fromEmail, "SUNILIES");
            helper.setTo(toEmail);
            helper.setSubject("Đặt lại mật khẩu SUNILIES — Mã OTP của bạn");
            helper.setText(buildOtpEmail(fullName, otp, "đặt lại mật khẩu", 10), true);

            mailSender.send(msg);
        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email: " + e.getMessage());
        }
    }

    // ── HTML template email OTP ───────────────────────────
    private String buildOtpEmail(String name, String otp, String purpose, int expireMinutes) {
        return """
            <!DOCTYPE html>
            <html lang="vi">
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background:#f5f0e8;font-family:'Helvetica Neue',Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0">
                <tr><td align="center" style="padding:40px 20px;">
                  <table width="520" cellpadding="0" cellspacing="0"
                         style="background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);">

                    <!-- Header -->
                    <tr>
                      <td style="background:#3d2e1c;padding:32px;text-align:center;">
                        <h1 style="margin:0;color:#faf2e6;font-size:28px;font-weight:300;letter-spacing:8px;">
                          SUNILIES
                        </h1>
                        <p style="margin:6px 0 0;color:#c8986a;font-size:11px;letter-spacing:3px;text-transform:uppercase;">
                          Handmade &amp; Artisan
                        </p>
                      </td>
                    </tr>

                    <!-- Body -->
                    <tr>
                      <td style="padding:40px 40px 32px;">
                        <p style="margin:0 0 12px;font-size:16px;color:#3d2e1c;">
                          Xin chào <strong>%s</strong>,
                        </p>
                        <p style="margin:0 0 28px;font-size:14px;color:#7a6a57;line-height:1.7;">
                          Đây là mã OTP để <strong>%s</strong> trên SUNILIES.<br>
                          Mã có hiệu lực trong <strong>%d phút</strong>.
                        </p>

                        <!-- OTP Box -->
                        <div style="text-align:center;margin:28px 0;">
                          <div style="display:inline-block;background:#faf2e6;border:2px solid #c8986a;
                                      border-radius:12px;padding:20px 48px;">
                            <span style="font-size:36px;font-weight:700;color:#3d2e1c;letter-spacing:10px;">
                              %s
                            </span>
                          </div>
                        </div>

                        <p style="margin:0;font-size:12px;color:#aaa;line-height:1.6;text-align:center;">
                          Không yêu cầu mã này? Vui lòng bỏ qua email này.<br>
                          Không chia sẻ mã OTP với bất kỳ ai.
                        </p>
                      </td>
                    </tr>

                    <!-- Footer -->
                    <tr>
                      <td style="background:#faf7f2;padding:20px 40px;text-align:center;
                                 border-top:1px solid #e8e4de;">
                        <p style="margin:0;font-size:11px;color:#bbb;">
                          © 2024 SUNILIES — Handmade &amp; Artisan<br>
                          Email này được gửi tự động, vui lòng không reply.
                        </p>
                      </td>
                    </tr>

                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(name, purpose, expireMinutes, otp);
    }
}