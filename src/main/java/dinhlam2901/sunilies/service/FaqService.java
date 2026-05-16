package dinhlam2901.sunilies.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * FaqService – Load và match FAQ từ file chat-faq.json
 *
 *  Cách hoạt động:
 *   1. Load file JSON khi app khởi động
 *   2. Chuẩn hoá câu hỏi (bỏ dấu, lowercase) để match dễ hơn
 *   3. Tính điểm = tổng độ dài keyword match → câu nào điểm cao nhất thắng
 *   4. Trả null nếu không có match đủ mạnh (tránh false positive)
 *
 *  File JSON: src/main/resources/chat-faq.json
 *  Format:
 *    [
 *      {
 *        "question": "Phí ship",
 *        "keywords": ["phi ship", "ship bao nhieu", ...],
 *        "answer": "Phí ship của SUNILIES là..."
 *      }
 *    ]
 */
@Service
public class FaqService {

    private List<FaqEntry> faqs = new ArrayList<>();

    @PostConstruct
    public void loadFaqs() {
        try (InputStream is = new ClassPathResource("chat-faq.json").getInputStream()) {
            ObjectMapper mapper = new ObjectMapper();
            faqs = mapper.readValue(is, new TypeReference<List<FaqEntry>>() {});
            System.out.println("✅ Loaded " + faqs.size() + " FAQ entries");
        } catch (Exception e) {
            System.err.println("⚠️ Không load được chat-faq.json: " + e.getMessage());
        }
    }

    /**
     * Tìm câu trả lời phù hợp nhất bằng keyword matching.
     * @return câu trả lời, hoặc null nếu không match được FAQ nào
     */
    public String findAnswer(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) return null;

        String normalized = normalize(userMessage);

        FaqEntry bestMatch = null;
        int bestScore = 0;

        for (FaqEntry faq : faqs) {
            if (faq.keywords == null) continue;

            int score = 0;
            for (String keyword : faq.keywords) {
                String nk = normalize(keyword);
                if (nk.isEmpty()) continue;
                if (normalized.contains(nk)) {
                    // keyword dài → điểm cao hơn (chính xác hơn)
                    score += nk.length();
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestMatch = faq;
            }
        }

        // Yêu cầu điểm tối thiểu để tránh match nhầm câu ngắn
        if (bestMatch != null && bestScore >= 4) {
            return bestMatch.answer;
        }
        return null;
    }

    /** Bỏ dấu tiếng Việt + lowercase + bỏ ký tự đặc biệt */
    private String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace("đ", "d").replace("Đ", "D");
        return n.toLowerCase().trim();
    }

    /**
     * Cấu trúc 1 mục FAQ trong JSON.
     * Phải là public + có default constructor để Jackson deserialize được.
     */
    public static class FaqEntry {
        public String question;
        public List<String> keywords;
        public String answer;

        public FaqEntry() {}
    }
}
