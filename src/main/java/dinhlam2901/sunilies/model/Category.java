package dinhlam2901.sunilies.model;

public class Category {
    private String handle;
    private String name;
    private String imageUrl;

    public Category(String handle, String name, String imageUrl) {
        this.handle = handle;
        this.name = name;
        this.imageUrl = imageUrl;
    }

    public String getHandle() { return handle; }
    public void setHandle(String handle) { this.handle = handle; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}