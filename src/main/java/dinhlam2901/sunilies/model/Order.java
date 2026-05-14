package dinhlam2901.sunilies.model;

import java.util.List;

/**
 * Order – đơn hàng, lưu trong Firestore collection "orders"
 */
public class Order {

    private String id;              // Firestore document ID (= orderId)
    private String userId;          // null nếu guest checkout
    private String fullName;
    private String phone;
    private String address;
    private String note;

    private List<CartItem> items;
    private double totalAmount;

    // PENDING | PAID | FAILED | CANCELLED
    private String status;

    // MOMO | COD | ...
    private String paymentMethod;

    // MoMo trả về sau khi thanh toán
    private String momoTransId;     // transId từ MoMo
    private String momoRequestId;   // requestId gửi lên MoMo (để đối soát)

    private long createdAt;         // System.currentTimeMillis()
    private long updatedAt;

    public Order() {}

    // ── Getters & Setters ──────────────────────────────────

    public String getId()                     { return id; }
    public void   setId(String v)             { this.id = v; }

    public String getUserId()                 { return userId; }
    public void   setUserId(String v)         { this.userId = v; }

    public String getFullName()               { return fullName; }
    public void   setFullName(String v)       { this.fullName = v; }

    public String getPhone()                  { return phone; }
    public void   setPhone(String v)          { this.phone = v; }

    public String getAddress()                { return address; }
    public void   setAddress(String v)        { this.address = v; }

    public String getNote()                   { return note; }
    public void   setNote(String v)           { this.note = v; }

    public List<CartItem> getItems()          { return items; }
    public void   setItems(List<CartItem> v)  { this.items = v; }

    public double getTotalAmount()            { return totalAmount; }
    public void   setTotalAmount(double v)    { this.totalAmount = v; }

    public String getStatus()                 { return status; }
    public void   setStatus(String v)         { this.status = v; }

    public String getPaymentMethod()          { return paymentMethod; }
    public void   setPaymentMethod(String v)  { this.paymentMethod = v; }

    public String getMomoTransId()            { return momoTransId; }
    public void   setMomoTransId(String v)    { this.momoTransId = v; }

    public String getMomoRequestId()          { return momoRequestId; }
    public void   setMomoRequestId(String v)  { this.momoRequestId = v; }

    public long getCreatedAt()                { return createdAt; }
    public void setCreatedAt(long v)          { this.createdAt = v; }

    public long getUpdatedAt()                { return updatedAt; }
    public void setUpdatedAt(long v)          { this.updatedAt = v; }
}