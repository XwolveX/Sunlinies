package dinhlam2901.sunilies.repository;

import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import dinhlam2901.sunilies.model.User;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ExecutionException;

@Repository
public class UserRepository {

    private static final String COL = "users";

    private Firestore db() { return FirestoreClient.getFirestore(); }

    // Tìm theo email
    public User findByEmail(String email) throws ExecutionException, InterruptedException {
        QuerySnapshot qs = db().collection(COL)
                .whereEqualTo("email", email.toLowerCase().trim())
                .limit(1).get().get();
        if (qs.isEmpty()) return null;
        DocumentSnapshot doc = qs.getDocuments().get(0);
        User u = doc.toObject(User.class);
        if (u != null) u.setId(doc.getId());
        return u;
    }

    // Tìm theo id
    public User findById(String id) throws ExecutionException, InterruptedException {
        DocumentSnapshot doc = db().collection(COL).document(id).get().get();
        if (!doc.exists()) return null;
        User u = doc.toObject(User.class);
        if (u != null) u.setId(doc.getId());
        return u;
    }

    // Tạo user mới
    public String save(User user) throws ExecutionException, InterruptedException {
        DocumentReference ref = db().collection(COL).document();
        user.setId(ref.getId());
        ref.set(user).get();
        return ref.getId();
    }

    // Cập nhật user
    public void update(User user) throws ExecutionException, InterruptedException {
        db().collection(COL).document(user.getId()).set(user).get();
    }

    // Kiểm tra email đã tồn tại chưa
    public boolean existsByEmail(String email) throws ExecutionException, InterruptedException {
        return findByEmail(email) != null;
    }
}