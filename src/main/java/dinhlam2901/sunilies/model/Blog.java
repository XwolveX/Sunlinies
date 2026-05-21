package dinhlam2901.sunilies.model;

import com.google.cloud.firestore.annotation.DocumentId;

public class Blog {

    @DocumentId
    private String id;

    private String slug;          // URL slug: "phu-vinh-lang-nghe"
    private String title;         // Tiêu đề bài viết
    private String excerpt;       // Tóm tắt ngắn
    private String content;       // Nội dung HTML đầy đủ
    private String imageUrl;      // Ảnh đại diện
    private String category;      // "Làng nghề" | "Phong cách" | "Chăm sóc sản phẩm" | "Bền vững"
    private int    readTime;      // Số phút đọc
    private boolean published;    // Hiển thị trên web?
    private boolean featured;     // Bài nổi bật (hiện ở hero)?
    private long   createdAt;     // Timestamp ms
    private long   updatedAt;

    // SEO
    private String metaTitle;
    private String metaDescription;

    public Blog() {}

    // ── Getters & Setters ──────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getExcerpt() { return excerpt; }
    public void setExcerpt(String excerpt) { this.excerpt = excerpt; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getReadTime() { return readTime; }
    public void setReadTime(int readTime) { this.readTime = readTime; }

    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }

    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public String getMetaTitle() { return metaTitle; }
    public void setMetaTitle(String metaTitle) { this.metaTitle = metaTitle; }

    public String getMetaDescription() { return metaDescription; }
    public void setMetaDescription(String metaDescription) { this.metaDescription = metaDescription; }
}
