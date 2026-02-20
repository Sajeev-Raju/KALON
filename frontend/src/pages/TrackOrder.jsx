import { useState } from 'react';
import { Link } from 'react-router-dom';
import { FiSearch, FiPackage, FiTruck, FiCheckCircle, FiClock, FiXCircle } from 'react-icons/fi';
import { orderAPI } from '../services/api';
import toast from 'react-hot-toast';
import './TrackOrder.css';

const TrackOrder = () => {
  const [orderNumber, setOrderNumber] = useState('');
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleTrack = async (e) => {
    e.preventDefault();
    if (!orderNumber.trim()) return;

    setLoading(true);
    setError(null);
    setOrder(null);

    try {
      const response = await orderAPI.track(orderNumber.trim());
      setOrder(response.data.data);
    } catch (err) {
      setError('Order not found. Please check the order number and try again.');
      toast.error('Order not found');
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('en-IN', {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    });
  };

  const getStatusSteps = (status) => {
    if (status === 'CANCELLED') {
      return [{ id: 'CANCELLED', label: 'Cancelled', icon: FiXCircle, completed: true, current: true }];
    }

    if (status === 'RETURNED') {
      return [{ id: 'RETURNED', label: 'Returned', icon: FiPackage, completed: true, current: true }];
    }

    const steps = [
      { id: 'PENDING', label: 'Order Placed', icon: FiPackage },
      { id: 'CONFIRMED', label: 'Confirmed', icon: FiCheckCircle },
      { id: 'PROCESSING', label: 'Processing', icon: FiClock },
      { id: 'SHIPPED', label: 'Shipped', icon: FiTruck },
      { id: 'DELIVERED', label: 'Delivered', icon: FiCheckCircle },
    ];

    const statusIndex = steps.findIndex(s => s.id === status);

    return steps.map((step, index) => ({
      ...step,
      completed: index <= statusIndex,
      current: index === statusIndex,
    }));
  };

  return (
    <div className="track-order-page">
      <div className="container">
        <div className="track-header">
          <h1>Track Your Order</h1>
          <p>Enter your order number to track the status of your shipment</p>
        </div>

        <div className="track-form-container">
          <form className="track-form" onSubmit={handleTrack}>
            <div className="input-group">
              <input
                type="text"
                placeholder="Enter Order Number (e.g., KLN123456789)"
                value={orderNumber}
                onChange={(e) => setOrderNumber(e.target.value)}
                required
              />
              <button type="submit" className="btn btn-primary" disabled={loading}>
                <FiSearch /> {loading ? 'Tracking...' : 'Track Order'}
              </button>
            </div>
          </form>
        </div>

        {error && (
          <div className="track-error">
            <FiXCircle className="error-icon" />
            <p>{error}</p>
          </div>
        )}

        {order && (
          <div className="track-result">
            <div className="order-summary">
              <h2>Order #{order.orderNumber}</h2>
              <div className="summary-grid">
                <div>
                  <span className="label">Status:</span>
                  <span className="value status">{order.status}</span>
                </div>
                <div>
                  <span className="label">Order Date:</span>
                  <span className="value">{formatDate(order.createdAt)}</span>
                </div>
                <div>
                  <span className="label">Total Amount:</span>
                  <span className="value">₹{order.totalAmount?.toLocaleString()}</span>
                </div>
              </div>
            </div>

            <div className="tracking-timeline">
              <h3>Order Status</h3>
              <div className="timeline">
                {getStatusSteps(order.status).map((step, index) => {
                  const Icon = step.icon;
                  return (
                    <div
                      key={step.id}
                      className={`timeline-step ${step.completed ? 'completed' : ''} ${step.current ? 'current' : ''}`}
                    >
                      <div className="timeline-icon">
                        <Icon />
                      </div>
                      <div className="timeline-content">
                        <h4>{step.label}</h4>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            <div className="order-items">
              <h3>Order Items</h3>
              <div className="items-list">
                {order.items?.map((item) => (
                  <div key={item.id} className="track-item">
                    <img src={item.productImage || '/placeholder.jpg'} alt={item.productName} />
                    <div className="item-info">
                      <h4>{item.productName}</h4>
                      <p>Size: {item.size} • Color: {item.color} • Qty: {item.quantity}</p>
                      <p className="item-price">₹{item.totalPrice?.toLocaleString()}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {order.shippingAddress && (
              <div className="shipping-info">
                <h3>Shipping Address</h3>
                <div className="address-card">
                  <p className="name">{order.shippingAddress.fullName}</p>
                  <p>{order.shippingAddress.addressLine1}</p>
                  {order.shippingAddress.addressLine2 && <p>{order.shippingAddress.addressLine2}</p>}
                  <p>{order.shippingAddress.city}, {order.shippingAddress.state} - {order.shippingAddress.postalCode}</p>
                  <p>Phone: {order.shippingAddress.phoneNumber}</p>
                </div>
              </div>
            )}
          </div>
        )}

        {!order && (
          <div className="track-help">
            <h3>Need Help?</h3>
            <p>You can find your order number in:</p>
            <ul>
              <li>Your order confirmation email</li>
              <li>Your account order history</li>
              <li>The SMS sent to your registered mobile number</li>
            </ul>
          </div>
        )}
      </div>
    </div>
  );
};

export default TrackOrder;



