package dinhlam2901.sunilies.controller;

import dinhlam2901.sunilies.model.User;
import dinhlam2901.sunilies.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    @Autowired private AuthService authService;

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
        // Validate
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
            ra.addFlashAttribute("success",
                    "Đăng ký thành công! Vui lòng kiểm tra email và nhập mã OTP.");
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

            // Đăng nhập luôn sau khi verify thành công
            session.setAttribute("SUNILIES_USER", user.getId());

            ra.addFlashAttribute("success", "Xác minh email thành công! Chào mừng bạn đến SUNILIES 🎉");

            // Nếu chưa có SĐT thì bỏ qua, nếu có thì redirect verify phone
            if (user.getPhone() != null && !user.getPhone().isBlank()
                    && !user.isPhoneVerified()) {
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

        // Tự động gửi OTP khi vào trang
        String devOtp = authService.generatePhoneOtp(user.getId());
        model.addAttribute("phone", maskPhone(user.getPhone()));
        model.addAttribute("userId", user.getId());

        // Dev mode: hiển thị OTP (xoá khi production)
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
        return "auth/account";
    }

    // ── Helper: mask số điện thoại ────────────────────────
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 3);
    }
}