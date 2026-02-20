import HeroBanner from '../components/home/HeroBanner';
import CategorySection from '../components/home/CategorySection';
import ProductSection from '../components/home/ProductSection';
import SEO, { BASE_URL } from '../components/common/SEO';
import './Home.css';

const Home = () => {
  const organizationSchema = {
    '@context': 'https://schema.org',
    '@type': 'Organization',
    name: 'KALON',
    url: BASE_URL,
    logo: `${BASE_URL}/logo.png`,
    description: 'Discover the latest fashion trends at KALON. Shop men\'s and women\'s clothing, sneakers, and accessories.',
    contactPoint: {
      '@type': 'ContactPoint',
      contactType: 'customer service',
      availableLanguage: 'English',
    },
  };

  const webSiteSchema = {
    '@context': 'https://schema.org',
    '@type': 'WebSite',
    name: 'KALON',
    url: BASE_URL,
    potentialAction: {
      '@type': 'SearchAction',
      target: `${BASE_URL}/search?q={search_term_string}`,
      'query-input': 'required name=search_term_string',
    },
  };

  return (
    <div className="home-page">
      <SEO
        title={null}
        description="Discover the latest fashion trends at KALON. Shop men's and women's clothing, sneakers, and accessories with free shipping on orders above ₹999."
        canonicalUrl={BASE_URL}
        ogType="website"
        jsonLd={[organizationSchema, webSiteSchema]}
      />
      <HeroBanner />
      <CategorySection />
      <ProductSection
        title="New Arrivals"
        type="new-arrivals"
        viewAllLink="/new-arrivals"
      />
      <ProductSection
        title="Best Sellers"
        type="best-sellers"
        viewAllLink="/best-sellers"
      />
      <ProductSection
        title="Featured Collection"
        type="featured"
        viewAllLink="/featured"
      />

      {/* Feature Highlights */}
      <section className="features-section">
        <div className="container">
          <div className="features-grid">
            <div className="feature-item">
              <div className="feature-icon">🚚</div>
              <h3>Free Shipping</h3>
              <p>On orders above ₹999</p>
            </div>
            <div className="feature-item">
              <div className="feature-icon">↩️</div>
              <h3>Easy Returns</h3>
              <p>30 days return policy</p>
            </div>
            <div className="feature-item">
              <div className="feature-icon">💳</div>
              <h3>Secure Payment</h3>
              <p>100% secure checkout</p>
            </div>
            <div className="feature-item">
              <div className="feature-icon">🎧</div>
              <h3>24/7 Support</h3>
              <p>Dedicated customer service</p>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
};

export default Home;
