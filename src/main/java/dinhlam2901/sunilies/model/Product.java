package dinhlam2901.sunilies.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;

import java.util.List;

/**
 * Model sản phẩm – tương thích với Firestore
 *
 * Cấu trúc Firestore collection: "products"
 * Mỗi document = 1 sản phẩm
 */
public class Product {

    // ── Firestore document ID (tự động map khi dùng toObject()) ──
    @DocumentId
    private String id;

    // ── Thông tin cơ bản ──────────────────────────────────────────
    private String handle;          // URL slug, VD: "sandy-bag"
    private String name;            // Tên sản phẩm
    private String description;     // Mô tả ngắn
    private String fullDescription; // Mô tả chi tiết (HTML)

    // ── Giá ───────────────────────────────────────────────────────
    private Double price;           // Giá bán hiện tại
    private Double comparePrice;    // Giá gốc (để hiện gạch ngang)

    // ── Hình ảnh ──────────────────────────────────────────────────
    private String imageUrl;        // Ảnh đại diện chính
    private List<String> images;    // Danh sách tất cả ảnh

    // ── Phân loại ─────────────────────────────────────────────────
    private String category;        // VD: "bag", "hat", "accessory"
    private List<String> tags;      // VD: ["new", "summer", "coi-may"]
    private String collection;      // VD: "dang-ha", "vi-may"

    // ── Biến thể ──────────────────────────────────────────────────
    private List<String> sizes;     // VD: ["S", "M", "L", "XL"]
    private List<String> colors;    // VD: ["Nâu", "Kem", "Đen"]

    // ── Trạng thái ────────────────────────────────────────────────
    private boolean onSale;         // Đang giảm giá?
    private boolean isNew;          // Sản phẩm mới?
    private boolean hot;            // Bán chạy / nổi bật?
    private boolean active;         // Hiển thị trên web?
    private int stock;              // Số lượng tồn kho

    // ── SEO ───────────────────────────────────────────────────────
    private String metaTitle;
    private String metaDescription;

    // ═══════════════════════════════════════════════════════════════
    // Constructors
    // ═══════════════════════════════════════════════════════════════

    /** Constructor rỗng – BẮT BUỘC cho Firestore toObject() */
    public Product() {}

    /** Constructor nhanh dùng ở HomeController (dữ liệu mẫu) */
    public Product(String handle, String name, Double price,
                   Double comparePrice, String imageUrl) {
        this.handle       = handle;
        this.name         = name;
        this.price        = price;
        this.comparePrice = comparePrice;
        this.imageUrl     = imageUrl;
        this.active       = true;
        this.onSale       = comparePrice != null && comparePrice > price;
    }

    // ═══════════════════════════════════════════════════════════════
    // Helper methods
    // ═══════════════════════════════════════════════════════════════

    /** Tính % giảm giá để hiển thị badge "-30%" */
    public int getDiscountPercent() {
        if (comparePrice == null || comparePrice <= 0 || price == null) return 0;
        return (int) Math.round((1 - price / comparePrice) * 100);
    }

    /**
     * Setter giả cho discountPercent — Firestore cần setter khi deserialize.
     * Giá trị thực được tính động qua getDiscountPercent().
     */
    public void setDiscountPercent(int discountPercent) {
        // computed field — không lưu
    }

    /** Ảnh đầu tiên trong list (fallback về imageUrl) */
    public String getFeaturedImage() {
        if (images != null && !images.isEmpty()) return images.get(0);
        return imageUrl;
    }

    /**
     * Setter giả cho featuredImage — Firestore cần setter khi deserialize.
     * Giá trị thực được tính động qua getFeaturedImage().
     */
    public void setFeaturedImage(String featuredImage) {
        // computed field — không lưu
    }

    // ═══════════════════════════════════════════════════════════════
    // Getters & Setters
    // ═══════════════════════════════════════════════════════════════

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getHandle() { return handle; }
    public void setHandle(String handle) { this.handle = handle; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFullDescription() { return fullDescription; }
    public void setFullDescription(String fullDescription) { this.fullDescription = fullDescription; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Double getComparePrice() { return comparePrice; }
    public void setComparePrice(Double comparePrice) { this.comparePrice = comparePrice; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public String getCollection() { return collection; }
    public void setCollection(String collection) { this.collection = collection; }

    public List<String> getSizes() { return sizes; }
    public void setSizes(List<String> sizes) { this.sizes = sizes; }

    public List<String> getColors() { return colors; }
    public void setColors(List<String> colors) { this.colors = colors; }

    public boolean isOnSale() { return onSale; }
    public void setOnSale(boolean onSale) { this.onSale = onSale; }

    // Firestore lưu field "isNew" → dùng @PropertyName để map đúng
    @PropertyName("isNew")
    public boolean isNew() { return isNew; }

    @PropertyName("isNew")
    public void setNew(boolean isNew) { this.isNew = isNew; }

    public boolean isHot() { return hot; }
    public void setHot(boolean hot) { this.hot = hot; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public String getMetaTitle() { return metaTitle; }
    public void setMetaTitle(String metaTitle) { this.metaTitle = metaTitle; }

    public String getMetaDescription() { return metaDescription; }
    public void setMetaDescription(String metaDescription) { this.metaDescription = metaDescription; }
}