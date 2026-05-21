package dinhlam2901.sunilies.model;

import com.google.cloud.firestore.annotation.DocumentId;

/**
 * HeroSlide – mỗi slide trên hero homepage
 * Firestore collection: "hero_slides"
 */
public class HeroSlide {

    @DocumentId
    private String id;

    private String imageUrl;    // URL ảnh nền
    private String subtitle;    // Dòng nhỏ phía trên: "SUMMER COLLECTION 2025"
    private String titleLine1;  // Dòng 1 tiêu đề lớn: "Tự do"
    private String titleLine2;  // Dòng 2 tiêu đề lớn: "dưới nắng"
    private String buttonText;  // Text nút CTA: "Khám phá ngay"
    private String buttonLink;  // Link nút: "/collections"
    private boolean active;     // Hiển thị hay không
    private int sortOrder;      // Thứ tự (0, 1, 2...)

    public HeroSlide() {}

    // ── Getters & Setters ──────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public String getTitleLine1() { return titleLine1; }
    public void setTitleLine1(String titleLine1) { this.titleLine1 = titleLine1; }

    public String getTitleLine2() { return titleLine2; }
    public void setTitleLine2(String titleLine2) { this.titleLine2 = titleLine2; }

    public String getButtonText() { return buttonText; }
    public void setButtonText(String buttonText) { this.buttonText = buttonText; }

    public String getButtonLink() { return buttonLink; }
    public void setButtonLink(String buttonLink) { this.buttonLink = buttonLink; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
