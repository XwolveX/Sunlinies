package dinhlam2901.sunilies.repository;

import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import dinhlam2901.sunilies.model.Blog;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Repository
public class BlogRepository {

    private static final String COLLECTION = "blogs";

    private Firestore db() {
        return FirestoreClient.getFirestore();
    }

    // ─── Tất cả bài viết đã published (cho trang /blogs) ─────────
    public List<Blog> findAllPublished() throws ExecutionException, InterruptedException {
        return db().collection(COLLECTION)
                .whereEqualTo("published", true)
                .get().get()
                .getDocuments().stream()
                .map(this::toBlog)
                .sorted((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    // ─── Bài nổi bật (featured = true, published = true) ─────────
    public Blog findFeatured() throws ExecutionException, InterruptedException {
        QuerySnapshot qs = db().collection(COLLECTION)
                .whereEqualTo("published", true)
                .whereEqualTo("featured", true)
                .limit(1)
                .get().get();
        if (qs.isEmpty()) return null;
        return toBlog(qs.getDocuments().get(0));
    }

    // ─── Tìm theo slug (URL) ──────────────────────────────────────
    public Blog findBySlug(String slug) throws ExecutionException, InterruptedException {
        QuerySnapshot qs = db().collection(COLLECTION)
                .whereEqualTo("slug", slug)
                .limit(1)
                .get().get();
        if (qs.isEmpty()) return null;
        return toBlog(qs.getDocuments().get(0));
    }

    // ─── Tìm theo ID ─────────────────────────────────────────────
    public Blog findById(String id) throws ExecutionException, InterruptedException {
        DocumentSnapshot doc = db().collection(COLLECTION).document(id).get().get();
        if (!doc.exists()) return null;
        return toBlog(doc);
    }

    // ─── Tất cả bài (kể cả ẩn, cho admin) ───────────────────────
    public List<Blog> findAllAdmin() throws ExecutionException, InterruptedException {
        return db().collection(COLLECTION)
                .get().get()
                .getDocuments().stream()
                .map(this::toBlog)
                .sorted((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    // ─── Lưu bài mới ─────────────────────────────────────────────
    public String save(Blog blog) throws ExecutionException, InterruptedException {
        DocumentReference ref = db().collection(COLLECTION).document();
        blog.setId(ref.getId());
        ref.set(blog).get();
        return ref.getId();
    }

    // ─── Cập nhật ─────────────────────────────────────────────────
    public void update(Blog blog) throws ExecutionException, InterruptedException {
        db().collection(COLLECTION).document(blog.getId()).set(blog).get();
    }

    // ─── Ẩn bài (soft delete) ─────────────────────────────────────
    public void unpublish(String id) throws ExecutionException, InterruptedException {
        db().collection(COLLECTION).document(id).update("published", false).get();
    }

    // ─── Xoá vĩnh viễn ───────────────────────────────────────────
    public void hardDelete(String id) throws ExecutionException, InterruptedException {
        db().collection(COLLECTION).document(id).delete().get();
    }

    // ─── Toggle published ─────────────────────────────────────────
    public boolean togglePublished(String id) throws ExecutionException, InterruptedException {
        Blog blog = findById(id);
        if (blog == null) return false;
        boolean next = !blog.isPublished();
        db().collection(COLLECTION).document(id).update("published", next).get();
        return next;
    }

    // ─── Helper ───────────────────────────────────────────────────
    private Blog toBlog(DocumentSnapshot doc) {
        Blog b = doc.toObject(Blog.class);
        if (b != null) b.setId(doc.getId());
        return b;
    }
}
