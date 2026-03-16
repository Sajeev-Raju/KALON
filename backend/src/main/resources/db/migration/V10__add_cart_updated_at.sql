-- Add updated_at to cart for abandoned cart tracking
ALTER TABLE carts ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
UPDATE carts SET updated_at = CURRENT_TIMESTAMP WHERE updated_at IS NULL;
