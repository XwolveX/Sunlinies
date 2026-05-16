package dinhlam2901.sunilies.controller;

import dinhlam2901.sunilies.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ChatController – Xử lý chatbox AI cho khách hàng
 *
 *  GET  /api/chat/welcome  → tin nhắn chào mở đầu + gợi ý câu hỏi
 *  POST /api/chat          → gửi câu hỏi, nhận trả lời (Hybrid: FAQ + Gemini)
 *
 *  Body POST:
 *    {
 *      "message": "Phí ship bao nhiêu?",
 *      "history": [
 *        { "role": "user",  "text": "..." },
 *        { "role": "model", "text": "..." }
 *      ]
 *    }
 *
 *  Response:
 *    { "reply": "...", "source": "faq" | "ai" | "system" | "error" }
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping
    public Map<String, Object> chat(@RequestBody Map<String, Object> body) {
        String message = body.getOrDefault("message", "").toString();

        @SuppressWarnings("unchecked")
        List<Map<String, String>> history =
                (List<Map<String, String>>) body.getOrDefault("history", List.of());

        if (message.isBlank()) {
            return Map.of(
                    "reply",  "Dạ bạn chưa nhập câu hỏi. Em có thể giúp gì ạ?",
                    "source", "system"
            );
        }

        try {
            return chatService.handleMessage(message.trim(), history);
        } catch (Exception e) {
            System.err.println("❌ Chat error: " + e.getMessage());
            return Map.of(
                    "reply",  "Xin lỗi, em đang gặp chút sự cố. Bạn vui lòng thử lại sau hoặc inbox fanpage SUNILIES nhé ạ!",
                    "source", "error"
            );
        }
    }

    /**
     * Tin nhắn chào + danh sách gợi ý câu hỏi nhanh cho khách
     */
    @GetMapping("/welcome")
    public Map<String, Object> welcome() {
        return Map.of(
                "reply", "Xin chào bạn! 🌾 Em là trợ lý của SUNILIES. Em có thể giúp bạn tư vấn sản phẩm, "
                       + "phí ship, đổi trả, hoặc cách bảo quản túi cói. Bạn cần hỗ trợ gì ạ?",
                "suggestions", List.of(
                        "Sản phẩm nào đang hot?",
                        "Phí ship bao nhiêu?",
                        "Chính sách đổi trả?",
                        "Cách bảo quản túi cói?"
                )
        );
    }
}
