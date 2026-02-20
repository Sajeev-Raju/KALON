package com.kalon.config;

import com.kalon.entity.*;
import com.kalon.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Create admin user if not exists
        if (!userRepository.existsByEmail("admin@kalon.com")) {
            User admin = User.builder()
                    .firstName("Admin")
                    .lastName("User")
                    .email("admin@kalon.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role(User.Role.ADMIN)
                    .isActive(true)
                    .build();
            admin = userRepository.save(admin);

            Cart adminCart = Cart.builder().user(admin).build();
            cartRepository.save(adminCart);
        }

        // Create sample categories and products if database is empty
        if (categoryRepository.count() == 0) {
            initializeCategories();
        }
    }

    private void initializeCategories() {
        // Men's Categories
        Category menCategory = Category.builder()
                .name("Men")
                .slug("men")
                .description("Men's Fashion Collection")
                .genderType(Category.GenderType.MEN)
                .displayOrder(1)
                .isActive(true)
                .build();
        menCategory = categoryRepository.save(menCategory);

        List<Category> menSubcategories = Arrays.asList(
                createSubcategory("Men's T-Shirts", "men-t-shirts", menCategory, Category.GenderType.MEN, 1),
                createSubcategory("Men's Shirts", "men-shirts", menCategory, Category.GenderType.MEN, 2),
                createSubcategory("Men's Hoodies", "men-hoodies", menCategory, Category.GenderType.MEN, 3),
                createSubcategory("Men's Joggers", "men-joggers", menCategory, Category.GenderType.MEN, 4),
                createSubcategory("Men's Jeans", "men-jeans", menCategory, Category.GenderType.MEN, 5),
                createSubcategory("Men's Shorts", "men-shorts", menCategory, Category.GenderType.MEN, 6)
        );
        categoryRepository.saveAll(menSubcategories);

        // Women's Categories
        Category womenCategory = Category.builder()
                .name("Women")
                .slug("women")
                .description("Women's Fashion Collection")
                .genderType(Category.GenderType.WOMEN)
                .displayOrder(2)
                .isActive(true)
                .build();
        womenCategory = categoryRepository.save(womenCategory);

        List<Category> womenSubcategories = Arrays.asList(
                createSubcategory("Women's T-Shirts", "women-t-shirts", womenCategory, Category.GenderType.WOMEN, 1),
                createSubcategory("Women's Tops", "women-tops", womenCategory, Category.GenderType.WOMEN, 2),
                createSubcategory("Women's Dresses", "women-dresses", womenCategory, Category.GenderType.WOMEN, 3),
                createSubcategory("Women's Hoodies", "women-hoodies", womenCategory, Category.GenderType.WOMEN, 4),
                createSubcategory("Women's Joggers", "women-joggers", womenCategory, Category.GenderType.WOMEN, 5)
        );
        categoryRepository.saveAll(womenSubcategories);

        // Sneakers Category
        Category sneakersCategory = Category.builder()
                .name("Sneakers")
                .slug("sneakers")
                .description("Footwear Collection")
                .genderType(Category.GenderType.UNISEX)
                .displayOrder(3)
                .isActive(true)
                .build();
        categoryRepository.save(sneakersCategory);

        // Create sample products
        createSampleProducts(menSubcategories.get(0)); // Men's T-Shirts
        createSampleProducts(menSubcategories.get(2)); // Men's Hoodies
        createSampleProducts(womenSubcategories.get(0)); // Women's T-Shirts
    }

    private Category createSubcategory(String name, String slug, Category parent, Category.GenderType genderType, int order) {
        return Category.builder()
                .name(name)
                .slug(slug)
                .parent(parent)
                .genderType(genderType)
                .displayOrder(order)
                .isActive(true)
                .build();
    }

    private void createSampleProducts(Category category) {
        String[] colors = {"Black", "White", "Navy", "Grey", "Maroon"};
        String[] sizes = {"S", "M", "L", "XL", "XXL"};
        String[] colorCodes = {"#000000", "#FFFFFF", "#000080", "#808080", "#800000"};

        for (int i = 1; i <= 5; i++) {
            BigDecimal price = new BigDecimal(799 + (i * 200));
            BigDecimal originalPrice = price.multiply(new BigDecimal("1.25"));

            Product product = Product.builder()
                    .name(category.getName() + " Style " + i)
                    .slug(category.getSlug() + "-style-" + i)
                    .description("Premium quality " + category.getName().toLowerCase() + " with comfortable fit and stylish design. Made from high-quality materials.")
                    .price(price)
                    .originalPrice(originalPrice)
                    .discountPercentage(20)
                    .category(category)
                    .brandName("Kalon")
                    .material("100% Cotton")
                    .careInstructions("Machine wash cold, tumble dry low")
                    .countryOfOrigin("India")
                    .isFeatured(i <= 2)
                    .isNewArrival(i == 1)
                    .isBestSeller(i == 2)
                    .isActive(true)
                    .averageRating(4.0 + (Math.random() * 0.9))
                    .reviewCount(10 + (int)(Math.random() * 90))
                    .build();

            product = productRepository.save(product);

            // Add product images
            ProductImage primaryImage = ProductImage.builder()
                    .product(product)
                    .imageUrl("https://via.placeholder.com/500x600?text=" + category.getName().replace(" ", "+") + "+" + i)
                    .altText(product.getName() + " - Primary")
                    .isPrimary(true)
                    .displayOrder(0)
                    .build();
            product.getImages().add(primaryImage);

            for (int j = 1; j <= 3; j++) {
                ProductImage image = ProductImage.builder()
                        .product(product)
                        .imageUrl("https://via.placeholder.com/500x600?text=" + category.getName().replace(" ", "+") + "+" + i + "-" + j)
                        .altText(product.getName() + " - View " + j)
                        .isPrimary(false)
                        .displayOrder(j)
                        .build();
                product.getImages().add(image);
            }

            // Add variants
            for (int c = 0; c < colors.length; c++) {
                for (String size : sizes) {
                    ProductVariant variant = ProductVariant.builder()
                            .product(product)
                            .color(colors[c])
                            .colorCode(colorCodes[c])
                            .size(size)
                            .sku(product.getSlug() + "-" + colors[c].toLowerCase() + "-" + size.toLowerCase())
                            .stockQuantity(10 + (int)(Math.random() * 40))
                            .isAvailable(true)
                            .build();
                    product.getVariants().add(variant);
                }
            }

            productRepository.save(product);
        }
    }
}
