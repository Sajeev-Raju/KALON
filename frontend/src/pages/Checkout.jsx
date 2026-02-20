import { useState, useEffect, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useSelector, useDispatch } from 'react-redux';
import { orderAPI, addressAPI, cartAPI } from '../services/api';
import { fetchCart, clearCart } from '../redux/slices/cartSlice';
import CheckoutAuthModal from '../components/auth/CheckoutAuthModal';
import toast from 'react-hot-toast';
import './Checkout.css';

const Checkout = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const { items: serverItems, subtotal: serverSubtotal, shippingCost: serverShipping, total: serverTotal } = useSelector((state) => state.cart);
  const { items: guestItems, subtotal: guestSubtotal, shippingCost: guestShipping, total: guestTotal } = useSelector((state) => state.guestCart);
  const { user, isAuthenticated } = useSelector((state) => state.auth);

  const [addresses, setAddresses] = useState([]);
  const [selectedAddressId, setSelectedAddressId] = useState(null);
  const [paymentMethod, setPaymentMethod] = useState('');
  const [paymentMethods, setPaymentMethods] = useState([]);
  const [notes, setNotes] = useState('');
  const [loading, setLoading] = useState(false);
  const [loadingAddresses, setLoadingAddresses] = useState(true);
  const [showAuthModal, setShowAuthModal] = useState(false);
  const [razorpayLoaded, setRazorpayLoaded] = useState(!!window.Razorpay);

  // Use guest cart data when not authenticated, server cart when authenticated
  const items = isAuthenticated ? serverItems : guestItems;
  const subtotal = isAuthenticated ? serverSubtotal : guestSubtotal;
  const shippingCost = isAuthenticated ? serverShipping : guestShipping;
  const total = isAuthenticated ? serverTotal : guestTotal;

  // Filter payment methods by order total amount constraints
  const availableMethods = useMemo(() => {
    return paymentMethods.filter((m) => {
      if (m.minAmount && total < m.minAmount) return false;
      if (m.maxAmount && total > m.maxAmount) return false;
      return true;
    });
  }, [paymentMethods, total]);

  // If selected method is no longer available, switch to first available
  useEffect(() => {
    if (availableMethods.length > 0 && !availableMethods.find(m => m.paymentMethod === paymentMethod)) {
      setPaymentMethod(availableMethods[0].paymentMethod);
    }
  }, [availableMethods, paymentMethod]);

  // Preload Razorpay script
  useEffect(() => {
    if (!window.Razorpay) {
      const script = document.createElement('script');
      script.src = 'https://checkout.razorpay.com/v1/checkout.js';
      script.async = true;
      script.onload = () => setRazorpayLoaded(true);
      script.onerror = () => {
        console.error('Failed to load Razorpay script');
        setRazorpayLoaded(false);
      };
      document.body.appendChild(script);
    }
  }, []);

  const loadAddresses = useCallback(async () => {
    try {
      setLoadingAddresses(true);
      const response = await addressAPI.getAll();
      setAddresses(response.data.data || []);
      if (response.data.data && response.data.data.length > 0) {
        const defaultAddress = response.data.data.find(addr => addr.isDefault) || response.data.data[0];
        setSelectedAddressId(defaultAddress.id);
      }
    } catch (error) {
      toast.error('Failed to load addresses');
    } finally {
      setLoadingAddresses(false);
    }
  }, []);

  const loadPaymentMethods = useCallback(async () => {
    try {
      const response = await orderAPI.getPaymentMethods();
      const methods = response.data.data || [];
      setPaymentMethods(methods);
      // Auto-select first method
      if (methods.length > 0 && !paymentMethod) {
        setPaymentMethod(methods[0].paymentMethod);
      }
    } catch (error) {
      // Fallback to hardcoded methods if API fails
      setPaymentMethods([
        { paymentMethod: 'RAZORPAY', displayName: 'Pay Online', description: 'UPI, Cards, Net Banking, Wallets' },
        { paymentMethod: 'COD', displayName: 'Cash on Delivery', description: 'Pay when you receive your order' },
      ]);
      if (!paymentMethod) setPaymentMethod('RAZORPAY');
    }
  }, []);

  useEffect(() => {
    if (isAuthenticated) {
      loadAddresses();
      loadPaymentMethods();
      dispatch(fetchCart());
    } else {
      // Guest user — show auth modal immediately
      setLoadingAddresses(false);
      setShowAuthModal(true);
    }
  }, [isAuthenticated, dispatch, loadAddresses, loadPaymentMethods]);

  const handleAuthSuccess = useCallback(() => {
    setShowAuthModal(false);
    // After auth, Layout will trigger cart merge via the merge logic
    // We just need to reload addresses, payment methods and cart
    loadAddresses();
    loadPaymentMethods();
    dispatch(fetchCart());
  }, [loadAddresses, loadPaymentMethods, dispatch]);

  const handleCODOrder = async () => {
    try {
      const response = await orderAPI.create({
        addressId: selectedAddressId,
        paymentMethod: 'COD',
        notes: notes,
      });

      toast.success('Order placed successfully! Pay on delivery.');
      dispatch(clearCart());
      navigate(`/orders/${response.data.data.id}`);
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to place order');
      setLoading(false);
    }
  };

  const handleRazorpayOrder = async () => {
    if (!razorpayLoaded || !window.Razorpay) {
      toast.error('Payment gateway is loading. Please try again.');
      setLoading(false);
      return;
    }

    try {
      // Create Razorpay order
      const response = await orderAPI.createRazorpayOrder({
        addressId: selectedAddressId,
        notes: notes,
      });

      const data = response.data.data;

      const options = {
        key: data.keyId,
        amount: data.amountInPaise,
        currency: data.currency,
        name: 'Kalon',
        description: `Order #${data.orderId}`,
        order_id: data.razorpayOrderId,
        handler: async (razorpayResponse) => {
          try {
            // Verify payment
            await orderAPI.verifyRazorpayPayment({
              razorpayOrderId: razorpayResponse.razorpay_order_id,
              razorpayPaymentId: razorpayResponse.razorpay_payment_id,
              razorpaySignature: razorpayResponse.razorpay_signature,
              addressId: selectedAddressId,
              notes: notes,
            });

            toast.success('Payment successful! Order placed.');
            dispatch(clearCart());
            navigate(`/orders/${data.orderId}`);
          } catch (error) {
            toast.error(error.response?.data?.message || 'Payment verification failed');
            setLoading(false);
          }
        },
        prefill: {
          name: `${user?.firstName || ''} ${user?.lastName || ''}`.trim(),
          email: user?.email || '',
          contact: user?.phoneNumber || '',
        },
        theme: {
          color: '#000000',
        },
        modal: {
          ondismiss: () => {
            toast('Payment cancelled. Your order is saved and will expire in 30 minutes.', {
              icon: '⏳',
              duration: 5000,
            });
            setLoading(false);
          },
        },
      };

      const razorpay = new window.Razorpay(options);
      razorpay.open();
      razorpay.on('payment.failed', (response) => {
        toast.error('Payment failed. Your order is saved — you can retry from your orders page.');
        setLoading(false);
      });
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to initiate payment');
      setLoading(false);
    }
  };

  const handlePlaceOrder = async () => {
    if (!isAuthenticated) {
      setShowAuthModal(true);
      return;
    }

    if (!selectedAddressId) {
      toast.error('Please select a delivery address');
      return;
    }

    if (items.length === 0) {
      toast.error('Your cart is empty');
      return;
    }

    setLoading(true);
    try {
      // Pre-payment stock validation
      const validateResponse = await cartAPI.validate();
      const validatedCart = validateResponse.data.data;
      if (validatedCart.hasStockIssues) {
        toast.error('Some items have stock issues. Redirecting to cart...');
        dispatch(fetchCart());
        navigate('/cart');
        setLoading(false);
        return;
      }
      if (validatedCart.itemCount !== items.reduce((sum, i) => sum + i.quantity, 0)) {
        toast.error('Cart was adjusted due to stock changes. Please review.');
        dispatch(fetchCart());
        navigate('/cart');
        setLoading(false);
        return;
      }

      if (paymentMethod === 'COD') {
        await handleCODOrder();
      } else {
        await handleRazorpayOrder();
      }
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to place order');
      setLoading(false);
    }
  };

  // Show empty cart for both guest and authenticated users
  if (items.length === 0 && !showAuthModal) {
    return (
      <div className="checkout-page">
        <div className="empty-cart">
          <h2>Your cart is empty</h2>
          <button onClick={() => navigate('/')} className="btn btn-primary">
            Continue Shopping
          </button>
        </div>
      </div>
    );
  }

  // Show auth modal for guest users
  if (!isAuthenticated) {
    return (
      <div className="checkout-page">
        <div className="checkout-container">
          <h1>Checkout</h1>
          <div className="checkout-content">
            <div className="checkout-section">
              <h2>Order Summary</h2>
              <div className="order-items">
                {items.map((item) => (
                  <div key={item.id} className="order-item">
                    <img src={item.productImage || '/placeholder.jpg'} alt={item.productName} />
                    <div className="item-details">
                      <h4>{item.productName}</h4>
                      <p>Size: {item.size}, Color: {item.color}</p>
                      <p>Qty: {item.quantity} x ₹{item.price}</p>
                    </div>
                    <div className="item-total">₹{item.totalPrice}</div>
                  </div>
                ))}
              </div>

              <div className="order-summary">
                <div className="summary-row">
                  <span>Subtotal</span>
                  <span>₹{subtotal}</span>
                </div>
                <div className="summary-row">
                  <span>Shipping</span>
                  <span>{shippingCost === 0 ? 'FREE' : `₹${shippingCost}`}</span>
                </div>
                <div className="summary-row total">
                  <span>Total</span>
                  <span>₹{total}</span>
                </div>
              </div>

              <button
                className="btn btn-primary btn-full btn-lg"
                onClick={() => setShowAuthModal(true)}
              >
                Login / Register to Continue
              </button>
            </div>
          </div>
        </div>

        <CheckoutAuthModal
          isOpen={showAuthModal}
          onClose={() => setShowAuthModal(false)}
          onAuthSuccess={handleAuthSuccess}
        />
      </div>
    );
  }

  if (loadingAddresses) {
    return <div className="checkout-page"><div className="loading">Loading...</div></div>;
  }

  return (
    <div className="checkout-page">
      <div className="checkout-container">
        <h1>Checkout</h1>

        <div className="checkout-content">
          {/* Delivery Address */}
          <div className="checkout-section">
            <h2>Delivery Address</h2>
            {addresses.length === 0 ? (
              <div className="no-address">
                <p>No addresses found. Please add an address to continue.</p>
                <button onClick={() => navigate('/addresses')} className="btn btn-primary">
                  Add Address
                </button>
              </div>
            ) : (
              <div className="address-list">
                {addresses.map((address) => (
                  <label key={address.id} className="address-card">
                    <input
                      type="radio"
                      name="address"
                      value={address.id}
                      checked={selectedAddressId === address.id}
                      onChange={(e) => setSelectedAddressId(parseInt(e.target.value))}
                    />
                    <div className="address-details">
                      <div className="address-header">
                        <strong>{address.fullName}</strong>
                        {address.isDefault && <span className="default-badge">Default</span>}
                      </div>
                      <p>{address.addressLine1}</p>
                      {address.addressLine2 && <p>{address.addressLine2}</p>}
                      <p>{address.city}, {address.state} {address.postalCode}</p>
                      <p>{address.country}</p>
                      <p>Phone: {address.phoneNumber}</p>
                    </div>
                  </label>
                ))}
              </div>
            )}

            {/* Payment Method */}
            <h2 className="section-title-spaced">Payment Method</h2>
            <div className="payment-methods">
              {availableMethods.map((method) => (
                <label
                  key={method.paymentMethod}
                  className={`payment-method-card ${paymentMethod === method.paymentMethod ? 'selected' : ''}`}
                >
                  <input
                    type="radio"
                    name="paymentMethod"
                    value={method.paymentMethod}
                    checked={paymentMethod === method.paymentMethod}
                    onChange={(e) => setPaymentMethod(e.target.value)}
                  />
                  <div className="payment-method-info">
                    <strong>{method.displayName}</strong>
                    <span>{method.description}</span>
                  </div>
                </label>
              ))}
              {availableMethods.length === 0 && paymentMethods.length > 0 && (
                <p className="no-methods-warning">No payment methods available for this order amount.</p>
              )}
            </div>
          </div>

          {/* Order Summary */}
          <div className="checkout-section">
            <h2>Order Summary</h2>
            <div className="order-items">
              {items.map((item) => (
                <div key={item.id} className="order-item">
                  <img src={item.productImage || '/placeholder.jpg'} alt={item.productName} />
                  <div className="item-details">
                    <h4>{item.productName}</h4>
                    <p>Size: {item.size}, Color: {item.color}</p>
                    <p>Qty: {item.quantity} x ₹{item.price}</p>
                  </div>
                  <div className="item-total">₹{item.totalPrice}</div>
                </div>
              ))}
            </div>

            <div className="order-summary">
              <div className="summary-row">
                <span>Subtotal</span>
                <span>₹{subtotal}</span>
              </div>
              <div className="summary-row">
                <span>Shipping</span>
                <span>{shippingCost === 0 ? 'FREE' : `₹${shippingCost}`}</span>
              </div>
              <div className="summary-row total">
                <span>Total</span>
                <span>₹{total}</span>
              </div>
            </div>

            <div className="notes-section">
              <label>
                Order Notes (Optional)
                <textarea
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  placeholder="Any special instructions for delivery..."
                  rows="3"
                />
              </label>
            </div>

            <button
              className="btn btn-primary btn-full btn-lg"
              onClick={handlePlaceOrder}
              disabled={loading || !selectedAddressId || !paymentMethod}
            >
              {loading
                ? 'Processing...'
                : paymentMethod === 'COD'
                  ? 'Place Order (Cash on Delivery)'
                  : `Pay with ${availableMethods.find(m => m.paymentMethod === paymentMethod)?.displayName || 'Online Payment'}`}
            </button>
          </div>
        </div>
      </div>

      <CheckoutAuthModal
        isOpen={showAuthModal}
        onClose={() => setShowAuthModal(false)}
        onAuthSuccess={handleAuthSuccess}
      />
    </div>
  );
};

export default Checkout;
