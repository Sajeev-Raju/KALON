import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { orderAPI } from '../services/api';
import {
  FiPackage,
  FiTruck,
  FiCheckCircle,
  FiClock,
  FiMapPin,
  FiArrowLeft,
  FiRefreshCw,
  FiXCircle,
} from 'react-icons/fi';
import toast from 'react-hot-toast';
import './OrderDetail.css';

const OrderDetail = () => {
  const { orderId } = useParams();
  const navigate = useNavigate();
  const { user, isAuthenticated } = useSelector((state) => state.auth);
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [cancelling, setCancelling] = useState(false);
  const [retrying, setRetrying] = useState(false);
  const [paymentAttempts, setPaymentAttempts] = useState([]);
  const [showAttempts, setShowAttempts] = useState(false);

  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    fetchOrder();
  }, [isAuthenticated, orderId, navigate]);

  // Load Razorpay script for retry payment
  useEffect(() => {
    if (!window.Razorpay) {
      const script = document.createElement('script');
      script.src = 'https://checkout.razorpay.com/v1/checkout.js';
      script.async = true;
      document.body.appendChild(script);
    }
  }, []);

  const fetchOrder = async () => {
    try {
      setLoading(true);
      const response = await orderAPI.getById(orderId);
      setOrder(response.data.data);
    } catch (error) {
      toast.error('Failed to load order details');
      navigate('/orders');
    } finally {
      setLoading(false);
    }
  };

  const fetchPaymentAttempts = async () => {
    try {
      const response = await orderAPI.getPaymentAttempts(orderId);
      setPaymentAttempts(response.data.data || []);
    } catch (error) {
      // Silently fail — not critical
    }
  };

  const handleCancelOrder = async () => {
    if (!window.confirm('Are you sure you want to cancel this order?')) {
      return;
    }

    try {
      setCancelling(true);
      await orderAPI.cancel(orderId);
      toast.success('Order cancelled successfully');
      fetchOrder();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to cancel order');
    } finally {
      setCancelling(false);
    }
  };

  const handleToggleAttempts = () => {
    if (!showAttempts && paymentAttempts.length === 0) {
      fetchPaymentAttempts();
    }
    setShowAttempts(!showAttempts);
  };

  const canRetryPayment = useCallback(() => {
    return (
      order &&
      order.status === 'PENDING' &&
      (order.paymentStatus === 'PENDING' || order.paymentStatus === 'FAILED') &&
      order.paymentMethod === 'RAZORPAY'
    );
  }, [order]);

  const handleRetryPayment = async () => {
    if (!window.Razorpay) {
      toast.error('Payment gateway is loading. Please refresh and try again.');
      return;
    }

    try {
      setRetrying(true);
      const response = await orderAPI.retryPayment(orderId);
      const data = response.data.data;

      const options = {
        key: data.keyId,
        amount: data.amountInPaise,
        currency: data.currency,
        name: 'Kalon',
        description: `Order #${order.orderNumber}`,
        order_id: data.razorpayOrderId,
        handler: async (razorpayResponse) => {
          try {
            await orderAPI.verifyRazorpayPayment({
              razorpayOrderId: razorpayResponse.razorpay_order_id,
              razorpayPaymentId: razorpayResponse.razorpay_payment_id,
              razorpaySignature: razorpayResponse.razorpay_signature,
            });
            toast.success('Payment successful!');
            fetchOrder();
          } catch (error) {
            toast.error(error.response?.data?.message || 'Payment verification failed');
          } finally {
            setRetrying(false);
          }
        },
        prefill: {
          name: `${user?.firstName || ''} ${user?.lastName || ''}`.trim(),
          email: user?.email || '',
          contact: user?.phoneNumber || '',
        },
        theme: { color: '#000000' },
        modal: {
          ondismiss: () => {
            toast('Payment cancelled.', { icon: '⏳' });
            setRetrying(false);
          },
        },
      };

      const razorpay = new window.Razorpay(options);
      razorpay.open();
      razorpay.on('payment.failed', () => {
        toast.error('Payment failed. You can try again.');
        setRetrying(false);
      });
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to retry payment');
      setRetrying(false);
    }
  };

  const getStatusSteps = () => {
    const steps = [
      { id: 'PENDING', label: 'Order Placed', icon: FiPackage },
      { id: 'CONFIRMED', label: 'Confirmed', icon: FiCheckCircle },
      { id: 'PROCESSING', label: 'Processing', icon: FiClock },
      { id: 'SHIPPED', label: 'Shipped', icon: FiTruck },
      { id: 'DELIVERED', label: 'Delivered', icon: FiCheckCircle },
    ];

    if (order?.status === 'CANCELLED') {
      return [{ id: 'CANCELLED', label: 'Cancelled', icon: FiXCircle, completed: true, current: true }];
    }

    if (order?.status === 'RETURNED') {
      return [{ id: 'RETURNED', label: 'Returned', icon: FiRefreshCw, completed: true, current: true }];
    }

    const statusIndex = steps.findIndex((s) => s.id === order?.status);

    return steps.map((step, index) => ({
      ...step,
      completed: index <= statusIndex,
      current: index === statusIndex,
    }));
  };

  const canCancel = () => {
    return order && ['PENDING', 'CONFIRMED'].includes(order.status);
  };

  const canReturn = (item) => {
    return (
      (order?.status === 'DELIVERED' || order?.status === 'SHIPPED') &&
      item.isReturnable &&
      !item.returnRequested
    );
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('en-IN', {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const formatPaymentMethod = (method) => {
    const labels = {
      RAZORPAY: 'Online Payment (Razorpay)',
      COD: 'Cash on Delivery',
      CREDIT_CARD: 'Credit Card',
      DEBIT_CARD: 'Debit Card',
      UPI: 'UPI',
      NET_BANKING: 'Net Banking',
      WALLET: 'Wallet',
    };
    return labels[method] || method || 'Online Payment';
  };

  const formatPaymentStatus = (status) => {
    const labels = {
      PENDING: 'Pending',
      COMPLETED: 'Paid',
      FAILED: 'Failed',
      REFUNDED: 'Refunded',
    };
    return labels[status] || status || 'Pending';
  };

  const getPaymentStatusClass = (status) => {
    if (status === 'COMPLETED') return 'paid';
    if (status === 'REFUNDED') return 'refunded';
    if (status === 'FAILED') return 'failed';
    return 'pending';
  };

  const getAttemptStatusClass = (status) => {
    if (status === 'SUCCESS') return 'paid';
    if (status === 'FAILED') return 'failed';
    if (status === 'EXPIRED') return 'failed';
    return 'pending';
  };

  const formatAttemptStatus = (status) => {
    const labels = { INITIATED: 'Initiated', SUCCESS: 'Success', FAILED: 'Failed', EXPIRED: 'Expired' };
    return labels[status] || status;
  };

  const formatAttemptSource = (source) => {
    const labels = { CHECKOUT: 'Checkout', RETRY: 'Retry', WEBHOOK: 'Webhook' };
    return labels[source] || source;
  };

  if (!isAuthenticated) {
    return null;
  }

  if (loading) {
    return (
      <div className="order-detail-page">
        <div className="container">
          <div className="loading">Loading order details...</div>
        </div>
      </div>
    );
  }

  if (!order) {
    return (
      <div className="order-detail-page">
        <div className="container">
          <div className="not-found">Order not found</div>
        </div>
      </div>
    );
  }

  return (
    <div className="order-detail-page">
      <div className="container">
        <Link to="/orders" className="back-link">
          <FiArrowLeft /> Back to Orders
        </Link>

        <div className="order-detail-header">
          <div className="header-info">
            <h1>Order #{order.orderNumber}</h1>
            <p className="order-date">Placed on {formatDate(order.createdAt)}</p>
          </div>
          <div className="header-actions">
            {canRetryPayment() && (
              <button
                className="btn btn-primary"
                onClick={handleRetryPayment}
                disabled={retrying}
              >
                {retrying ? 'Processing...' : 'Retry Payment'}
              </button>
            )}
            {canCancel() && (
              <button
                className="btn btn-outline-danger"
                onClick={handleCancelOrder}
                disabled={cancelling}
              >
                {cancelling ? 'Cancelling...' : 'Cancel Order'}
              </button>
            )}
          </div>
        </div>

        {/* Order Status Timeline */}
        <div className="order-status-section">
          <h2>Order Status</h2>
          <div className="status-timeline">
            {getStatusSteps().map((step, index) => {
              const Icon = step.icon;
              return (
                <div
                  key={step.id}
                  className={`timeline-step ${step.completed ? 'completed' : ''} ${
                    step.current ? 'current' : ''
                  }`}
                >
                  <div className="timeline-icon">
                    <Icon />
                  </div>
                  <div className="timeline-label">{step.label}</div>
                  {index < getStatusSteps().length - 1 && (
                    <div className={`timeline-line ${step.completed ? 'completed' : ''}`} />
                  )}
                </div>
              );
            })}
          </div>
        </div>

        <div className="order-detail-content">
          {/* Order Items */}
          <div className="order-items-section">
            <h2>Order Items</h2>
            <div className="order-items-list">
              {order.items?.map((item) => (
                <div key={item.id} className="order-item-card">
                  <img
                    src={item.productImage || '/placeholder.jpg'}
                    alt={item.productName}
                  />
                  <div className="item-details">
                    <h3>{item.productName}</h3>
                    <p className="item-variant">
                      Size: {item.size} • Color: {item.color}
                    </p>
                    <p className="item-quantity">Quantity: {item.quantity}</p>
                    <p className="item-price">₹{item.totalPrice?.toLocaleString()}</p>
                  </div>
                  <div className="item-actions">
                    {item.returnRequested && (
                      <span className="return-badge">Return Requested</span>
                    )}
                    {canReturn(item) && (
                      <Link
                        to={`/return/${order.id}/${item.id}`}
                        className="btn btn-outline-sm"
                      >
                        <FiRefreshCw /> Return/Exchange
                      </Link>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Order Summary & Shipping */}
          <div className="order-sidebar">
            {/* Shipping Address */}
            <div className="sidebar-card">
              <h3>
                <FiMapPin /> Shipping Address
              </h3>
              {order.shippingAddress && (
                <div className="address-details">
                  <p className="name">{order.shippingAddress.fullName}</p>
                  <p>{order.shippingAddress.addressLine1}</p>
                  {order.shippingAddress.addressLine2 && (
                    <p>{order.shippingAddress.addressLine2}</p>
                  )}
                  <p>
                    {order.shippingAddress.city}, {order.shippingAddress.state}{' '}
                    {order.shippingAddress.postalCode}
                  </p>
                  <p className="phone">Phone: {order.shippingAddress.phoneNumber}</p>
                </div>
              )}
            </div>

            {/* Payment Info */}
            <div className="sidebar-card">
              <h3>Payment Information</h3>
              <div className="payment-details">
                <p>
                  <span>Payment Method:</span>
                  <strong>{formatPaymentMethod(order.paymentMethod)}</strong>
                </p>
                <p>
                  <span>Payment Status:</span>
                  <strong className={getPaymentStatusClass(order.paymentStatus)}>
                    {formatPaymentStatus(order.paymentStatus)}
                  </strong>
                </p>
              </div>
              {order.paymentMethod === 'RAZORPAY' && (
                <div className="payment-attempts-section">
                  <button
                    className="toggle-attempts-btn"
                    onClick={handleToggleAttempts}
                  >
                    {showAttempts ? 'Hide' : 'View'} Payment History
                  </button>
                  {showAttempts && (
                    <div className="payment-attempts-list">
                      {paymentAttempts.length === 0 ? (
                        <p className="no-attempts">No payment attempts recorded.</p>
                      ) : (
                        paymentAttempts.map((attempt) => (
                          <div key={attempt.id} className="payment-attempt-item">
                            <div className="attempt-header">
                              <span className="attempt-number">Attempt #{attempt.attemptNumber}</span>
                              <span className={`attempt-status ${getAttemptStatusClass(attempt.status)}`}>
                                {formatAttemptStatus(attempt.status)}
                              </span>
                            </div>
                            <div className="attempt-details">
                              <span className="attempt-source">{formatAttemptSource(attempt.source)}</span>
                              <span className="attempt-date">
                                {new Date(attempt.createdAt).toLocaleString('en-IN', {
                                  day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit',
                                })}
                              </span>
                            </div>
                            {attempt.failureReason && (
                              <p className="attempt-failure">{attempt.failureReason}</p>
                            )}
                          </div>
                        ))
                      )}
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* Order Summary */}
            <div className="sidebar-card">
              <h3>Order Summary</h3>
              <div className="order-summary">
                <div className="summary-row">
                  <span>Subtotal</span>
                  <span>₹{order.subtotal?.toLocaleString()}</span>
                </div>
                <div className="summary-row">
                  <span>Shipping</span>
                  <span>
                    {order.shippingCost > 0
                      ? `₹${order.shippingCost}`
                      : 'Free'}
                  </span>
                </div>
                {order.discountAmount > 0 && (
                  <div className="summary-row discount">
                    <span>Discount</span>
                    <span>-₹{order.discountAmount}</span>
                  </div>
                )}
                <div className="summary-row total">
                  <span>Total</span>
                  <strong>₹{order.totalAmount?.toLocaleString()}</strong>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default OrderDetail;
