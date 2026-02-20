-- =====================================================
-- V5: Stock Management - movements, alerts, reorder points
-- =====================================================

-- 1. Stock movement audit trail
CREATE TABLE stock_movements (
    id              BIGSERIAL PRIMARY KEY,
    variant_id      BIGINT NOT NULL REFERENCES product_variants(id) ON DELETE CASCADE,
    product_id      BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    movement_type   VARCHAR(50) NOT NULL,
    quantity_change  INTEGER NOT NULL,
    quantity_before  INTEGER NOT NULL,
    quantity_after   INTEGER NOT NULL,
    reference_type  VARCHAR(50),
    reference_id    BIGINT,
    notes           TEXT,
    created_by      BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sm_variant_id ON stock_movements(variant_id);
CREATE INDEX idx_sm_product_id ON stock_movements(product_id);
CREATE INDEX idx_sm_movement_type ON stock_movements(movement_type);
CREATE INDEX idx_sm_reference ON stock_movements(reference_type, reference_id);
CREATE INDEX idx_sm_created_at ON stock_movements(created_at);

-- 2. Low stock alert deduplication tracking
CREATE TABLE low_stock_alerts_sent (
    id          BIGSERIAL PRIMARY KEY,
    variant_id  BIGINT NOT NULL UNIQUE REFERENCES product_variants(id) ON DELETE CASCADE,
    alerted_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 3. Add reorder_point to product_variants
ALTER TABLE product_variants ADD COLUMN reorder_point INTEGER;

-- 4. Seed default SiteConfig entries for stock management
INSERT INTO site_config (config_key, config_value, config_type, description, is_active, display_order, created_at, updated_at)
VALUES
    ('low_stock_threshold', '10', 'NUMBER', 'Stock quantity threshold below which a variant is considered low stock', true, 100, NOW(), NOW()),
    ('low_stock_alert_email', '', 'TEXT', 'Admin email address to receive low stock alert notifications', true, 101, NOW(), NOW())
ON CONFLICT (config_key) DO NOTHING;
