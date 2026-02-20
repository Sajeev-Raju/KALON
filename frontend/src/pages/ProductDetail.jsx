import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { FiHeart, FiShare2, FiTruck, FiRefreshCw, FiShield, FiStar, FiCheck, FiEdit2, FiTrash2 } from 'react-icons/fi';
import { productAPI, reviewAPI } from '../services/api';
import { addToCart } from '../redux/slices/cartSlice';
import { toggleWishlist } from '../redux/slices/wishlistSlice';
import { addGuestCartItem } from '../redux/slices/guestCartSlice';
import { toggleGuestWishlistItem } from '../redux/slices/guestWishlistSlice';
import StarRating from '../components/common/StarRating';
import SEO, { BASE_URL, toAbsoluteUrl } from '../components/common/SEO';
import toast from 'react-hot-toast';
import './ProductDetail.css';

const ProductDetail = () => {
  const { slug } = useParams();
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const { isAuthenticated } = useSelector((state) => state.auth);
  const { items: wishlistItems } = useSelector((state) => state.wishlist);
  const { items: guestWishlistItems } = useSelector((state) => state.guestWishlist);
  const activeWishlist = isAuthenticated ? wishlistItems : guestWishlistItems;

  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);
  const [selectedImage, setSelectedImage] = useState(0);
  const [selectedSize, setSelectedSize] = useState('');
  const [selectedColor, setSelectedColor] = useState('');
  const [quantity, setQuantity] = useState(1);

  // Reviews state
  const [reviews, setReviews] = useState([]);
  const [reviewsPage, setReviewsPage] = useState(0);
  const [reviewsTotalPages, setReviewsTotalPages] = useState(0);
  const [reviewSort, setReviewSort] = useState('newest');
  const [reviewsLoading, setReviewsLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [showReviewForm, setShowReviewForm] = useState(false);
  const [editingReview, setEditingReview] = useState(null);
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewComment, setReviewComment] = useState('');
  const [hoverRating, setHoverRating] = useState(0);
  const [submittingReview, setSubmittingReview] = useState(false);
  const [userReview, setUserReview] = useState(null);
  const [deletingReviewId, setDeletingReviewId] = useState(null);

  const isInWishlist = product && activeWishlist.some(item => item.id === product.id);

  useEffect(() => {
    const fetchProduct = async () => {
      try {
        const response = await productAPI.getBySlug(slug);
        setProduct(response.data.data);
        if (response.data.data.availableColors?.length > 0) {
          setSelectedColor(response.data.data.availableColors[0]);
        }
        // Fetch reviews and check user's existing review
        fetchReviews(response.data.data.id);
        fetchUserReview(response.data.data.id);
      } catch (error) {
        console.error('Error fetching product:', error);
        toast.error('Product not found');
      } finally {
        setLoading(false);
      }
    };

    fetchProduct();
  }, [slug]);

  const fetchReviews = async (productId, page = 0, append = false, sort = reviewSort) => {
    try {
      if (append) {
        setLoadingMore(true);
      } else {
        setReviewsLoading(true);
      }
      const response = await reviewAPI.getProductReviews(productId, { page, size: 10, sort });
      const data = response.data.data;
      if (append) {
        setReviews(prev => [...prev, ...(data?.content || [])]);
      } else {
        setReviews(data?.content || []);
      }
      setReviewsPage(data?.number || 0);
      setReviewsTotalPages(data?.totalPages || 0);
    } catch (error) {
      console.error('Error fetching reviews:', error);
    } finally {
      setReviewsLoading(false);
      setLoadingMore(false);
    }
  };

  const fetchUserReview = async (productId) => {
    if (!isAuthenticated) return;
    try {
      const response = await reviewAPI.getUserReviewForProduct(productId);
      setUserReview(response.data.data || null);
    } catch (error) {
      // Not critical
    }
  };

  const handleLoadMore = () => {
    if (product && reviewsPage < reviewsTotalPages - 1) {
      fetchReviews(product.id, reviewsPage + 1, true, reviewSort);
    }
  };

  const handleSortChange = (newSort) => {
    setReviewSort(newSort);
    if (product) {
      fetchReviews(product.id, 0, false, newSort);
    }
  };

  const handleSubmitReview = async (e) => {
    e.preventDefault();
    if (!isAuthenticated) {
      toast.error('Please login to write a review');
      navigate('/login');
      return;
    }

    try {
      setSubmittingReview(true);
      if (editingReview) {
        await reviewAPI.update(editingReview.id, {
          rating: reviewRating,
          comment: reviewComment,
        });
        toast.success('Review updated successfully');
      } else {
        await reviewAPI.create({
          productId: product.id,
          rating: reviewRating,
          comment: reviewComment,
        });
        toast.success('Review submitted successfully');
      }
      setShowReviewForm(false);
      setEditingReview(null);
      setReviewRating(5);
      setReviewComment('');
      setHoverRating(0);
      fetchReviews(product.id);
      fetchUserReview(product.id);
      // Refresh product to get updated rating
      const response = await productAPI.getBySlug(slug);
      setProduct(response.data.data);
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to submit review');
    } finally {
      setSubmittingReview(false);
    }
  };

  const handleEditReview = (review) => {
    setEditingReview(review);
    setReviewRating(review.rating);
    setReviewComment(review.comment || '');
    setShowReviewForm(true);
  };

  const handleDeleteReview = async (reviewId) => {
    if (!window.confirm('Are you sure you want to delete your review?')) return;
    try {
      setDeletingReviewId(reviewId);
      await reviewAPI.delete(reviewId);
      toast.success('Review deleted');
      fetchReviews(product.id);
      setUserReview(null);
      const response = await productAPI.getBySlug(slug);
      setProduct(response.data.data);
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to delete review');
    } finally {
      setDeletingReviewId(null);
    }
  };

  const handleCancelReviewForm = () => {
    setShowReviewForm(false);
    setEditingReview(null);
    setReviewRating(5);
    setReviewComment('');
    setHoverRating(0);
  };

  const formatReviewDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('en-IN', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    });
  };

  // Compute selected variant and max quantity
  const selectedVariant = product?.variants?.find(
    v => v.size === selectedSize && v.color === selectedColor
  );
  const maxQuantity = selectedVariant?.stockQuantity || 0;

  // Reset quantity when variant changes
  useEffect(() => {
    setQuantity(1);
  }, [selectedSize, selectedColor]);

  const handleAddToCart = () => {
    if (!selectedSize) {
      toast.error('Please select a size');
      return;
    }

    if (!selectedVariant) {
      toast.error('Selected variant not available');
      return;
    }

    if (selectedVariant.stockQuantity < quantity) {
      toast.error(`Only ${selectedVariant.stockQuantity} available in stock`);
      return;
    }

    if (isAuthenticated) {
      dispatch(addToCart({
        productId: product.id,
        variantId: selectedVariant.id,
        quantity,
      }))
        .unwrap()
        .then(() => toast.success('Added to cart'))
        .catch((error) => {
          if (error.stockError) {
            toast.error(`Only ${error.stockError.availableStock} in stock (${error.stockError.currentCartQuantity} already in cart)`);
          } else {
            toast.error(error.message || error);
          }
        });
    } else {
      const primaryImage = product.images?.find(img => img.isPrimary)?.imageUrl ||
        product.images?.[0]?.imageUrl || '';
      dispatch(addGuestCartItem({
        productId: product.id,
        variantId: selectedVariant.id,
        quantity,
        productName: product.name,
        productImage: primaryImage,
        size: selectedSize,
        color: selectedColor,
        price: product.price,
        stockQuantity: selectedVariant.stockQuantity,
      }));
      toast.success('Added to cart');
    }
  };

  const handleWishlistToggle = () => {
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

  if (loading) {
    return (
      <div className="product-detail-page loading">
        <div className="container">
          <div className="loading-skeleton">Loading...</div>
        </div>
      </div>
    );
  }

  if (!product) {
    return (
      <div className="product-detail-page not-found">
        <div className="container">
          <h2>Product not found</h2>
        </div>
      </div>
    );
  }

  const primaryImage = product.images?.find(img => img.isPrimary)?.imageUrl ||
    product.images?.[0]?.imageUrl ||
    'https://via.placeholder.com/600x800?text=No+Image';

  const productSchema = {
    '@context': 'https://schema.org',
    '@type': 'Product',
    name: product.name,
    description: product.description,
    image: product.images?.map(img => toAbsoluteUrl(img.imageUrl)) || [],
    brand: {
      '@type': 'Brand',
      name: product.brandName || 'KALON',
    },
    sku: product.slug,
    offers: {
      '@type': 'Offer',
      url: `${BASE_URL}/product/${product.slug}`,
      priceCurrency: 'INR',
      price: product.price,
      availability: product.variants?.some(v => v.isAvailable && v.stockQuantity > 0)
        ? 'https://schema.org/InStock'
        : 'https://schema.org/OutOfStock',
      seller: {
        '@type': 'Organization',
        name: 'KALON',
      },
    },
    ...(product.averageRating > 0 && {
      aggregateRating: {
        '@type': 'AggregateRating',
        ratingValue: product.averageRating,
        reviewCount: product.reviewCount,
      },
    }),
  };

  const breadcrumbSchema = {
    '@context': 'https://schema.org',
    '@type': 'BreadcrumbList',
    itemListElement: [
      {
        '@type': 'ListItem',
        position: 1,
        name: 'Home',
        item: BASE_URL,
      },
      ...(product.categoryName ? [{
        '@type': 'ListItem',
        position: 2,
        name: product.categoryName,
        item: `${BASE_URL}/category/${product.categorySlug || ''}`,
      }] : []),
      {
        '@type': 'ListItem',
        position: product.categoryName ? 3 : 2,
        name: product.name,
        item: `${BASE_URL}/product/${product.slug}`,
      },
    ],
  };

  return (
    <div className="product-detail-page">
      <SEO
        title={product.metaTitle || product.name}
        description={product.metaDescription || (product.description ? product.description.slice(0, 160) : '')}
        keywords={product.metaKeywords}
        canonicalUrl={`${BASE_URL}/product/${product.slug}`}
        ogTitle={product.metaTitle || product.name}
        ogDescription={product.metaDescription || (product.description ? product.description.slice(0, 160) : '')}
        ogImage={product.ogImage || primaryImage}
        ogType="product"
        jsonLd={[productSchema, breadcrumbSchema]}
      />
      <div className="container">
        <div className="product-detail-content">
          {/* Image Gallery */}
          <div className="product-gallery">
            <div className="gallery-thumbnails">
              {product.images?.map((image, index) => (
                <button
                  key={image.id}
                  className={`thumbnail ${selectedImage === index ? 'active' : ''}`}
                  onClick={() => setSelectedImage(index)}
                >
                  <img src={image.imageUrl} alt={`View ${index + 1}`} />
                </button>
              ))}
            </div>
            <div className="gallery-main">
              <img
                src={product.images?.[selectedImage]?.imageUrl || primaryImage}
                alt={product.name}
              />
            </div>
          </div>

          {/* Product Info */}
          <div className="product-info">
            <div className="product-header">
              <p className="product-brand">{product.brandName || 'Kalon'}</p>
              <h1 className="product-title">{product.name}</h1>

              {product.averageRating > 0 && (
                <div className="product-rating">
                  <StarRating
                    rating={product.averageRating}
                    size="lg"
                    showValue
                    count={product.reviewCount}
                  />
                </div>
              )}
            </div>

            <div className="product-price">
              <span className="current-price">₹{product.price}</span>
              {product.originalPrice && product.originalPrice > product.price && (
                <>
                  <span className="original-price">₹{product.originalPrice}</span>
                  <span className="discount-badge">
                    {product.discountPercentage}% OFF
                  </span>
                </>
              )}
            </div>

            <p className="tax-info">Inclusive of all taxes</p>

            {/* Color Selection */}
            {product.availableColors?.length > 0 && (
              <div className="option-group">
                <label>
                  Color: <strong>{selectedColor}</strong>
                </label>
                <div className="color-options">
                  {product.availableColors.map((color) => (
                    <button
                      key={color}
                      className={`color-btn ${selectedColor === color ? 'active' : ''}`}
                      style={{ backgroundColor: color.toLowerCase() }}
                      onClick={() => setSelectedColor(color)}
                      title={color}
                    />
                  ))}
                </div>
              </div>
            )}

            {/* Size Selection */}
            {product.availableSizes?.length > 0 && (
              <div className="option-group">
                <label>
                  Select Size: <a href="#size-guide">Size Guide</a>
                </label>
                <div className="size-options">
                  {product.availableSizes.map((size) => {
                    const variant = product.variants?.find(
                      v => v.size === size && v.color === selectedColor
                    );
                    const isAvailable = variant?.isAvailable && variant?.stockQuantity > 0;

                    return (
                      <button
                        key={size}
                        className={`size-btn ${selectedSize === size ? 'active' : ''} ${!isAvailable ? 'disabled' : ''}`}
                        onClick={() => isAvailable && setSelectedSize(size)}
                        disabled={!isAvailable}
                      >
                        {size}
                      </button>
                    );
                  })}
                </div>
              </div>
            )}

            {/* Quantity */}
            {selectedSize && selectedVariant && maxQuantity > 0 && (
              <div className="option-group">
                <label>Quantity:</label>
                <div className="quantity-selector">
                  <button
                    onClick={() => setQuantity(Math.max(1, quantity - 1))}
                    disabled={quantity <= 1}
                  >-</button>
                  <span>{quantity}</span>
                  <button
                    onClick={() => setQuantity(Math.min(maxQuantity, quantity + 1))}
                    disabled={quantity >= maxQuantity}
                  >+</button>
                </div>
                {maxQuantity <= 5 && (
                  <span className="stock-hint low-stock">Only {maxQuantity} left in stock</span>
                )}
              </div>
            )}

            {/* Stock Info */}
            {selectedSize && selectedVariant && maxQuantity === 0 && (
              <div className="option-group">
                <span className="stock-hint out-of-stock">Out of Stock</span>
              </div>
            )}

            {/* Actions */}
            <div className="product-actions">
              <button
                className="btn btn-primary btn-lg"
                onClick={handleAddToCart}
                disabled={!selectedSize || !selectedVariant || maxQuantity === 0}
              >
                {!selectedSize ? 'Select a Size' : maxQuantity === 0 ? 'Out of Stock' : 'Add to Cart'}
              </button>
              <button
                className={`btn btn-outline wishlist-btn ${isInWishlist ? 'active' : ''}`}
                onClick={handleWishlistToggle}
              >
                <FiHeart /> {isInWishlist ? 'Wishlisted' : 'Wishlist'}
              </button>
            </div>

            {/* Features */}
            <div className="product-features">
              <div className="feature">
                <FiTruck />
                <span>Free shipping on orders above ₹999</span>
              </div>
              <div className="feature">
                <FiRefreshCw />
                <span>Easy 30 days return & exchange</span>
              </div>
              <div className="feature">
                <FiShield />
                <span>100% Authentic Products</span>
              </div>
            </div>

            {/* Product Details */}
            <div className="product-details">
              <h3>Product Details</h3>
              <p>{product.description}</p>

              <ul className="details-list">
                {product.material && <li><strong>Material:</strong> {product.material}</li>}
                {product.countryOfOrigin && <li><strong>Country of Origin:</strong> {product.countryOfOrigin}</li>}
                {product.careInstructions && <li><strong>Care:</strong> {product.careInstructions}</li>}
              </ul>
            </div>
          </div>
        </div>

        {/* Reviews Section */}
        <div className="reviews-section">
          <div className="reviews-header">
            <h2>Customer Reviews</h2>
            {product.averageRating > 0 && (
              <div className="rating-summary">
                <span className="rating-score">{product.averageRating.toFixed(1)}</span>
                <StarRating rating={product.averageRating} size="lg" />
                <span className="rating-count">Based on {product.reviewCount} review{product.reviewCount !== 1 ? 's' : ''}</span>
              </div>
            )}
            <div className="reviews-header-actions">
              {reviews.length > 1 && (
                <select
                  className="review-sort-select"
                  value={reviewSort}
                  onChange={(e) => handleSortChange(e.target.value)}
                >
                  <option value="newest">Newest First</option>
                  <option value="oldest">Oldest First</option>
                  <option value="highest">Highest Rated</option>
                  <option value="lowest">Lowest Rated</option>
                </select>
              )}
              {isAuthenticated && !userReview && (
                <button
                  className="btn btn-outline write-review-btn"
                  onClick={() => setShowReviewForm(!showReviewForm)}
                >
                  <FiStar /> Write a Review
                </button>
              )}
              {isAuthenticated && userReview && !showReviewForm && (
                <button
                  className="btn btn-outline write-review-btn"
                  onClick={() => handleEditReview(userReview)}
                >
                  <FiEdit2 /> Edit Your Review
                </button>
              )}
            </div>
          </div>

          {/* Review Form */}
          {showReviewForm && (
            <div className="review-form-container">
              <h3>{editingReview ? 'Edit Your Review' : 'Write Your Review'}</h3>
              <form onSubmit={handleSubmitReview}>
                <div className="rating-input">
                  <label>Your Rating:</label>
                  <div className="star-rating">
                    {[1, 2, 3, 4, 5].map((star) => (
                      <button
                        key={star}
                        type="button"
                        className={`star-btn ${star <= (hoverRating || reviewRating) ? 'active' : ''}`}
                        onClick={() => setReviewRating(star)}
                        onMouseEnter={() => setHoverRating(star)}
                        onMouseLeave={() => setHoverRating(0)}
                      >
                        ★
                      </button>
                    ))}
                    <span className="rating-label">
                      {(hoverRating || reviewRating) === 1 && 'Poor'}
                      {(hoverRating || reviewRating) === 2 && 'Fair'}
                      {(hoverRating || reviewRating) === 3 && 'Good'}
                      {(hoverRating || reviewRating) === 4 && 'Very Good'}
                      {(hoverRating || reviewRating) === 5 && 'Excellent'}
                    </span>
                  </div>
                </div>
                <div className="form-group">
                  <label>Your Review:</label>
                  <textarea
                    value={reviewComment}
                    onChange={(e) => setReviewComment(e.target.value)}
                    placeholder="Share your experience with this product..."
                    rows="4"
                  />
                </div>
                <div className="form-actions">
                  <button
                    type="button"
                    className="btn btn-outline"
                    onClick={handleCancelReviewForm}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="btn btn-primary"
                    disabled={submittingReview}
                  >
                    {submittingReview
                      ? (editingReview ? 'Updating...' : 'Submitting...')
                      : (editingReview ? 'Update Review' : 'Submit Review')}
                  </button>
                </div>
              </form>
            </div>
          )}

          {/* Reviews List */}
          <div className="reviews-list">
            {reviewsLoading ? (
              <div className="loading">Loading reviews...</div>
            ) : reviews.length === 0 ? (
              <div className="no-reviews">
                <p>No reviews yet. Be the first to review this product!</p>
              </div>
            ) : (
              <>
                {reviews.map((review) => (
                  <div key={review.id} className="review-card">
                    <div className="review-header">
                      <div className="reviewer-info">
                        <span className="reviewer-name">{review.userName}</span>
                        {review.verifiedPurchase && (
                          <span className="verified-badge">
                            <FiCheck /> Verified Purchase
                          </span>
                        )}
                      </div>
                      <div className="review-header-right">
                        <span className="review-date">{formatReviewDate(review.createdAt)}</span>
                        {isAuthenticated && userReview?.id === review.id && (
                          <div className="review-actions">
                            <button
                              className="review-action-btn"
                              onClick={() => handleEditReview(review)}
                              title="Edit review"
                            >
                              <FiEdit2 />
                            </button>
                            <button
                              className="review-action-btn delete"
                              onClick={() => handleDeleteReview(review.id)}
                              disabled={deletingReviewId === review.id}
                              title="Delete review"
                            >
                              <FiTrash2 />
                            </button>
                          </div>
                        )}
                      </div>
                    </div>
                    <div className="review-rating">
                      <StarRating rating={review.rating} size="md" />
                    </div>
                    {review.comment && <p className="review-comment">{review.comment}</p>}
                    {review.updatedAt && review.updatedAt !== review.createdAt && (
                      <span className="review-edited">Edited</span>
                    )}
                  </div>
                ))}
                {reviewsPage < reviewsTotalPages - 1 && (
                  <button
                    className="btn btn-outline load-more-btn"
                    onClick={handleLoadMore}
                    disabled={loadingMore}
                  >
                    {loadingMore ? 'Loading...' : 'Load More Reviews'}
                  </button>
                )}
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default ProductDetail;
