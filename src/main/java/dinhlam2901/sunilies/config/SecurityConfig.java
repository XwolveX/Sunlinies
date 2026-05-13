package dinhlam2901.sunilies.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig – Tắt form login mặc định của Spring Security.
 * Auth được xử lý thủ công qua AuthService + HttpSession.
 * Chỉ dùng Spring Security để lấy BCryptPasswordEncoder.
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
                // Tắt CSRF (Spring Boot dùng session riêng)
                .csrf(csrf -> csrf.disable());

        return http.build();
    }
}