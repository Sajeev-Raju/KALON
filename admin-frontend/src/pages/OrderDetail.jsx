import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { orderAPI } from '../services/api';
import { FiArrowLeft, FiPackage, FiMapPin, FiCreditCard } from 'react-icons/fi';
import toast from 'react-hot-toast';
import { getOrderStatusColor } from '../utils/statusColors';
import './OrderDetail.css';

const OrderDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchOrder();
  }, [id]);

  const fetchOrder = async () => {
    try {
      setLoading(true);
      const response = await orderAPI.getById(id);
      setOrder(response.data.data);
    } catch (error) {
      toast.error('Failed to load order details');
      navigate('/admin/orders');
    } finally {
      setLoading(false);
    }
  };

  const handleStatusUpdate = async (newStatus) => {
    try {
      await orderAPI.updateStatus(id, newStatus);
      toast.success('Order status updated');
      fetchOrder();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to update order status');
    }
  };

  const getStatusColor = getOrderStatusColor;

  if (loading) {
    return <div className="order-detail-loading">Loading order details...</div>;
  }

  if (!order) {
    return <div className="order-detail-error">Order not found</div>;
  }

  return (
    <div className="order-detail-page">
      <div className="order-detail-header">
        <button className="back-button" onClick={() => navigate('/admin/orders')}>
          <FiArrowLeft /> Back to Orders
        </button>
        <div className="header-actions">
          <h1>Order Details</h1>
          <select
            value={order.status}
            onChange={(e) => handleStatusUpdate(e.target.value)}
            className="status-select-large"
            style={{ borderColor: getStatusColor(order.status) }}
          >
            <option value="PENDING">Pending</option>
            <option value="CONFIRMED">Confirmed</option>
            <option value="PROCESSING">Processing</option>
            <option value="SHIPPED">Shipped</option>
            <option value="DELIVERED">Delivered</option>
            <option value="CANCELLED">Cancelled</option>
            <option value="RETURNED">Returned</option>
          </select>
        </div>
      </div>

      <div className="order-detail-content">
        <div className="order-info-grid">
          {/* Order Summary */}
          <div className="info-card">
            <h3>
              <FiPackage /> Order Information
            </h3>
            <div className="info-row">
              <span className="label">Order Number:</span>
              <span className="value">{order.orderNumber}</span>
            </div>
            <div className="info-row">
              <span className="label">Order Date:</span>
              <span className="value">
                {new Date(order.createdAt).toLocaleString()}
              </span>
            </div>
            <div className="info-row">
              <span className="label">Status:</span>
              <span
                className="status-badge"
                style={{ backgroundColor: `${getStatusColor(order.status)}20`, color: getStatusColor(order.status) }}
              >
                {order.status}
              </span>
            </div>
            <div className="info-row">
              <span className="label">Payment Status:</span>
              <span className="value">{order.paymentStatus || 'N/A'}</span>
            </div>
            <div className="info-row">
              <span className="label">Payment Method:</span>
              <span className="value">{order.paymentMethod || 'N/A'}</span>
            </div>
            {order.paymentId && (
              <div className="info-row">
                <span className="label">Payment ID:</span>
                <span className="value">{order.paymentId}</span>
              </div>
            )}
            {order.trackingNumber && (
              <div className="info-row">
                <span className="label">Tracking Number:</span>
                <span className="value">{order.trackingNumber}</span>
              </div>
            )}
          </div>

          {/* Shipping Address */}
          {order.shippingAddress && (
            <div className="info-card">
              <h3>
                <FiMapPin /> Shipping Address
              </h3>
              <div className="address-block">
                <p className="address-name">{order.shippingAddress.fullName}</p>
                <p>{order.shippingAddress.addressLine1}</p>
                {order.shippingAddress.addressLine2 && (
                  <p>{order.shippingAddress.addressLine2}</p>
                )}
                <p>
                  {order.shippingAddress.city}, {order.shippingAddress.state}{' '}
                  {order.shippingAddress.postalCode}
                </p>
                <p>{order.shippingAddress.country}</p>
                <p className="address-phone">
                  Phone: {order.shippingAddress.phoneNumber}
                </p>
              </div>
            </div>
          )}

          {/* Order Summary */}
          <div className="info-card">
            <h3>
              <FiCreditCard /> Order Summary
            </h3>
            <div className="summary-row">
              <span>Subtotal:</span>
              <span>₹{order.subtotal?.toLocaleString()}</span>
            </div>
            <div className="summary-row">
              <span>Shipping:</span>
              <span>₹{order.shippingCost?.toLocaleString()}</span>
            </div>
            {order.taxAmount && order.taxAmount > 0 && (
              <div className="summary-row">
                <span>Tax:</span>
                <span>₹{order.taxAmount.toLocaleString()}</span>
              </div>
            )}
            {order.discountAmount && order.discountAmount > 0 && (
              <div className="summary-row discount">
                <span>Discount:</span>
                <span>-₹{order.discountAmount.toLocaleString()}</span>
              </div>
            )}
            <div className="summary-row total">
              <span>Total:</span>
              <span>₹{order.totalAmount?.toLocaleString()}</span>
            </div>
          </div>
        </div>

        {/* Order Items */}
        <div className="order-items-card">
          <h3>Order Items ({order.items?.length || 0})</h3>
          <div className="order-items-table">
            <table>
              <thead>
                <tr>
                  <th>Product</th>
                  <th>Size</th>
                  <th>Color</th>
                  <th>Quantity</th>
                  <th>Price</th>
                  <th>Total</th>
                </tr>
              </thead>
              <tbody>
                {order.items?.map((item) => (
                  <tr key={item.id}>
                    <td>
                      <div className="product-info-cell">
                        {item.productImage && (
                          <img
                            src={item.productImage}
                            alt={item.productName}
                            className="product-image-small"
                          />
                        )}
                        <span>{item.productName}</span>
                      </div>
                    </td>
                    <td>{item.size}</td>
                    <td>
                      <span className="color-badge">{item.color}</span>
                    </td>
                    <td>{item.quantity}</td>
                    <td>₹{item.price?.toLocaleString()}</td>
                    <td className="total-cell">
                      ₹{item.totalPrice?.toLocaleString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
};

export default OrderDetail;



