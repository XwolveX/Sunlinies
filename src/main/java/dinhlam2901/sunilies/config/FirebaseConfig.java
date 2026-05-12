package dinhlam2901.sunilies.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount =
                        getClass().getClassLoader()
                                .getResourceAsStream("firebase-service-account.json");

                if (serviceAccount == null) {
                    throw new RuntimeException(
                            "Không tìm thấy firebase-service-account.json trong resources!");
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setProjectId("sunilies-431e5")
                        .build();

                FirebaseApp.initializeApp(options);
                System.out.println("✅ Firebase initialized successfully!");
            }
        } catch (Exception e) {
            System.err.println("❌ Firebase initialization failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}