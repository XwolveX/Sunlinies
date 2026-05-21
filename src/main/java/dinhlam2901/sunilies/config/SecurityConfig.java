package dinhlam2901.sunilies.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * SecurityConfig – Tắt form login mặc định của Spring Security.
 * Auth được xử lý thủ công qua AuthService + HttpSession.
 * Thêm security headers để bảo vệ trình duyệt người dùng.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Cho phép tất cả request — auth tự quản lý qua Session
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                // Tắt form login mặc định
                .formLogin(form -> form.disable())
                // Tắt HTTP Basic
                .httpBasic(basic -> basic.disable())
                // Tắt CSRF (AJAX endpoints gọi không mang token)
                .csrf(csrf -> csrf.disable())
                // ─── Security Headers ───────────────────────────────
                .headers(headers -> headers
                        // X-Content-Type-Options: nosniff — chặn MIME sniffing
                        .contentTypeOptions(cto -> {})
                        // X-Frame-Options: DENY — chống clickjacking
                        .frameOptions(fo -> fo.deny())
                        // Referrer-Policy — giới hạn thông tin gửi kèm khi chuyển trang
                        .referrerPolicy(rp -> rp
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        // HSTS — bắt buộc HTTPS (có tác dụng khi deploy production)
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31_536_000L))
                        // Permissions-Policy — tắt các API trình duyệt không cần thiết
                        .permissionsPolicy(pp -> pp
                                .policy("camera=(), microphone=(), geolocation=(), payment=(self)"))
                );

        return http.build();
    }
}