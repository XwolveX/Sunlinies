package dinhlam2901.sunilies.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dinhlam2901.sunilies.model.Product;
import dinhlam2901.sunilies.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
@Service
public class GeminiService {

    @Value("${gemini.enabled:false}")
    private boolean enabled;

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String model;

    @Value("${gemini.max-products:20}")
    private int maxProducts;

    @Autowired
    private ProductRepository productRepository;

    private final ObjectMapper mapper = new ObjectMapper();
    private String staticKnowledge = "";

    // Cache danh sách sản phẩm (5 phút) — tránh gọi Firestore mỗi lần chat
    private String cachedProductsText = "";
    private long   cachedAt = 0;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;

    @PostConstruct
    public void init() {
        loadStaticKnowledge();
    }

    private void loadStaticKnowledge() {
        try (InputStream is = new ClassPathResource("chat-knowledge-base.txt").getInputStream();
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            staticKnowledge = sb.toString();
            System.out.println("✅ Loaded knowledge base (" + staticKnowledge.length() + " chars)");
        } catch (Exception e) {
            System.err.println("⚠️ Không load được chat-knowledge-base.txt: " + e.getMessage());
        }
    }

    public String ask(String userMessage, List<Map<String, String>> history) throws Exception {

        if (!enabled) {
            return "Dạ câu hỏi này em chưa có thông tin sẵn. Bạn vui lòng inbox fanpage SUNILIES "
                 + "để được tư vấn chi tiết hơn nhé ạ! 🌾";
        }
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("❌ Chưa cấu hình gemini.api-key trong application.properties");
            return "Xin lỗi, hệ thống chatbot đang được bảo trì. Bạn liên hệ trực tiếp shop nhé ạ!";
        }

        String systemPrompt = buildSystemPrompt();
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/"
                        + model + ":generateContent?key=" + apiKey;

        // ──── Build request body ─────────────────────────────
        ObjectNode root = mapper.createObjectNode();

        // system_instruction = bối cảnh + tài liệu
        ObjectNode systemInstruction = root.putObject("system_instruction");
        ArrayNode sysParts = systemInstruction.putArray("parts");
        sysParts.addObject().put("text", systemPrompt);

        // contents = lịch sử + câu hỏi hiện tại
        ArrayNode contents = root.putArray("contents");

        if (history != null) {
            for (Map<String, String> turn : history) {
                String role = turn.getOrDefault("role", "user");
                String text = turn.getOrDefault("text", "");
                if (text == null || text.isBlank()) continue;

                ObjectNode item = contents.addObject();
                item.put("role", "user".equals(role) ? "user" : "model");
                ArrayNode parts = item.putArray("parts");
                parts.addObject().put("text", text);
            }
        }

        // Câu hỏi hiện tại
        ObjectNode currentTurn = contents.addObject();
        currentTurn.put("role", "user");
        ArrayNode currentParts = currentTurn.putArray("parts");
        currentParts.addObject().put("text", userMessage);

        // Cấu hình sinh
        ObjectNode genConfig = root.putObject("generationConfig");
        genConfig.put("temperature", 0.7);
        genConfig.put("maxOutputTokens", 400);
        genConfig.put("topP", 0.9);

        String requestBody = mapper.writeValueAsString(root);

        // ──── Gọi API ───────────────────────────────────────
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(20000);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        String response = readResponse(conn);

        if (code != 200) {
            System.err.println("❌ Gemini API error [" + code + "]: " + response);
            return "Xin lỗi, em đang gặp chút sự cố kỹ thuật. Bạn thử lại sau hoặc inbox fanpage giúp em nhé!";
        }

        return parseAnswer(response);
    }

    /**
     * Build system prompt: persona + chính sách + sản phẩm hiện có
     */
    private String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();

        sb.append("Bạn là trợ lý ảo của SUNILIES – thương hiệu phụ kiện handmade cói mây Việt Nam.\n");
        sb.append("Phong cách trả lời:\n");
        sb.append("  • Thân thiện, lịch sự, xưng \"em\" - gọi khách là \"bạn\".\n");
        sb.append("  • NGẮN GỌN: tối đa 3-4 câu, có thể dùng emoji 🌾✨ nhẹ nhàng.\n");
        sb.append("  • CHỈ trả lời dựa trên TÀI LIỆU bên dưới. Không bịa thông tin.\n");
        sb.append("  • Nếu không có thông tin → lịch sự xin lỗi và đề nghị liên hệ fanpage.\n");
        sb.append("  • KHÔNG bịa giá, KHÔNG bịa tên sản phẩm không có trong danh sách.\n");
        sb.append("  • Nếu khách hỏi sản phẩm cụ thể → giới thiệu kèm giá từ danh sách.\n\n");

        sb.append("═══ THÔNG TIN SHOP & CHÍNH SÁCH ═══\n");
        sb.append(staticKnowledge).append("\n");

        sb.append("═══ DANH SÁCH SẢN PHẨM HIỆN CÓ ═══\n");
        sb.append(getProductsText());

        return sb.toString();
    }

    /**
     * Lấy danh sách sản phẩm — có cache 5 phút
     */
    private String getProductsText() {
        long now = System.currentTimeMillis();
        if (!cachedProductsText.isEmpty() && (now - cachedAt) < CACHE_TTL_MS) {
            return cachedProductsText;
        }

        StringBuilder sb = new StringBuilder();
        try {
            List<Product> products = productRepository.findAll();
            int count = 0;
            for (Product p : products) {
                if (count >= maxProducts) break;
                if (p.getName() == null) continue;

                sb.append("- ").append(p.getName());
                if (p.getPrice() != null) {
                    sb.append(" | Giá: ").append(String.format("%,.0f", p.getPrice())).append("đ");
                }
                if (p.getCategory() != null && !p.getCategory().isBlank()) {
                    sb.append(" | Danh mục: ").append(p.getCategory());
                }
                if (p.getHandle() != null) {
                    sb.append(" | Link: /products/").append(p.getHandle());
                }
                if (p.getStock() <= 0) sb.append(" | ⚠ HẾT HÀNG");
                sb.append("\n");
                count++;
            }
            if (count == 0) sb.append("(Chưa có sản phẩm)\n");
        } catch (Exception e) {
            System.err.println("⚠️ Không load được products cho chatbot: " + e.getMessage());
            sb.append("(Không lấy được danh sách sản phẩm)\n");
        }

        cachedProductsText = sb.toString();
        cachedAt = now;
        return cachedProductsText;
    }

    /** Parse text trả lời từ JSON response của Gemini */
    private String parseAnswer(String json) {
        try {
            JsonNode root = mapper.readTree(json);

            // Check blocked content (safety filter)
            JsonNode promptFeedback = root.path("promptFeedback").path("blockReason");
            if (!promptFeedback.isMissingNode()) {
                return "Dạ câu hỏi này em chưa thể trả lời. Bạn hỏi em câu khác về sản phẩm nhé ạ!";
            }

            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && parts.size() > 0) {
                    String text = parts.get(0).path("text").asText("").trim();
                    if (!text.isEmpty()) return text;
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Parse Gemini response error: " + e.getMessage());
        }
        return "Dạ em chưa hiểu rõ câu hỏi. Bạn có thể nói rõ hơn được không ạ?";
    }

    private String readResponse(HttpURLConnection conn) {
        try {
            InputStream is = conn.getResponseCode() >= 400
                    ? conn.getErrorStream() : conn.getInputStream();
            if (is == null) return "";
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                return sb.toString();
            }
        } catch (Exception e) {
            return "";
        }
    }
}
