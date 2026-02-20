import { useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { FiTrash2, FiPlus, FiMinus, FiShoppingBag, FiAlertTriangle } from 'react-icons/fi';
import { fetchCart, updateCartItemQuantity, removeFromCart } from '../redux/slices/cartSlice';
import { updateGuestCartQuantity, removeGuestCartItem } from '../redux/slices/guestCartSlice';
import toast from 'react-hot-toast';
import './Cart.css';

const Cart = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { items: serverItems, subtotal: serverSubtotal, shippingCost: serverShipping, total: serverTotal, loading, hasStockIssues: serverStockIssues } = useSelector((state) => state.cart);
  const { items: guestItems, subtotal: guestSubtotal, shippingCost: guestShipping, total: guestTotal } = useSelector((state) => state.guestCart);
  const { isAuthenticated } = useSelector((state) => state.auth);

  const items = isAuthenticated ? serverItems : guestItems;
  const subtotal = isAuthenticated ? serverSubtotal : guestSubtotal;
  const shippingCost = isAuthenticated ? serverShipping : guestShipping;
  const total = isAuthenticated ? serverTotal : guestTotal;

  // Compute stock issues for guest cart
  const guestStockIssues = guestItems.some(item =>
    item.isAvailable === false || (item.stockQuantity != null && item.quantity > item.stockQuantity)
  );
  const hasStockIssues = isAuthenticated ? serverStockIssues : guestStockIssues;

  useEffect(() => {
    if (isAuthenticated) {
      dispatch(fetchCart());
    }
  }, [isAuthenticated, dispatch]);

  const handleQuantityChange = (itemId, newQuantity, maxStock) => {
    if (newQuantity < 1) return;
    if (maxStock != null && newQuantity > maxStock) {
      toast.error(`Only ${maxStock} available in stock`);
      return;
    }
    if (isAuthenticated) {
      dispatch(updateCartItemQuantity({ itemId, quantity: newQuantity }))
        .unwrap()
        .catch((error) => {
          if (error.stockError) {
            toast.error(`Only ${error.stockError.availableStock} in stock (${error.stockError.currentCartQuantity} already in cart)`);
          } else {
            toast.error(error.message || error);
          }
        });
    } else {
      dispatch(updateGuestCartQuantity({ itemId, quantity: newQuantity }));
    }
  };

  const handleRemoveItem = (itemId) => {
    if (isAuthenticated) {
      dispatch(removeFromCart(itemId))
        .unwrap()
        .then(() => toast.success('Item removed from cart'))
        .catch((error) => toast.error(error.message || error));
    } else {
      dispatch(removeGuestCartItem(itemId));
      toast.success('Item removed from cart');
    }
  };

  const handleCheckout = () => {
    if (hasStockIssues) {
      toast.error('Please resolve stock issues before checkout');
      return;
    }
    navigate('/checkout');
  };

  const getStockWarning = (item) => {
    if (item.isAvailable === false || (item.stockQuantity != null && item.stockQuantity === 0)) {
      return { text: 'Out of Stock — Please remove this item', type: 'out-of-stock' };
    }
    if (item.stockQuantity != null && item.quantity > item.stockQuantity) {
      return { text: `Only ${item.stockQuantity} available — Reduce quantity`, type: 'over-limit' };
    }
    if (item.stockQuantity != null && item.stockQuantity <= 5) {
      return { text: `Only ${item.stockQuantity} left in stock`, type: 'low-stock' };
    }
    return null;
  };

  if (isAuthenticated && loading) {
    return (
      <div className="cart-page">
        <div className="container">
          <div className="loading">Loading...</div>
        </div>
      </div>
    );
  }

  if (items.length === 0) {
    return (
      <div className="cart-page">
        <div className="container">
          <div className="empty-cart">
            <FiShoppingBag className="empty-icon" />
            <h2>Your cart is empty</h2>
            <p>Looks like you haven't added anything to your cart yet</p>
            <Link to="/" className="btn btn-primary">Continue Shopping</Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="cart-page">
      <div className="container">
        <div className="cart-header">
          <h1>Shopping Cart</h1>
          <p>{items.length} {items.length === 1 ? 'item' : 'items'}</p>
        </div>

        {hasStockIssues && (
          <div className="stock-issues-banner">
            <FiAlertTriangle />
            <span>Some items in your cart have stock issues. Please update quantities or remove unavailable items before checkout.</span>
          </div>
        )}

        <div className="cart-content">
          {/* Cart Items */}
          <div className="cart-items">
            {items.map((item) => {
              const stockWarning = getStockWarning(item);
              const maxStock = item.stockQuantity;
              const isOutOfStock = item.isAvailable === false || (maxStock != null && maxStock === 0);

              return (
                <div key={item.id} className={`cart-item ${stockWarning ? 'stock-issue' : ''}`}>
                  <div className="item-image">
                    <img src={item.productImage || 'https://via.placeholder.com/120x150'} alt={item.productName} />
                  </div>

                  <div className="item-details">
                    <h3 className="item-name">{item.productName}</h3>
                    <p className="item-variant">
                      Size: {item.size} | Color: {item.color}
                    </p>
                    <p className="item-price">₹{item.price}</p>

                    {stockWarning && (
                      <p className={`stock-warning ${stockWarning.type}`}>
                        {stockWarning.text}
                      </p>
                    )}

                    <div className="item-actions">
                      {!isOutOfStock && (
                        <div className="quantity-control">
                          <button
                            onClick={() => handleQuantityChange(item.id, item.quantity - 1, maxStock)}
                            disabled={item.quantity <= 1}
                          >
                            <FiMinus />
                          </button>
                          <input
                            type="number"
                            className="quantity-input"
                            value={item.quantity}
                            min="1"
                            max={maxStock || undefined}
                            onChange={(e) => {
                              const val = parseInt(e.target.value, 10);
                              if (!isNaN(val) && val >= 1) {
                                handleQuantityChange(item.id, val, maxStock);
                              }
                            }}
                          />
                          <button
                            onClick={() => handleQuantityChange(item.id, item.quantity + 1, maxStock)}
                            disabled={maxStock != null && item.quantity >= maxStock}
                          >
                            <FiPlus />
                          </button>
                        </div>
                      )}

                      <button
                        className="remove-btn"
                        onClick={() => handleRemoveItem(item.id)}
                      >
                        <FiTrash2 /> Remove
                      </button>
                    </div>
                  </div>

                  <div className="item-total">
                    <p>₹{item.totalPrice}</p>
                  </div>
                </div>
              );
            })}
          </div>

          {/* Cart Summary */}
          <div className="cart-summary">
            <h3>Order Summary</h3>

            <div className="summary-row">
              <span>Subtotal</span>
              <span>₹{subtotal}</span>
            </div>

            <div className="summary-row">
              <span>Shipping</span>
              <span>{shippingCost === 0 ? 'FREE' : `₹${shippingCost}`}</span>
            </div>

            {shippingCost > 0 && (
              <p className="shipping-info">
                Add ₹{999 - subtotal} more for FREE shipping
              </p>
            )}

            <div className="summary-row total">
              <span>Total</span>
              <span>₹{total}</span>
            </div>

            <button
              className="btn btn-primary btn-full btn-lg"
              onClick={handleCheckout}
              disabled={hasStockIssues}
            >
              {hasStockIssues ? 'Resolve Stock Issues' : isAuthenticated ? 'Proceed to Checkout' : 'Login to Checkout'}
            </button>

            <Link to="/" className="continue-shopping">
              Continue Shopping
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Cart;
