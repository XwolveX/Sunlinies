package dinhlam2901.sunilies.service;

import com.google.firebase.cloud.StorageClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class FirebaseStorageService {

    @Value("${firebase.storage.bucket}")
    private String bucketName;

    public String uploadBlogImage(MultipartFile file) throws IOException {
        return upload(file, "blog-images");
    }

    public String uploadHeroImage(MultipartFile file) throws IOException {
        return upload(file, "hero-images");
    }

    public String uploadProductImage(MultipartFile file) throws IOException {
        return upload(file, "product-images");
    }

    private String upload(MultipartFile file, String folder) throws IOException {
        if (file == null || file.isEmpty()) return null;

        String blobName = folder + "/" + UUID.randomUUID() + getExtension(file.getOriginalFilename());

        String token = UUID.randomUUID().toString();
        java.util.Map<String, String> metadata = new java.util.HashMap<>();
        metadata.put("firebaseStorageDownloadTokens", token);

        com.google.cloud.storage.Bucket bucket = StorageClient.getInstance().bucket(bucketName);
        com.google.cloud.storage.BlobInfo blobInfo = com.google.cloud.storage.BlobInfo.newBuilder(bucket.getName(), blobName)
                .setContentType(file.getContentType())
                .setMetadata(metadata)
                .build();

        bucket.getStorage().create(blobInfo, file.getInputStream());

        return "https://firebasestorage.googleapis.com/v0/b/"
                + bucketName + "/o/"
                + URLEncoder.encode(blobName, StandardCharsets.UTF_8).replace("+", "%20")
                + "?alt=media&token=" + token;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }
}
