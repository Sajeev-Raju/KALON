import { Link } from 'react-router-dom';
import { FiFacebook, FiInstagram, FiTwitter, FiYoutube } from 'react-icons/fi';
import logo from '../../assets/images/logo.png';
import './Footer.css';

const Footer = () => {
  return (
    <footer className="footer">
      {/* Brand Banner */}
      <div className="brand-banner">
        <div className="container">
          <p className="brand-tagline">PREMIUM FASHION BRAND</p>
          <p className="brand-stat">Over <span>1 Million</span> Happy Customers</p>
        </div>
      </div>

      {/* Main Footer */}
      <div className="footer-main">
        <div className="container">
          <div className="footer-grid">
            {/* Need Help */}
            <div className="footer-column">
              <h4>Need Help</h4>
              <ul>
                <li><Link to="/contact">Contact Us</Link></li>
                <li><Link to="/track-order">Track Order</Link></li>
                <li><Link to="/returns">Returns & Refunds</Link></li>
                <li><Link to="/faq">FAQs</Link></li>
                <li><Link to="/account">My Account</Link></li>
              </ul>
            </div>

            {/* Company */}
            <div className="footer-column">
              <h4>Company</h4>
              <ul>
                <li><Link to="/about">About Us</Link></li>
                <li><Link to="/careers">Careers</Link></li>
                <li><Link to="/blog">Blog</Link></li>
              </ul>
            </div>

            {/* More Info */}
            <div className="footer-column">
              <h4>More Info</h4>
              <ul>
                <li><Link to="/terms">Terms & Conditions</Link></li>
                <li><Link to="/privacy">Privacy Policy</Link></li>
                <li><Link to="/sitemap">Sitemap</Link></li>
              </ul>
            </div>

            {/* Newsletter */}
            <div className="footer-column newsletter-column">
              <h4>Stay Connected</h4>
              <p>Subscribe to get special offers and updates</p>
              <form className="newsletter-form">
                <input type="email" placeholder="Enter your email" />
                <button type="submit" className="btn btn-secondary">Subscribe</button>
              </form>
              <div className="social-links">
                <a href="https://facebook.com" target="_blank" rel="noopener noreferrer">
                  <FiFacebook />
                </a>
                <a href="https://instagram.com" target="_blank" rel="noopener noreferrer">
                  <FiInstagram />
                </a>
                <a href="https://twitter.com" target="_blank" rel="noopener noreferrer">
                  <FiTwitter />
                </a>
                <a href="https://youtube.com" target="_blank" rel="noopener noreferrer">
                  <FiYoutube />
                </a>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Footer Bottom */}
      <div className="footer-bottom">
        <div className="container">
          <div className="footer-bottom-content">
            <div className="footer-logo">
              <img src={logo} alt="Kalon" />
            </div>
            <p className="copyright">
              © {new Date().getFullYear()} Kalon. All Rights Reserved.
            </p>
            <div className="payment-methods">
              <span>Secure Payments</span>
              <div className="payment-icons">
                <span>💳</span>
                <span>🏦</span>
                <span>📱</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
