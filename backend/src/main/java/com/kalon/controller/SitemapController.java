package com.kalon.controller;

import com.kalon.dto.SiteConfigDTO;
import com.kalon.entity.Category;
import com.kalon.entity.Product;
import com.kalon.repository.CategoryRepository;
import com.kalon.repository.ProductRepository;
import com.kalon.service.SiteConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class SitemapController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SiteConfigService siteConfigService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @GetMapping(value = {"/sitemap.xml", "/api/sitemap.xml"}, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getSitemap() {
        // Get base URL from site config
        SiteConfigDTO baseUrlConfig = siteConfigService.getConfigByKey("site_base_url");
        String baseUrl = (baseUrlConfig != null && baseUrlConfig.getConfigValue() != null)
                ? baseUrlConfig.getConfigValue()
                : "https://kalon.com";

        // Remove trailing slash if present
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // Static pages
        addStaticPage(xml, baseUrl, "/", "1.0", "daily");
        addStaticPage(xml, baseUrl, "/products", "0.8", "daily");
        addStaticPage(xml, baseUrl, "/new-arrivals", "0.8", "daily");
        addStaticPage(xml, baseUrl, "/best-sellers", "0.8", "daily");
        addStaticPage(xml, baseUrl, "/featured", "0.8", "weekly");
        addStaticPage(xml, baseUrl, "/men", "0.7", "weekly");
        addStaticPage(xml, baseUrl, "/women", "0.7", "weekly");
        addStaticPage(xml, baseUrl, "/sneakers", "0.7", "weekly");
        addStaticPage(xml, baseUrl, "/about", "0.5", "monthly");
        addStaticPage(xml, baseUrl, "/contact", "0.5", "monthly");
        addStaticPage(xml, baseUrl, "/faq", "0.5", "monthly");
        addStaticPage(xml, baseUrl, "/terms", "0.3", "yearly");
        addStaticPage(xml, baseUrl, "/privacy", "0.3", "yearly");
        addStaticPage(xml, baseUrl, "/blog", "0.6", "weekly");
        addStaticPage(xml, baseUrl, "/careers", "0.4", "monthly");
        addStaticPage(xml, baseUrl, "/returns", "0.4", "monthly");

        // Category pages
        List<Category> categories = categoryRepository.findAllActiveOrderByDisplayOrder();
        for (Category category : categories) {
            xml.append("  <url>\n");
            xml.append("    <loc>").append(baseUrl).append("/category/").append(category.getSlug()).append("</loc>\n");
            if (category.getUpdatedAt() != null) {
                xml.append("    <lastmod>").append(category.getUpdatedAt().format(DATE_FORMATTER)).append("</lastmod>\n");
            }
            xml.append("    <changefreq>weekly</changefreq>\n");
            xml.append("    <priority>0.7</priority>\n");
            xml.append("  </url>\n");
        }

        // Product pages
        List<Product> products = productRepository.findByIsActiveTrue();
        for (Product product : products) {
            xml.append("  <url>\n");
            xml.append("    <loc>").append(baseUrl).append("/product/").append(product.getSlug()).append("</loc>\n");
            if (product.getUpdatedAt() != null) {
                xml.append("    <lastmod>").append(product.getUpdatedAt().format(DATE_FORMATTER)).append("</lastmod>\n");
            }
            xml.append("    <changefreq>weekly</changefreq>\n");
            xml.append("    <priority>0.8</priority>\n");
            xml.append("  </url>\n");
        }

        xml.append("</urlset>");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(xml.toString());
    }

    @GetMapping(value = {"/robots.txt", "/api/robots.txt"}, produces = "text/plain")
    public ResponseEntity<String> getRobotsTxt() {
        // Get base URL from site config
        SiteConfigDTO baseUrlConfig = siteConfigService.getConfigByKey("site_base_url");
        String baseUrl = (baseUrlConfig != null && baseUrlConfig.getConfigValue() != null)
                ? baseUrlConfig.getConfigValue()
                : "https://kalon.com";

        // Remove trailing slash if present
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        // Get robots.txt content from site config
        SiteConfigDTO robotsConfig = siteConfigService.getConfigByKey("robots_txt");
        String robotsTxt;

        if (robotsConfig != null && robotsConfig.getConfigValue() != null) {
            // Replace {base_url} placeholder with actual base URL
            robotsTxt = robotsConfig.getConfigValue().replace("{base_url}", baseUrl);
        } else {
            // Default robots.txt if not configured
            robotsTxt = "User-agent: *\n" +
                       "Allow: /\n" +
                       "Disallow: /api/\n" +
                       "Disallow: /admin/\n\n" +
                       "Sitemap: " + baseUrl + "/sitemap.xml";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(robotsTxt);
    }

    private void addStaticPage(StringBuilder xml, String baseUrl, String path, String priority, String changefreq) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(baseUrl).append(path).append("</loc>\n");
        xml.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        xml.append("    <priority>").append(priority).append("</priority>\n");
        xml.append("  </url>\n");
    }
}
