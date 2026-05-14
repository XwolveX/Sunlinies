package dinhlam2901.sunilies.model;

import java.io.Serializable;
import com.google.cloud.firestore.annotation.IgnoreExtraProperties;

/**
 * CartItem – một dòng trong giỏ hàng
 * Lưu trong HTTP Session nên phải implements Serializable
 */
@IgnoreExtraProperties
public class CartItem implements Serializable {

    private String productId;
    private String handle;
    private String name;
    private String image;
    private Double price;
    private Double comparePrice;
    private String size;
    private String color;
    private int    qty;

    public CartItem() {}

    public CartItem(String productId, String handle, String name,
                    String image, Double price, Double comparePrice,
                    String size, String color, int qty) {
        this.productId    = productId;
        this.handle       = handle;
        this.name         = name;
        this.image        = image;
        this.price        = price;
        this.comparePrice = comparePrice;
        this.size         = size;
        this.color        = color;
        this.qty          = qty;
    }

    /** Key dùng để nhận diện item (product + size + color) */
    @com.google.cloud.firestore.annotation.Exclude
    public String getKey() {
        return productId + "_" + (size != null ? size : "") + "_" + (color != null ? color : "");
    }

    @com.google.cloud.firestore.annotation.Exclude
    public double getSubtotal() {
        return price != null ? price * qty : 0;
    }

    // ── Getters & Setters ──────────────────────────────────
    public String getProductId()    { return productId; }
    public void   setProductId(String v) { this.productId = v; }

    public String getHandle()       { return handle; }
    public void   setHandle(String v) { this.handle = v; }

    public String getName()         { return name; }
    public void   setName(String v) { this.name = v; }

    public String getImage()        { return image; }
    public void   setImage(String v) { this.image = v; }

    public Double getPrice()        { return price; }
    public void   setPrice(Double v) { this.price = v; }

    public Double getComparePrice() { return comparePrice; }
    public void   setComparePrice(Double v) { this.comparePrice = v; }

    public String getSize()         { return size; }
    public void   setSize(String v) { this.size = v; }

    public String getColor()        { return color; }
    public void   setColor(String v) { this.color = v; }

    public int    getQty()          { return qty; }
    public void   setQty(int v)     { this.qty = v; }
}