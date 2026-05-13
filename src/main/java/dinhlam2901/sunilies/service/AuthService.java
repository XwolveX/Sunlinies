package dinhlam2901.sunilies.service;

import dinhlam2901.sunilies.model.User;
import dinhlam2901.sunilies.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Date;

@Service
public class AuthService {

    @Autowired private UserRepository userRepository;
    @Autowired private EmailService emailService;
    @Autowired private SmsService smsService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private static final String SESSION_USER = "SUNILIES_USER";

    // ════════════════════════════════════════════════════════
    // ĐĂNG KÝ
    // ════════════════════════════════════════════════════════

    /**
     * Bước 1: Đăng ký tài khoản → tạo user chưa xác minh → gửi OTP email
     */
    public User register(String email, String password,
                         String fullName, String phone) throws Exception {

        email = email.toLowerCase().trim();

        // Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email này đã được đăng ký.");
        }

        // Tạo user
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(password));
        user.setFullName(fullName.trim());
        user.setPhone(phone != null ? phone.trim() : null);
        user.setRole("USER");
        user.setActive(true);
        user.setEmailVerified(false);
        user.setPhoneVerified(false);
        user.setCreatedAt(new Date());

        // Tạo OTP email
        String otp = generateOtp();
        user.setEmailOtp(encoder.encode(otp));   // lưu hash
        user.setOtpExpiry(expireAfter(10));       // hết hạn 10 phút

        // Lưu vào Firestore
        userRepository.save(user);

        // Gửi OTP qua email
        emailService.sendVerificationOtp(email, fullName, otp);

        return user;
    }

    // ════════════════════════════════════════════════════════
    // XÁC MINH EMAIL OTP
    // ════════════════════════════════════════════════════════

    /**
     * Bước 2: Xác minh OTP gửi qua email (1 lần duy nhất)
     */
    public User verifyEmailOtp(String email, String inputOtp) throws Exception {
        User user = userRepository.findByEmail(email);

        if (user == null) throw new IllegalArgumentException("Tài khoản không tồn tại.");
        if (user.isEmailVerified()) throw new IllegalStateException("Email đã được xác minh.");
        if (user.getOtpExpiry() == null || new Date().after(user.getOtpExpiry())) {
            throw new IllegalStateException("Mã OTP đã hết hạn. Vui lòng đăng ký lại.");
        }
        if (!encoder.matches(inputOtp.trim(), user.getEmailOtp())) {
            throw new IllegalArgumentException("Mã OTP không đúng.");
        }

        // Xác minh thành công
        user.setEmailVerified(true);
        user.setEmailOtp(null);
        user.setOtpExpiry(null);
        userRepository.update(user);

        return user;
    }

    // ════════════════════════════════════════════════════════
    // ĐĂNG NHẬP
    // ════════════════════════════════════════════════════════

    public User login(String email, String password,
                      HttpSession session) throws Exception {
        email = email.toLowerCase().trim();
        User user = userRepository.findByEmail(email);

        if (user == null) throw new IllegalArgumentException("Email không tồn tại.");
        if (!user.isActive()) throw new IllegalStateException("Tài khoản đã bị khoá.");
        if (!user.isEmailVerified()) throw new IllegalStateException("EMAIL_NOT_VERIFIED");
        if (!encoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu không đúng.");
        }

        // Cập nhật lần đăng nhập
        user.setLastLogin(new Date());
        userRepository.update(user);

        // Lưu vào session
        session.setAttribute(SESSION_USER, user.getId());
        return user;
    }

    // ════════════════════════════════════════════════════════
    // XÁC MINH SỐ ĐIỆN THOẠI (1 lần duy nhất)
    // ════════════════════════════════════════════════════════

    /**
     * Tạo OTP SĐT — hiển thị cho user nhập (demo: log ra console)
     * Production: tích hợp Twilio / ESMS
     */
    public String generatePhoneOtp(String userId) throws Exception {
        User user = userRepository.findById(userId);
        if (user == null) throw new IllegalArgumentException("Tài khoản không tồn tại.");
        if (user.isPhoneVerified()) throw new IllegalStateException("Số điện thoại đã được xác minh.");

        String otp = generateOtp();
        user.setPhoneOtp(encoder.encode(otp));
        user.setPhoneOtpExpiry(expireAfter(5));  // 5 phút
        userRepository.update(user);

        // Gửi OTP qua SpeedSMS
        smsService.sendOtp(user.getPhone(), otp);
        System.out.println("📱 SMS OTP sent to: " + user.getPhone());

        return otp;
    }

    public User verifyPhoneOtp(String userId, String inputOtp) throws Exception {
        User user = userRepository.findById(userId);
        if (user == null) throw new IllegalArgumentException("Tài khoản không tồn tại.");
        if (user.isPhoneVerified()) throw new IllegalStateException("Số điện thoại đã được xác minh.");
        if (user.getPhoneOtpExpiry() == null || new Date().after(user.getPhoneOtpExpiry())) {
            throw new IllegalStateException("Mã OTP đã hết hạn.");
        }
        if (!encoder.matches(inputOtp.trim(), user.getPhoneOtp())) {
            throw new IllegalArgumentException("Mã OTP không đúng.");
        }

        user.setPhoneVerified(true);
        user.setPhoneOtp(null);
        user.setPhoneOtpExpiry(null);
        userRepository.update(user);
        return user;
    }

    // ════════════════════════════════════════════════════════
    // ĐĂNG XUẤT
    // ════════════════════════════════════════════════════════

    public void logout(HttpSession session) {
        session.removeAttribute(SESSION_USER);
        session.invalidate();
    }

    // ════════════════════════════════════════════════════════
    // SESSION HELPER
    // ════════════════════════════════════════════════════════

    public User getCurrentUser(HttpSession session) throws Exception {
        String uid = (String) session.getAttribute(SESSION_USER);
        if (uid == null) return null;
        return userRepository.findById(uid);
    }

    public boolean isLoggedIn(HttpSession session) {
        return session.getAttribute(SESSION_USER) != null;
    }

    // ════════════════════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════════════════════

    private String generateOtp() {
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }

    private Date expireAfter(int minutes) {
        return new Date(System.currentTimeMillis() + (long) minutes * 60 * 1000);
    }
}