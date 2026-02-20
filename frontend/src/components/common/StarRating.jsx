import './StarRating.css';

const StarRating = ({ rating, size = 'md', showValue = false, count = null }) => {
  const stars = [];
  for (let i = 1; i <= 5; i++) {
    if (rating >= i) {
      stars.push(<span key={i} className="star full">★</span>);
    } else if (rating >= i - 0.5) {
      stars.push(
        <span key={i} className="star half">
          <span className="star-bg">☆</span>
          <span className="star-fill">★</span>
        </span>
      );
    } else {
      stars.push(<span key={i} className="star empty">☆</span>);
    }
  }

  return (
    <span className={`star-rating star-rating-${size}`}>
      <span className="stars-container">{stars}</span>
      {showValue && <span className="rating-value">{rating.toFixed(1)}</span>}
      {count !== null && <span className="rating-count">({count})</span>}
    </span>
  );
};

export default StarRating;
