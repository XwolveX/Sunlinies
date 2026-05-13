package dinhlam2901.sunilies.model;

import com.google.cloud.firestore.annotation.DocumentId;

import java.util.Date;

/**
 * User – tài khoản người dùng lưu trong Firestore collection "users"
 */
public class User {

    @DocumentId
    private String id;

    private String email;
    private String passwordHash;     // BCrypt hash
    private String fullName;
    private String phone;

    // Xác minh email
    private boolean emailVerified;   // false cho đến khi xác minh OTP
    private String  emailOtp;        // 6 chữ số, xoá sau khi verify
    private Date    otpExpiry;       // hết hạn sau 10 phút

    // Xác minh SĐT (1 lần duy nhất)
    private boolean phoneVerified;
    private String  phoneOtp;
    private Date    phoneOtpExpiry;

    // Thông tin thêm
    private String  role;            // "USER" | "ADMIN"
    private boolean active;
    private Date    createdAt;
    private Date    lastLogin;

    // ── Constructors ──────────────────────────────────────
    public User() {}

    // ── Getters & Setters ─────────────────────────────────
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public String getEmailOtp() { return emailOtp; }
    public void setEmailOtp(String emailOtp) { this.emailOtp = emailOtp; }

    public Date getOtpExpiry() { return otpExpiry; }
    public void setOtpExpiry(Date otpExpiry) { this.otpExpiry = otpExpiry; }

    public boolean isPhoneVerified() { return phoneVerified; }
    public void setPhoneVerified(boolean phoneVerified) { this.phoneVerified = phoneVerified; }

    public String getPhoneOtp() { return phoneOtp; }
    public void setPhoneOtp(String phoneOtp) { this.phoneOtp = phoneOtp; }

    public Date getPhoneOtpExpiry() { return phoneOtpExpiry; }
    public void setPhoneOtpExpiry(Date phoneOtpExpiry) { this.phoneOtpExpiry = phoneOtpExpiry; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getLastLogin() { return lastLogin; }
    public void setLastLogin(Date lastLogin) { this.lastLogin = lastLogin; }
}