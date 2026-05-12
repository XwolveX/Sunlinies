package dinhlam2901.sunilies.seeder;

import dinhlam2901.sunilies.model.Product;
import dinhlam2901.sunilies.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Chạy 1 lần khi khởi động app để seed dữ liệu mẫu vào Firestore.
 * Sau khi seed xong → xoá @Component hoặc set SEED_ENABLED = false
 */
@Component
public class DataSeeder implements CommandLineRunner {

    // ⚠️ Đổi thành false sau khi đã seed xong để tránh duplicate
    private static final boolean SEED_ENABLED = false;

    @Autowired
    private ProductRepository productRepository;

    @Override
    public void run(String... args) throws Exception {
        if (!SEED_ENABLED) {
            System.out.println("⏭️  DataSeeder disabled, bỏ qua.");
            return;
        }

        System.out.println("🌱 Bắt đầu seed dữ liệu sản phẩm...");

        List<Product> products = List.of(

                // ── BAG ──────────────────────────────────────────────
                createProduct(
                        "sandy-bag",
                        "Sandy Bag",
                        "Túi tote vải canvas phong cách minimalist",
                        650000.0, 800000.0,
                        "/image/SandyBag.jpg",
                        List.of("/image/SandyBag.jpg", "/image/SandyBag_2.jpg"),
                        "bag", "dang-ha",
                        true, false, true,
                        List.of("One Size"),
                        List.of("Nâu", "Kem", "Đen")
                ),

                createProduct(
                        "lumer-hat",
                        "Lumer Hat",
                        "Mũ cói đan tay phong cách hè",
                        420000.0, null,
                        "/image/LumerHat.JPG",
                        List.of("/image/LumerHat.JPG"),
                        "hat", "vi-may",
                        false, true, true,
                        List.of("Free Size"),
                        List.of("Tự nhiên", "Đen")
                ),

                createProduct(
                        "bag-charm",
                        "Bag Charm",
                        "Phụ kiện túi handmade độc đáo",
                        350000.0, 500000.0,
                        "/image/bag_charm.jpeg",
                        List.of("/image/bag_charm.jpeg"),
                        "accessory", "dang-ha",
                        true, false, true,
                        List.of("One Size"),
                        List.of("Nhiều màu")
                ),

                createProduct(
                        "ao-thun-basic",
                        "Áo Thun Basic",
                        "Áo thun cotton 100% form rộng basic",
                        299000.0, 399000.0,
                        "/image/img_cus_1.jpg",
                        List.of("/image/img_cus_1.jpg"),
                        "ao", "vi-may",
                        true, true, true,
                        List.of("S", "M", "L", "XL"),
                        List.of("Trắng", "Đen", "Be")
                ),

                createProduct(
                        "quan-jeans-slim",
                        "Quần Jeans Slim",
                        "Quần jeans dáng slim fit co giãn nhẹ",
                        499000.0, null,
                        "/image/img_cus_2.jpg",
                        List.of("/image/img_cus_2.jpg"),
                        "quan", "dang-ha",
                        false, true, true,
                        List.of("S", "M", "L"),
                        List.of("Xanh đậm", "Xanh nhạt")
                ),

                createProduct(
                        "vay-hoa-nhi",
                        "Váy Hoa Nhí",
                        "Váy midi họa tiết hoa nhí vintage",
                        399000.0, 550000.0,
                        "/image/img_cus_3.jpg",
                        List.of("/image/img_cus_3.jpg"),
                        "vay", "vi-may",
                        true, false, true,
                        List.of("S", "M", "L"),
                        List.of("Hồng", "Xanh")
                ),

                createProduct(
                        "ao-khoac-denim",
                        "Áo Khoác Denim",
                        "Áo khoác denim oversize unisex",
                        699000.0, null,
                        "/image/img_cus_4.jpg",
                        List.of("/image/img_cus_4.jpg"),
                        "ao", "dang-ha",
                        false, false, true,
                        List.of("S", "M", "L", "XL"),
                        List.of("Xanh đậm")
                ),

                createProduct(
                        "trending-set",
                        "Trending Set",
                        "Bộ set trang phục theo xu hướng mới nhất",
                        700000.0, 900000.0,
                        "/image/trending.JPG",
                        List.of("/image/trending.JPG"),
                        "ao", "dang-ha",
                        true, true, true,
                        List.of("S", "M", "L"),
                        List.of("Trắng", "Đen")
                )
        );

        int count = 0;
        for (Product p : products) {
            try {
                productRepository.save(p);
                System.out.println("  ✅ Đã thêm: " + p.getName());
                count++;
            } catch (Exception e) {
                System.err.println("  ❌ Lỗi thêm " + p.getName() + ": " + e.getMessage());
            }
        }

        System.out.println("🎉 Seed hoàn tất! Đã thêm " + count + "/" + products.size() + " sản phẩm.");
        System.out.println("⚠️  Nhớ đổi SEED_ENABLED = false trong DataSeeder.java!");
    }

    // ─── Helper tạo Product ───────────────────────────────────────
    private Product createProduct(
            String handle, String name, String description,
            Double price, Double comparePrice, String imageUrl,
            List<String> images, String category, String collection,
            boolean onSale, boolean isNew, boolean hot,
            List<String> sizes, List<String> colors
    ) {
        Product p = new Product();
        p.setHandle(handle);
        p.setName(name);
        p.setDescription(description);
        p.setPrice(price);
        p.setComparePrice(comparePrice);
        p.setImageUrl(imageUrl);
        p.setImages(images);
        p.setCategory(category);
        p.setCollection(collection);
        p.setOnSale(onSale);
        p.setNew(isNew);
        p.setHot(hot);
        p.setActive(true);
        p.setStock(50);
        p.setSizes(sizes);
        p.setColors(colors);
        return p;
    }
}