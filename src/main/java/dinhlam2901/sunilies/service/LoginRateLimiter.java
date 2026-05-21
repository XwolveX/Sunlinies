package dinhlam2901.sunilies.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LoginRateLimiter – Chống brute-force đăng nhập.
 *
 * Sau MAX_ATTEMPTS lần thất bại liên tiếp trong WINDOW_MS,
 * key bị khóa LOCK_MS và không thể thử thêm.
 *
 * Key = email (lowercase). Dữ liệu lưu in-memory, tự xóa khi hết hạn.
 */
@Component
public class LoginRateLimiter {

    /** Số lần thất bại tối đa trước khi khóa */
    private static final int MAX_ATTEMPTS = 5;

    /** Thời gian khóa sau khi vượt quá MAX_ATTEMPTS (15 phút) */
    private static final long LOCK_MS = 15 * 60 * 1_000L;

    private final ConcurrentHashMap<String, AttemptRecord> records = new ConcurrentHashMap<>();

    // ─── Public API ───────────────────────────────────────────────

    /**
     * Kiểm tra xem key có đang bị khóa không.
     * Tự động dọn dẹp record đã hết hạn.
     */
    public boolean isBlocked(String key) {
        key = normalize(key);
        AttemptRecord r = records.get(key);
        if (r == null) return false;

        if (r.lockedUntil > System.currentTimeMillis()) {
            return true;   // đang bị khóa
        }
        if (r.lockedUntil > 0) {
            records.remove(key); // hết hạn → dọn dẹp
        }
        return false;
    }

    /**
     * Số giây còn lại cho đến khi mở khóa (0 nếu không bị khóa).
     */
    public long secondsRemaining(String key) {
        AttemptRecord r = records.get(normalize(key));
        if (r == null || r.lockedUntil == 0) return 0;
        return Math.max(0, (r.lockedUntil - System.currentTimeMillis()) / 1_000);
    }

    /**
     * Số lần thử thất bại hiện tại (trước khi bị khóa).
     */
    public int attemptsRemaining(String key) {
        AttemptRecord r = records.get(normalize(key));
        if (r == null) return MAX_ATTEMPTS;
        return Math.max(0, MAX_ATTEMPTS - r.count.get());
    }

    /**
     * Ghi nhận một lần đăng nhập thất bại.
     * Khi đạt MAX_ATTEMPTS, khóa key trong LOCK_MS.
     */
    public void recordFailure(String key) {
        key = normalize(key);
        AttemptRecord r = records.computeIfAbsent(key, k -> new AttemptRecord());
        int count = r.count.incrementAndGet();
        if (count >= MAX_ATTEMPTS) {
            r.lockedUntil = System.currentTimeMillis() + LOCK_MS;
        }
    }

    /**
     * Xóa bộ đếm khi đăng nhập thành công.
     */
    public void recordSuccess(String key) {
        records.remove(normalize(key));
    }

    // ─── Internal ────────────────────────────────────────────────

    private String normalize(String key) {
        return key == null ? "" : key.toLowerCase().trim();
    }

    private static class AttemptRecord {
        final AtomicInteger count = new AtomicInteger(0);
        volatile long lockedUntil = 0;
    }
}
