package dinhlam2901.sunilies.model;

public class Product {
    private String handle;
    private String name;
    private Double price;
    private Double comparePrice;
    private String imageUrl;

    public Product(String handle, String name, Double price, Double comparePrice, String imageUrl) {
        this.handle = handle;
        this.name = name;
        this.price = price;
        this.comparePrice = comparePrice;
        this.imageUrl = imageUrl;
    }

    // Getters & Setters
    public String getHandle() { return handle; }
    public void setHandle(String handle) { this.handle = handle; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Double getComparePrice() { return comparePrice; }
    public void setComparePrice(Double comparePrice) { this.comparePrice = comparePrice; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
