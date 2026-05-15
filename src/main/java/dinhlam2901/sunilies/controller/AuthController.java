package dinhlam2901.sunilies.controller;

import dinhlam2901.sunilies.model.Order;
import dinhlam2901.sunilies.model.User;
import dinhlam2901.sunilies.repository.OrderRepository;
import dinhlam2901.sunilies.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class AuthController {

    @Autowired private AuthService       authService;
    @Autowired private OrderRepository   orderRepository;
    @Autowired private dinhlam2901.sunilies.repository.UserRepository userRepository;

    // ── Trang đăng nhập ──────────────────────────────────
    @GetMapping("/login")
    public String loginPage(HttpSession session, Model model,
                            @RequestParam(required = false) String redirect) {
        if (authService.isLoggedIn(session)) return "redirect:/account";
        model.addAttribute("redirect", redirect);
        return "auth/login";
    }

    @PostMapping("/login")
    public String loginSubmit(@RequestParam String email,
                              @RequestParam String password,
                              @RequestParam(required = false) String redirect,
                              HttpSession session,
                              RedirectAttributes ra) {
        try {
            authService.login(email, password, session);
            return "redirect:" + (redirect != null && !redirect.isBlank() ? redirect : "/account");
        } catch (IllegalStateException e) {
            if ("EMAIL_NOT_VERIFIED".equals(e.getMessage())) {
                ra.addFlashAttribute("error", "Email chưa được xác minh. Vui lòng kiểm tra hộp thư.");
                ra.addFlashAttribute("showResend", true);
                ra.addFlashAttribute("email", email);
            } else {
                ra.addFlashAttribute("error", e.getMessage());
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/login";
    }

    // ── Trang đăng ký ────────────────────────────────────
    @GetMapping("/register")
    public String registerPage(HttpSession session) {
        if (authService.isLoggedIn(session)) return "redirect:/account";
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerSubmit(@RequestParam String email,
                                 @RequestParam String password,
                                 @RequestParam String confirmPassword,
                                 @RequestParam String fullName,
                                 @RequestParam(required = false) String phone,
                                 RedirectAttributes ra) {
        if (!password.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "Mật khẩu xác nhận không khớp.");
            ra.addFlashAttribute("email", email);
            ra.addFlashAttribute("fullName", fullName);
            return "redirect:/register";
        }
        if (password.length() < 6) {
            ra.addFlashAttribute("error", "Mật khẩu tối thiểu 6 ký tự.");
            ra.addFlashAttribute("email", email);
            ra.addFlashAttribute("fullName", fullName);
            return "redirect:/register";
        }
        try {
            authService.register(email, password, fullName, phone);
            ra.addFlashAttribute("email", email);
            ra.addFlashAttribute("success", "Đăng ký thành công! Vui lòng kiểm tra email và nhập mã OTP.");
            return "redirect:/verify-email";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("email", email);
            ra.addFlashAttribute("fullName", fullName);
            return "redirect:/register";
        }
    }

    // ── Trang xác minh OTP email ──────────────────────────
    @GetMapping("/verify-email")
    public String verifyEmailPage(@ModelAttribute("email") String email, Model model) {
        return "auth/verify-email";
    }

    @PostMapping("/verify-email")
    public String verifyEmailSubmit(@RequestParam String email,
                                    @RequestParam String otp,
                                    HttpSession session,
                                    RedirectAttributes ra) {
        try {
            User user = authService.verifyEmailOtp(email, otp);
            session.setAttribute("SUNILIES_USER", user.getId());
            session.setAttribute("SUNILIES_ROLE", user.getRole());
            ra.addFlashAttribute("success", "Xác minh email thành công! Chào mừng bạn đến SUNILIES 🎉");
            if (user.getPhone() != null && !user.getPhone().isBlank() && !user.isPhoneVerified()) {
                return "redirect:/verify-phone";
            }
            return "redirect:/account";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("email", email);
            return "redirect:/verify-email";
        }
    }

    // ── Trang xác minh SĐT (1 lần duy nhất) ─────────────
    @GetMapping("/verify-phone")
    public String verifyPhonePage(HttpSession session, Model model) throws Exception {
        if (!authService.isLoggedIn(session)) return "redirect:/login";
        User user = authService.getCurrentUser(session);
        if (user == null) return "redirect:/login";
        if (user.isPhoneVerified()) return "redirect:/account";

        String devOtp = authService.generatePhoneOtp(user.getId());
        model.addAttribute("phone", maskPhone(user.getPhone()));
        model.addAttribute("userId", user.getId());
        model.addAttribute("devOtp", devOtp);
        return "auth/verify-phone";
    }

    @PostMapping("/verify-phone")
    public String verifyPhoneSubmit(@RequestParam String userId,
                                    @RequestParam String otp,
                                    HttpSession session,
                                    RedirectAttributes ra) {
        try {
            authService.verifyPhoneOtp(userId, otp);
            ra.addFlashAttribute("success", "Xác minh số điện thoại thành công!");
            return "redirect:/account";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("userId", userId);
            return "redirect:/verify-phone";
        }
    }

    // ── Đăng xuất ─────────────────────────────────────────
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        authService.logout(session);
        return "redirect:/login?logout";
    }

    // ── Trang account ──────────────────────────────────────
    @GetMapping("/account")
    public String accountPage(HttpSession session, Model model) throws Exception {
        if (!authService.isLoggedIn(session)) return "redirect:/login";
        User user = authService.getCurrentUser(session);
        if (user == null) return "redirect:/login";

        model.addAttribute("user", user);

        // Load đơn hàng của user
        try {
            List<Order> orders = orderRepository.findByUserId(user.getId());
            model.addAttribute("orders", orders);
        } catch (Exception e) {
            System.err.println("❌ Load orders lỗi: " + e.getMessage());
            model.addAttribute("orders", List.of());
        }

        return "auth/account";
    }

    // ── Helper: mask số điện thoại ────────────────────────
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 3);
    }

    // ── Cập nhật thông tin cá nhân ────────────────────────
    @PostMapping("/account/update")
    public String updateProfile(@RequestParam String fullName,
                                @RequestParam(required = false) String phone,
                                HttpSession session,
                                RedirectAttributes ra) throws Exception {
        if (!authService.isLoggedIn(session)) return "redirect:/login";
        User user = authService.getCurrentUser(session);
        if (user == null) return "redirect:/login";

        user.setFullName(fullName.trim());
        if (phone != null && !phone.isBlank()) user.setPhone(phone.trim());
        userRepository.update(user);

        ra.addFlashAttribute("success", "Cập nhật thông tin thành công!");
        return "redirect:/account";
    }

    // ── Đổi mật khẩu ──────────────────────────────────────
    @PostMapping("/account/change-password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 HttpSession session,
                                 RedirectAttributes ra) throws Exception {
        if (!authService.isLoggedIn(session)) return "redirect:/login";
        User user = authService.getCurrentUser(session);
        if (user == null) return "redirect:/login";

        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder
                = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

        if (!encoder.matches(currentPassword, user.getPasswordHash())) {
            ra.addFlashAttribute("error", "Mật khẩu hiện tại không đúng.");
            return "redirect:/account#password";
        }
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "Mật khẩu mới không khớp.");
            return "redirect:/account#password";
        }
        if (newPassword.length() < 6) {
            ra.addFlashAttribute("error", "Mật khẩu tối thiểu 6 ký tự.");
            return "redirect:/account#password";
        }

        user.setPasswordHash(encoder.encode(newPassword));
        userRepository.update(user);

        ra.addFlashAttribute("success", "Đổi mật khẩu thành công!");
        return "redirect:/account";
    }
}