import { Link } from 'react-router-dom';
import './NotFound.css';

const NotFound = () => {
  return (
    <div className="not-found-page">
      <div className="not-found-content">
        <h1 className="not-found-code">404</h1>
        <h2>Page Not Found</h2>
        <p>The page you're looking for doesn't exist or has been moved.</p>
        <div className="not-found-actions">
          <Link to="/" className="not-found-btn">Back to Home</Link>
          <Link to="/products" className="not-found-btn secondary">Browse Products</Link>
        </div>
      </div>
    </div>
  );
};

export default NotFound;
