package dinhlam2901.sunilies.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * ChatService – Logic Hybrid xử lý câu hỏi khách hàng
 *
 *  Bước 1: Match keyword trong FAQ → trả về ngay (MIỄN PHÍ, nhanh)
 *  Bước 2: Không match → gọi Gemini API với knowledge base (tốn token)
 *
 *  Nhờ vậy 80-90% câu hỏi phổ biến (phí ship, đổi trả, giờ mở cửa…)
 *  được trả lời tức thì mà không tốn API quota.
 */
@Service
public class ChatService {

    @Autowired
    private FaqService faqService;

    @Autowired
    private GeminiService geminiService;

    public Map<String, Object> handleMessage(String message,
                                             List<Map<String, String>> history) throws Exception {

        // ──── Bước 1: Tìm trong FAQ ──────────────────────────
        String faqAnswer = faqService.findAnswer(message);
        if (faqAnswer != null) {
            return Map.of(
                    "reply",  faqAnswer,
                    "source", "faq"
            );
        }

        // ──── Bước 2: Gọi Gemini ─────────────────────────────
        String aiAnswer = geminiService.ask(message, history);
        return Map.of(
                "reply",  aiAnswer,
                "source", "ai"
        );
    }
}
