import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { orderAPI } from '../services/api';
import { FiPackage, FiChevronRight, FiCalendar, FiClock } from 'react-icons/fi';
import toast from 'react-hot-toast';
import './Orders.css';

const Orders = () => {
  const navigate = useNavigate();
  const { isAuthenticated } = useSelector((state) => state.auth);
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    fetchOrders();
  }, [isAuthenticated, page, navigate]);

  const fetchOrders = async () => {
    try {
      setLoading(true);
      const response = await orderAPI.getPaginated({ page, size: 10 });
      const data = response.data.data;
      setOrders(data.content || []);
      setTotalPages(data.totalPages || 0);
    } catch (error) {
      toast.error('Failed to load orders');
    } finally {
      setLoading(false);
    }
  };

  const getStatusColor = (status) => {
    const colors = {
      PENDING: '#ed8936',
      CONFIRMED: '#4299e1',
      PROCESSING: '#667eea',
      SHIPPED: '#48bb78',
      DELIVERED: '#38b2ac',
      CANCELLED: '#f56565',
      RETURNED: '#9f7aea',
    };
    return colors[status] || '#666';
  };

  const getStatusLabel = (status) => {
    const labels = {
      PENDING: 'Order Placed',
      CONFIRMED: 'Confirmed',
      PROCESSING: 'Processing',
      SHIPPED: 'Shipped',
      DELIVERED: 'Delivered',
      CANCELLED: 'Cancelled',
      RETURNED: 'Returned',
    };
    return labels[status] || status;
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('en-IN', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    });
  };

  if (!isAuthenticated) {
    return null;
  }

  return (
    <div className="orders-page">
      <div className="container">
        <div className="orders-header">
          <h1>My Orders</h1>
          <p>Track and manage your orders</p>
        </div>

        {loading ? (
          <div className="loading">Loading orders...</div>
        ) : orders.length === 0 ? (
          <div className="empty-orders">
            <FiPackage className="empty-icon" />
            <h2>No orders yet</h2>
            <p>Looks like you haven't placed any orders yet.</p>
            <Link to="/products" className="btn btn-primary">
              Start Shopping
            </Link>
          </div>
        ) : (
          <>
            <div className="orders-list">
              {orders.map((order) => (
                <div key={order.id} className="order-card">
                  <div className="order-card-header">
                    <div className="order-info">
                      <span className="order-number">Order #{order.orderNumber}</span>
                      <span className="order-date">
                        <FiCalendar /> {formatDate(order.createdAt)}
                      </span>
                    </div>
                    <div
                      className="order-status"
                      style={{ backgroundColor: getStatusColor(order.status) }}
                    >
                      {getStatusLabel(order.status)}
                    </div>
                  </div>

                  <div className="order-items-preview">
                    {order.items?.slice(0, 3).map((item) => (
                      <div key={item.id} className="order-item-preview">
                        <img
                          src={item.productImage || '/placeholder.jpg'}
                          alt={item.productName}
                        />
                        <div className="item-info">
                          <span className="item-name">{item.productName}</span>
                          <span className="item-variant">
                            {item.size} • {item.color} • Qty: {item.quantity}
                          </span>
                        </div>
                      </div>
                    ))}
                    {order.items?.length > 3 && (
                      <div className="more-items">
                        +{order.items.length - 3} more item(s)
                      </div>
                    )}
                  </div>

                  <div className="order-card-footer">
                    <div className="order-total">
                      <span>Total:</span>
                      <strong>₹{order.totalAmount?.toLocaleString()}</strong>
                    </div>
                    <Link to={`/orders/${order.id}`} className="view-order-btn">
                      View Details <FiChevronRight />
                    </Link>
                  </div>
                </div>
              ))}
            </div>

            {totalPages > 1 && (
              <div className="pagination">
                <button
                  className="btn btn-outline"
                  disabled={page === 0}
                  onClick={() => setPage(page - 1)}
                >
                  Previous
                </button>
                <span className="page-info">
                  Page {page + 1} of {totalPages}
                </span>
                <button
                  className="btn btn-outline"
                  disabled={page >= totalPages - 1}
                  onClick={() => setPage(page + 1)}
                >
                  Next
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
};

export default Orders;
