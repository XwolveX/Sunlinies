package dinhlam2901.sunilies.repository;

import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import dinhlam2901.sunilies.model.HeroSlide;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Repository
public class HeroSlideRepository {

    private static final String COLLECTION = "hero_slides";

    private Firestore db() {
        return FirestoreClient.getFirestore();
    }

    /** Lấy tất cả slide đang active, sắp xếp theo sortOrder (cho homepage) */
    public List<HeroSlide> findAllActive() throws ExecutionException, InterruptedException {
        return db().collection(COLLECTION)
                .whereEqualTo("active", true)
                .get().get()
                .getDocuments().stream()
                .map(this::toSlide)
                .sorted(Comparator.comparingInt(HeroSlide::getSortOrder))
                .collect(Collectors.toList());
    }

    /** Lấy tất cả slide (kể cả ẩn, cho admin) */
    public List<HeroSlide> findAll() throws ExecutionException, InterruptedException {
        return db().collection(COLLECTION)
                .get().get()
                .getDocuments().stream()
                .map(this::toSlide)
                .sorted(Comparator.comparingInt(HeroSlide::getSortOrder))
                .collect(Collectors.toList());
    }

    public HeroSlide findById(String id) throws ExecutionException, InterruptedException {
        DocumentSnapshot doc = db().collection(COLLECTION).document(id).get().get();
        if (!doc.exists()) return null;
        return toSlide(doc);
    }

    public String save(HeroSlide slide) throws ExecutionException, InterruptedException {
        DocumentReference ref = db().collection(COLLECTION).document();
        slide.setId(ref.getId());
        ref.set(slide).get();
        return ref.getId();
    }

    public void update(HeroSlide slide) throws ExecutionException, InterruptedException {
        db().collection(COLLECTION).document(slide.getId()).set(slide).get();
    }

    public void toggleActive(String id, boolean active) throws ExecutionException, InterruptedException {
        db().collection(COLLECTION).document(id).update("active", active).get();
    }

    public void hardDelete(String id) throws ExecutionException, InterruptedException {
        db().collection(COLLECTION).document(id).delete().get();
    }

    private HeroSlide toSlide(DocumentSnapshot doc) {
        HeroSlide s = doc.toObject(HeroSlide.class);
        if (s != null) s.setId(doc.getId());
        return s;
    }
}
