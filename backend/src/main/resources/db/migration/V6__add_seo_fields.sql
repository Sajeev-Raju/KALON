-- =====================================================
-- V6: SEO Fields - meta tags, sitemap config, global defaults
-- =====================================================

-- 1. SEO columns on products
ALTER TABLE products ADD COLUMN meta_title VARCHAR(255);
ALTER TABLE products ADD COLUMN meta_description VARCHAR(500);
ALTER TABLE products ADD COLUMN meta_keywords VARCHAR(500);
ALTER TABLE products ADD COLUMN og_image VARCHAR(500);

-- 2. SEO columns on categories
ALTER TABLE categories ADD COLUMN meta_title VARCHAR(255);
ALTER TABLE categories ADD COLUMN meta_description VARCHAR(500);
ALTER TABLE categories ADD COLUMN meta_keywords VARCHAR(500);
ALTER TABLE categories ADD COLUMN og_image VARCHAR(500);

-- 3. Global SEO defaults in SiteConfig
INSERT INTO site_config (config_key, config_value, config_type, description, is_active, display_order, created_at, updated_at)
VALUES
    ('site_meta_title', 'KALON - Fashion E-Commerce', 'TEXT', 'Default meta title for the site', true, 200, NOW(), NOW()),
    ('site_meta_description', 'Discover the latest fashion trends at KALON.', 'TEXT', 'Default meta description for the site', true, 201, NOW(), NOW()),
    ('default_og_image', '', 'TEXT', 'Default Open Graph image URL for social sharing', true, 202, NOW(), NOW()),
    ('google_analytics_id', '', 'TEXT', 'Google Analytics measurement ID (e.g. G-XXXXXXXXXX)', true, 203, NOW(), NOW()),
    ('site_base_url', 'http://localhost:5173', 'TEXT', 'Frontend base URL for sitemap and canonical URLs', true, 204, NOW(), NOW()),
    ('robots_txt', E'User-agent: *\nAllow: /\nDisallow: /api/\nDisallow: /admin/\nSitemap: {base_url}/sitemap.xml', 'TEXT', 'Contents of robots.txt. Use {base_url} as placeholder.', true, 205, NOW(), NOW())
ON CONFLICT (config_key) DO NOTHING;
