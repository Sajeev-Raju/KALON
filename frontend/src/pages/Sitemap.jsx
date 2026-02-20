import { Link } from 'react-router-dom';
import './Sitemap.css';

const Sitemap = () => {
  return (
    <div className="sitemap-page">
      <div className="container">
        <div className="sitemap-header">
          <h1>Sitemap</h1>
          <p>Find everything you're looking for</p>
        </div>

        <div className="sitemap-content">
          <div className="sitemap-section">
            <h2>Shop</h2>
            <ul>
              <li><Link to="/men">Men</Link></li>
              <li><Link to="/women">Women</Link></li>
              <li><Link to="/sneakers">Sneakers</Link></li>
              <li><Link to="/new-arrivals">New Arrivals</Link></li>
              <li><Link to="/best-sellers">Best Sellers</Link></li>
              <li><Link to="/featured">Featured</Link></li>
              <li><Link to="/products">All Products</Link></li>
            </ul>
          </div>

          <div className="sitemap-section">
            <h2>Customer Service</h2>
            <ul>
              <li><Link to="/contact">Contact Us</Link></li>
              <li><Link to="/track-order">Track Order</Link></li>
              <li><Link to="/returns">Returns & Refunds</Link></li>
              <li><Link to="/faq">FAQs</Link></li>
              <li><Link to="/account">My Account</Link></li>
            </ul>
          </div>

          <div className="sitemap-section">
            <h2>Company</h2>
            <ul>
              <li><Link to="/about">About Us</Link></li>
              <li><Link to="/careers">Careers</Link></li>
              <li><Link to="/blog">Blog</Link></li>
            </ul>
          </div>

          <div className="sitemap-section">
            <h2>Legal</h2>
            <ul>
              <li><Link to="/terms">Terms & Conditions</Link></li>
              <li><Link to="/privacy">Privacy Policy</Link></li>
            </ul>
          </div>

          <div className="sitemap-section">
            <h2>Account</h2>
            <ul>
              <li><Link to="/login">Login</Link></li>
              <li><Link to="/register">Register</Link></li>
              <li><Link to="/account">My Account</Link></li>
              <li><Link to="/wishlist">Wishlist</Link></li>
              <li><Link to="/cart">Shopping Cart</Link></li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Sitemap;



