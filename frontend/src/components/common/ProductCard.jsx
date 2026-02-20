import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { FiHeart } from 'react-icons/fi';
import { toggleWishlist } from '../../redux/slices/wishlistSlice';
import { toggleGuestWishlistItem } from '../../redux/slices/guestWishlistSlice';
import StarRating from './StarRating';
import toast from 'react-hot-toast';
import './ProductCard.css';

const ProductCard = ({ product }) => {
  const [isHovered, setIsHovered] = useState(false);
  const dispatch = useDispatch();
  const { isAuthenticated } = useSelector((state) => state.auth);
  const { items: wishlistItems } = useSelector((state) => state.wishlist);
  const { items: guestWishlistItems } = useSelector((state) => state.guestWishlist);
  const activeWishlist = isAuthenticated ? wishlistItems : guestWishlistItems;

  const isInWishlist = activeWishlist.some(item => item.id === product.id);

  const handleWishlistToggle = (e) => {
    e.preventDefault();
    e.stopPropagation();

    if (isAuthenticated) {
      dispatch(toggleWishlist({ productId: product.id, product }))
        .unwrap()
        .then(() => {
          toast.success(isInWishlist ? 'Removed from wishlist' : 'Added to wishlist');
        })
        .catch((error) => {
          toast.error(typeof error === 'object' ? error.message : error);
        });
    } else {
      dispatch(toggleGuestWishlistItem({
        id: product.id,
        name: product.name,
        slug: product.slug,
        price: product.price,
        originalPrice: product.originalPrice,
        discountPercentage: product.discountPercentage,
        images: product.images,
        averageRating: product.averageRating,
        reviewCount: product.reviewCount,
        brandName: product.brandName,
        availableColors: product.availableColors,
        isNewArrival: product.isNewArrival,
        isBestSeller: product.isBestSeller,
      }));
      toast.success(isInWishlist ? 'Removed from wishlist' : 'Added to wishlist');
    }
  };

  const primaryImage = product.images?.find(img => img.isPrimary)?.imageUrl ||
    product.images?.[0]?.imageUrl ||
    'https://via.placeholder.com/300x400?text=No+Image';

  const secondaryImage = product.images?.find(img => !img.isPrimary)?.imageUrl || primaryImage;

  const discountPercentage = product.originalPrice && product.price < product.originalPrice
    ? Math.round(((product.originalPrice - product.price) / product.originalPrice) * 100)
    : product.discountPercentage;

  return (
    <Link
      to={`/product/${product.slug}`}
      className="product-card"
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
    >
      <div className="product-image-wrapper">
        <img
          src={isHovered ? secondaryImage : primaryImage}
          alt={product.name}
          className="product-image"
        />

        {/* Badges */}
        <div className="product-badges">
          {product.isNewArrival && <span className="badge new">NEW</span>}
          {product.isBestSeller && <span className="badge bestseller">BESTSELLER</span>}
          {discountPercentage > 0 && (
            <span className="badge discount">-{discountPercentage}%</span>
          )}
        </div>

        {/* Wishlist Button */}
        <button
          className={`wishlist-btn ${isInWishlist ? 'active' : ''}`}
          onClick={handleWishlistToggle}
        >
          <FiHeart />
        </button>

        {/* Quick View Overlay */}
        <div className={`quick-view-overlay ${isHovered ? 'visible' : ''}`}>
          <span>Quick View</span>
        </div>
      </div>

      <div className="product-info">
        <p className="product-brand">{product.brandName || 'Kalon'}</p>
        <h3 className="product-name">{product.name}</h3>

        <div className="product-rating">
          {product.averageRating > 0 && (
            <StarRating
              rating={product.averageRating}
              size="sm"
              count={product.reviewCount}
            />
          )}
        </div>

        <div className="product-price">
          <span className="current-price">₹{product.price}</span>
          {product.originalPrice && product.originalPrice > product.price && (
            <>
              <span className="original-price">₹{product.originalPrice}</span>
              <span className="discount-text">{discountPercentage}% OFF</span>
            </>
          )}
        </div>

        {/* Available Colors */}
        {product.availableColors && product.availableColors.length > 0 && (
          <div className="product-colors">
            {product.availableColors.slice(0, 5).map((color, index) => (
              <span
                key={index}
                className="color-dot"
                style={{ backgroundColor: color.toLowerCase() }}
                title={color}
              />
            ))}
            {product.availableColors.length > 5 && (
              <span className="more-colors">+{product.availableColors.length - 5}</span>
            )}
          </div>
        )}
      </div>
    </Link>
  );
};

export default ProductCard;
