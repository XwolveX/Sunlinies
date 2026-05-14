package dinhlam2901.sunilies.repository;

import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import dinhlam2901.sunilies.model.Product;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Repository
public class ProductRepository {

    private static final String COLLECTION = "products";

    private Firestore db() {
        return FirestoreClient.getFirestore();
    }

    // ─── Lấy tất cả sản phẩm đang active ────────────────────────
    public List<Product> findAll() throws ExecutionException, InterruptedException {
        return db().collection(COLLECTION)
                .whereEqualTo("active", true)
                .get().get()
                .getDocuments().stream()
                .map(this::toProduct)
                .collect(Collectors.toList());
    }

    // ─── Tìm sản phẩm theo handle (URL slug) ─────────────────────
    public Product findByHandle(String handle) throws ExecutionException, InterruptedException {
        QuerySnapshot qs = db().collection(COLLECTION)
                .whereEqualTo("handle", handle)
                .limit(1)
                .get().get();

        if (qs.isEmpty()) return null;
        return toProduct(qs.getDocuments().get(0));
    }

    // ─── Sản phẩm bán chạy (hot = true) ─────────────────────────
    public List<Product> findHotProducts(int limit) throws ExecutionException, InterruptedException {
        return db().collection(COLLECTION)
                .whereEqualTo("hot", true)
                .whereEqualTo("active", true)
                .limit(limit)
                .get().get()
                .getDocuments().stream()
                .map(this::toProduct)
                .collect(Collectors.toList());
    }
    //find id
    public Product findById(String id) throws ExecutionException, InterruptedException {
        DocumentSnapshot doc = db().collection(COLLECTION).document(id).get().get();
        if (!doc.exists()) return null;
        return toProduct(doc);
    }

    // ─── Sản phẩm mới (isNew = true) ─────────────────────────────
    public List<Product> findNewProducts(int limit) throws ExecutionException, InterruptedException {
        return db().collection(COLLECTION)
                .whereEqualTo("isNew", true)
                .whereEqualTo("active", true)
                .limit(limit)
                .get().get()
                .getDocuments().stream()
                .map(this::toProduct)
                .collect(Collectors.toList());
    }

    // ─── Sản phẩm theo category ───────────────────────────────────
    public List<Product> findByCategory(String category) throws ExecutionException, InterruptedException {
        return db().collection(COLLECTION)
                .whereEqualTo("category", category)
                .whereEqualTo("active", true)
                .get().get()
                .getDocuments().stream()
                .map(this::toProduct)
                .collect(Collectors.toList());
    }

    // ─── Sản phẩm theo collection ────────────────────────────────
    public List<Product> findByCollection(String collection) throws ExecutionException, InterruptedException {
        return db().collection(COLLECTION)
                .whereEqualTo("collection", collection)
                .whereEqualTo("active", true)
                .get().get()
                .getDocuments().stream()
                .map(this::toProduct)
                .collect(Collectors.toList());
    }

    // ─── Lưu sản phẩm mới ────────────────────────────────────────
    public String save(Product product) throws ExecutionException, InterruptedException {
        DocumentReference ref = db().collection(COLLECTION).document();
        product.setId(ref.getId());
        ref.set(product).get();
        return ref.getId();
    }

    // ─── Cập nhật sản phẩm ───────────────────────────────────────
    public void update(Product product) throws ExecutionException, InterruptedException {
        db().collection(COLLECTION)
                .document(product.getId())
                .set(product)
                .get();
    }

    // ─── Xoá sản phẩm (soft delete) ──────────────────────────────
    public void deactivate(String id) throws ExecutionException, InterruptedException {
        db().collection(COLLECTION)
                .document(id)
                .update("active", false)
                .get();
    }

    // ─── Helper: DocumentSnapshot → Product ──────────────────────
    private Product toProduct(DocumentSnapshot doc) {
        Product p = doc.toObject(Product.class);
        if (p != null) p.setId(doc.getId());
        return p;
    }
}