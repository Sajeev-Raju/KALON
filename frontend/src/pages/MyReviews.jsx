import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { reviewAPI } from '../services/api';
import { FiEdit2, FiTrash2, FiArrowLeft, FiStar } from 'react-icons/fi';
import StarRating from '../components/common/StarRating';
import toast from 'react-hot-toast';
import './MyReviews.css';

const MyReviews = () => {
  const navigate = useNavigate();
  const { isAuthenticated } = useSelector((state) => state.auth);
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editingId, setEditingId] = useState(null);
  const [editRating, setEditRating] = useState(5);
  const [editComment, setEditComment] = useState('');
  const [editHover, setEditHover] = useState(0);
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState(null);

  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    fetchReviews();
  }, [isAuthenticated, navigate]);

  const fetchReviews = async () => {
    try {
      setLoading(true);
      const response = await reviewAPI.getUserReviews();
      setReviews(response.data.data || []);
    } catch (error) {
      toast.error('Failed to load reviews');
    } finally {
      setLoading(false);
    }
  };

  const handleEdit = (review) => {
    setEditingId(review.id);
    setEditRating(review.rating);
    setEditComment(review.comment || '');
    setEditHover(0);
  };

  const handleCancelEdit = () => {
    setEditingId(null);
    setEditRating(5);
    setEditComment('');
    setEditHover(0);
  };

  const handleSaveEdit = async (reviewId) => {
    try {
      setSaving(true);
      await reviewAPI.update(reviewId, {
        rating: editRating,
        comment: editComment,
      });
      toast.success('Review updated');
      setEditingId(null);
      fetchReviews();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to update review');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (reviewId) => {
    if (!window.confirm('Are you sure you want to delete this review?')) return;
    try {
      setDeletingId(reviewId);
      await reviewAPI.delete(reviewId);
      toast.success('Review deleted');
      setReviews(prev => prev.filter(r => r.id !== reviewId));
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to delete review');
    } finally {
      setDeletingId(null);
    }
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('en-IN', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    });
  };

  if (loading) {
    return (
      <div className="my-reviews-page">
        <div className="container">
          <div className="loading">Loading your reviews...</div>
        </div>
      </div>
    );
  }

  return (
    <div className="my-reviews-page">
      <div className="container">
        <Link to="/account" className="back-link">
          <FiArrowLeft /> Back to Account
        </Link>

        <h1>My Reviews</h1>

        {reviews.length === 0 ? (
          <div className="empty-reviews">
            <FiStar size={48} />
            <h2>No reviews yet</h2>
            <p>You haven't written any reviews. Purchase a product and share your experience!</p>
            <Link to="/products" className="btn btn-primary">Browse Products</Link>
          </div>
        ) : (
          <div className="reviews-list">
            {reviews.map((review) => (
              <div key={review.id} className="my-review-card">
                <div className="my-review-product">
                  <Link to={`/product/${review.productSlug}`} className="product-link">
                    {review.productName}
                  </Link>
                </div>

                {editingId === review.id ? (
                  <div className="edit-form">
                    <div className="rating-input">
                      <label>Rating:</label>
                      <div className="star-rating">
                        {[1, 2, 3, 4, 5].map((star) => (
                          <button
                            key={star}
                            type="button"
                            className={`star-btn ${star <= (editHover || editRating) ? 'active' : ''}`}
                            onClick={() => setEditRating(star)}
                            onMouseEnter={() => setEditHover(star)}
                            onMouseLeave={() => setEditHover(0)}
                          >
                            ★
                          </button>
                        ))}
                      </div>
                    </div>
                    <textarea
                      value={editComment}
                      onChange={(e) => setEditComment(e.target.value)}
                      placeholder="Your review..."
                      rows="3"
                    />
                    <div className="edit-actions">
                      <button className="btn btn-outline" onClick={handleCancelEdit}>
                        Cancel
                      </button>
                      <button
                        className="btn btn-primary"
                        onClick={() => handleSaveEdit(review.id)}
                        disabled={saving}
                      >
                        {saving ? 'Saving...' : 'Save Changes'}
                      </button>
                    </div>
                  </div>
                ) : (
                  <>
                    <div className="my-review-content">
                      <div className="my-review-rating">
                        <StarRating rating={review.rating} size="md" />
                        <span className="my-review-date">{formatDate(review.createdAt)}</span>
                      </div>
                      {review.comment && <p className="my-review-comment">{review.comment}</p>}
                      {!review.approved && (
                        <span className="review-status-badge pending">Pending Approval</span>
                      )}
                    </div>
                    <div className="my-review-actions">
                      <button
                        className="review-action-btn"
                        onClick={() => handleEdit(review)}
                        title="Edit review"
                      >
                        <FiEdit2 /> Edit
                      </button>
                      <button
                        className="review-action-btn delete"
                        onClick={() => handleDelete(review.id)}
                        disabled={deletingId === review.id}
                        title="Delete review"
                      >
                        <FiTrash2 /> {deletingId === review.id ? 'Deleting...' : 'Delete'}
                      </button>
                    </div>
                  </>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default MyReviews;
