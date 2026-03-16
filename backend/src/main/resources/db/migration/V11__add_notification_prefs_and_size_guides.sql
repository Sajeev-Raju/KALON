-- Notification preferences table
CREATE TABLE IF NOT EXISTS notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    order_updates BOOLEAN DEFAULT true,
    promotional_emails BOOLEAN DEFAULT false,
    stock_alerts BOOLEAN DEFAULT true,
    review_reminders BOOLEAN DEFAULT true,
    abandoned_cart_reminders BOOLEAN DEFAULT true,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notification_prefs_user_id ON notification_preferences(user_id);

-- Size guides table
CREATE TABLE IF NOT EXISTS size_guides (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category_id BIGINT REFERENCES categories(id),
    gender_type VARCHAR(20),
    measurements TEXT,
    size_chart_image VARCHAR(500),
    fit_notes TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_size_guides_category_id ON size_guides(category_id);
CREATE INDEX IF NOT EXISTS idx_size_guides_gender_type ON size_guides(gender_type);
