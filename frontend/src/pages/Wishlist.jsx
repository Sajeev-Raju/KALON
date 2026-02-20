import { useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { FiHeart } from 'react-icons/fi';
import ProductCard from '../components/common/ProductCard';
import { fetchWishlist } from '../redux/slices/wishlistSlice';
import './Wishlist.css';

const Wishlist = () => {
  const dispatch = useDispatch();
  const { items: serverItems, loading, error } = useSelector((state) => state.wishlist);
  const { items: guestItems } = useSelector((state) => state.guestWishlist);
  const { isAuthenticated } = useSelector((state) => state.auth);

  const items = isAuthenticated ? serverItems : guestItems;

  useEffect(() => {
    if (isAuthenticated) {
      dispatch(fetchWishlist());
    }
  }, [isAuthenticated, dispatch]);

  if (isAuthenticated && loading) {
    return (
      <div className="wishlist-page">
        <div className="container">
          <div className="loading">Loading...</div>
        </div>
      </div>
    );
  }

  if (isAuthenticated && error) {
    return (
      <div className="wishlist-page">
        <div className="container">
          <div className="empty-wishlist">
            <h2>Something went wrong</h2>
            <p>{error}</p>
            <button onClick={() => dispatch(fetchWishlist())} className="btn btn-primary">Try Again</button>
          </div>
        </div>
      </div>
    );
  }

  if (items.length === 0) {
    return (
      <div className="wishlist-page">
        <div className="container">
          <div className="empty-wishlist">
            <FiHeart className="empty-icon" />
            <h2>Your wishlist is empty</h2>
            <p>Save items you love by clicking the heart icon</p>
            <Link to="/" className="btn btn-primary">Start Shopping</Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="wishlist-page">
      <div className="container">
        <div className="wishlist-header">
          <h1>My Wishlist</h1>
          <p>{items.length} {items.length === 1 ? 'item' : 'items'}</p>
        </div>

        <div className="wishlist-grid">
          {items.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      </div>
    </div>
  );
};

export default Wishlist;
