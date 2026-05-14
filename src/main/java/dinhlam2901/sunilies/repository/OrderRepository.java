package dinhlam2901.sunilies.repository;

import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import dinhlam2901.sunilies.model.Order;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class OrderRepository {

    private static final String COLLECTION = "orders";

    private Firestore db() {
        return FirestoreClient.getFirestore();
    }

    // ── Lưu đơn hàng mới ──────────────────────────────────
    public void save(Order order) throws Exception {
        db().collection(COLLECTION)
                .document(order.getId())
                .set(order)
                .get();
    }

    // ── Tìm theo orderId ───────────────────────────────────
    public Order findById(String orderId) throws Exception {
        DocumentSnapshot doc = db().collection(COLLECTION)
                .document(orderId)
                .get().get();
        if (!doc.exists()) return null;
        return doc.toObject(Order.class);
    }

    // ── Tìm tất cả đơn của một user, mới nhất trước ───────
    public List<Order> findByUserId(String userId) throws Exception {
        QuerySnapshot snap = db().collection(COLLECTION)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().get();

        List<Order> orders = new ArrayList<>();
        for (DocumentSnapshot doc : snap.getDocuments()) {
            Order o = doc.toObject(Order.class);
            if (o != null) orders.add(o);
        }
        return orders;
    }

    // ── Cập nhật trạng thái sau khi nhận IPN ──────────────
    public void updatePaymentResult(String orderId,
                                    String status,
                                    String momoTransId) throws Exception {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status",      status);
        updates.put("momoTransId", momoTransId);
        updates.put("updatedAt",   System.currentTimeMillis());

        db().collection(COLLECTION)
                .document(orderId)
                .update(updates)
                .get();
    }
}