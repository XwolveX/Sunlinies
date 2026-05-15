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
    @Autowired private StringeeVoiceService stringeeVoiceService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private static final String SESSION_USER = "SUNILIES_USER";

    // ════════════════════════════════════════════════════════
    // ĐĂNG KÝ
    // ════════════════════════════════════════════════════════
    public User register(String email, String password,
                         String fullName, String phone) throws Exception {
        email = email.toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email này đã được đăng ký.");
        }
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

        String otp = generateOtp();
        user.setEmailOtp(encoder.encode(otp));
        user.setOtpExpiry(expireAfter(10));
        userRepository.save(user);
        emailService.sendVerificationOtp(email, fullName, otp);
        return user;
    }

    // ════════════════════════════════════════════════════════
    // XÁC MINH EMAIL OTP
    // ════════════════════════════════════════════════════════
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
        user.setLastLogin(new Date());
        userRepository.update(user);
        session.setAttribute(SESSION_USER, user.getId());
        session.setAttribute("SUNILIES_ROLE", user.getRole());
        return user;
    }

    // ════════════════════════════════════════════════════════
    // XÁC MINH SỐ ĐIỆN THOẠI - STRINGEE VOICE OTP
    // ════════════════════════════════════════════════════════
    public String generatePhoneOtp(String userId) throws Exception {
        User user = userRepository.findById(userId);
        if (user == null) throw new IllegalArgumentException("Tài khoản không tồn tại.");
        if (user.isPhoneVerified()) throw new IllegalStateException("Số điện thoại đã được xác minh.");

        String otp = generateOtp();
        user.setPhoneOtp(encoder.encode(otp));
        user.setPhoneOtpExpiry(expireAfter(5));
        userRepository.update(user);

        // Gọi Stringee Voice OTP
        // Nếu stringee.enabled=false → dev mode, không gọi thật
        try {
            stringeeVoiceService.callOtp(user.getPhone(), otp);
        } catch (Exception e) {
            System.err.println("⚠️ Stringee thất bại: " + e.getMessage());
            System.out.println("🔧 DEV MODE — OTP: " + otp);
        }

        return otp; // Trả về để hiện dev box khi stringee.enabled=false
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
    // SESSION / LOGOUT
    // ════════════════════════════════════════════════════════
    public void logout(HttpSession session) {
        session.removeAttribute(SESSION_USER);
        session.invalidate();
    }

    public User getCurrentUser(HttpSession session) throws Exception {
        String uid = (String) session.getAttribute(SESSION_USER);
        if (uid == null) return null;
        return userRepository.findById(uid);
    }

    public boolean isLoggedIn(HttpSession session) {
        return session.getAttribute(SESSION_USER) != null;
    }

    private String generateOtp() {
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }

    private Date expireAfter(int minutes) {
        return new Date(System.currentTimeMillis() + (long) minutes * 60 * 1000);
    }
}