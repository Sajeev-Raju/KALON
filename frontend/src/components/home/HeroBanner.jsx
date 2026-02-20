import { Link } from 'react-router-dom';
import './HeroBanner.css';

const HeroBanner = () => {
  return (
    <section className="hero-banner">
      <div className="hero-content">
        <div className="hero-left">
          <Link to="/men" className="hero-category">
            <div className="hero-image-container">
              <img
                src="https://images.unsplash.com/photo-1617137968427-85924c800a22?w=600"
                alt="Men's Collection"
              />
              <div className="hero-overlay">
                <h2>MEN</h2>
                <span className="shop-now">Shop Now →</span>
              </div>
            </div>
          </Link>
        </div>
        <div className="hero-right">
          <Link to="/women" className="hero-category">
            <div className="hero-image-container">
              <img
                src="https://images.unsplash.com/photo-1483985988355-763728e1935b?w=600"
                alt="Women's Collection"
              />
              <div className="hero-overlay">
                <h2>WOMEN</h2>
                <span className="shop-now">Shop Now →</span>
              </div>
            </div>
          </Link>
        </div>
      </div>
    </section>
  );
};

export default HeroBanner;
