import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { returnAPI } from '../services/api';
import { FiPackage, FiClock, FiCheckCircle, FiXCircle, FiRefreshCw, FiSlash } from 'react-icons/fi';
import toast from 'react-hot-toast';
import './MyReturns.css';

const MyReturns = () => {
  const navigate = useNavigate();
  const { isAuthenticated } = useSelector((state) => state.auth);
  const [returns, setReturns] = useState([]);
  const [loading, setLoading] = useState(true);
  const [cancellingId, setCancellingId] = useState(null);

  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    fetchReturns();
  }, [isAuthenticated, navigate]);

  const fetchReturns = async () => {
    try {
      setLoading(true);
      const response = await returnAPI.getAll();
      setReturns(response.data.data || []);
    } catch (error) {
      toast.error('Failed to load return requests');
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = async (returnRequestId) => {
    if (!window.confirm('Are you sure you want to cancel this return request?')) {
      return;
    }

    try {
      setCancellingId(returnRequestId);
      await returnAPI.cancel(returnRequestId);
      toast.success('Return request cancelled');
      fetchReturns();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to cancel return request');
    } finally {
      setCancellingId(null);
    }
  };

  const getStatusInfo = (status) => {
    const statusMap = {
      PENDING: { label: 'Pending', color: '#ed8936', icon: FiClock },
      APPROVED: { label: 'Approved', color: '#48bb78', icon: FiCheckCircle },
      REJECTED: { label: 'Rejected', color: '#f56565', icon: FiXCircle },
      PROCESSING: { label: 'Processing', color: '#4299e1', icon: FiRefreshCw },
      COMPLETED: { label: 'Completed', color: '#38b2ac', icon: FiCheckCircle },
      CANCELLED: { label: 'Cancelled', color: '#a0aec0', icon: FiSlash },
    };
    return statusMap[status] || { label: status, color: '#666', icon: FiPackage };
  };

  const getRefundStatusInfo = (status) => {
    const statusMap = {
      PENDING: { label: 'Refund Pending', color: '#ed8936' },
      PROCESSING: { label: 'Refund Processing', color: '#4299e1' },
      COMPLETED: { label: 'Refunded', color: '#48bb78' },
      FAILED: { label: 'Refund Failed', color: '#f56565' },
    };
    return statusMap[status] || { label: status, color: '#666' };
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
    <div className="my-returns-page">
      <div className="container">
        <div className="returns-header">
          <h1>My Returns & Exchanges</h1>
          <p>Track your return and exchange requests</p>
        </div>

        {loading ? (
          <div className="loading">Loading return requests...</div>
        ) : returns.length === 0 ? (
          <div className="empty-returns">
            <FiRefreshCw className="empty-icon" />
            <h2>No return requests</h2>
            <p>You haven't made any return or exchange requests yet.</p>
            <Link to="/orders" className="btn btn-primary">
              View Orders
            </Link>
          </div>
        ) : (
          <div className="returns-list">
            {returns.map((returnRequest) => {
              const statusInfo = getStatusInfo(returnRequest.status);
              const StatusIcon = statusInfo.icon;
              const refundInfo = returnRequest.refundStatus
                ? getRefundStatusInfo(returnRequest.refundStatus)
                : null;

              return (
                <div key={returnRequest.id} className="return-card">
                  <div className="return-card-header">
                    <div className="return-info">
                      <span className="return-id">Request #{returnRequest.id}</span>
                      <span className="return-date">{formatDate(returnRequest.createdAt)}</span>
                    </div>
                    <div className="return-badges">
                      <span
                        className="return-type"
                        style={{
                          backgroundColor:
                            returnRequest.requestType === 'RETURN' ? '#e6f7ff' : '#f6ffed',
                          color: returnRequest.requestType === 'RETURN' ? '#1890ff' : '#52c41a',
                        }}
                      >
                        {returnRequest.requestType}
                      </span>
                      <span
                        className="return-status"
                        style={{ backgroundColor: statusInfo.color }}
                      >
                        <StatusIcon /> {statusInfo.label}
                      </span>
                    </div>
                  </div>

                  <div className="return-card-body">
                    {returnRequest.orderItem && (
                      <div className="return-item">
                        <img
                          src={returnRequest.orderItem.productImage || '/placeholder.jpg'}
                          alt={returnRequest.orderItem.productName}
                        />
                        <div className="item-details">
                          <h3>{returnRequest.orderItem.productName}</h3>
                          <p className="item-variant">
                            Size: {returnRequest.orderItem.size} • Color:{' '}
                            {returnRequest.orderItem.color}
                          </p>
                          <p className="item-quantity">
                            Quantity: {returnRequest.orderItem.quantity}
                          </p>
                        </div>
                      </div>
                    )}

                    <div className="return-details">
                      <div className="detail-row">
                        <span className="label">Order Number:</span>
                        <Link to={`/orders/${returnRequest.orderId}`} className="value link">
                          #{returnRequest.orderNumber}
                        </Link>
                      </div>
                      <div className="detail-row">
                        <span className="label">Reason:</span>
                        <span className="value">{returnRequest.reason}</span>
                      </div>
                      {returnRequest.requestType === 'RETURN' && returnRequest.refundAmount && (
                        <div className="detail-row">
                          <span className="label">Refund Amount:</span>
                          <span className="value">
                            ₹{returnRequest.refundAmount?.toLocaleString()}
                          </span>
                        </div>
                      )}
                      {refundInfo && (
                        <div className="detail-row">
                          <span className="label">Refund Status:</span>
                          <span className="value" style={{ color: refundInfo.color }}>
                            {refundInfo.label}
                          </span>
                        </div>
                      )}
                      {returnRequest.requestType === 'EXCHANGE' && (returnRequest.exchangeProductName || returnRequest.exchangeVariantInfo) && (
                        <div className="detail-row">
                          <span className="label">Exchange To:</span>
                          <span className="value">
                            {returnRequest.exchangeProductName}
                            {returnRequest.exchangeVariantInfo && ` (${returnRequest.exchangeVariantInfo})`}
                          </span>
                        </div>
                      )}
                      {returnRequest.adminNotes && (
                        <div className="detail-row admin-notes">
                          <span className="label">Admin Notes:</span>
                          <span className="value">{returnRequest.adminNotes}</span>
                        </div>
                      )}
                    </div>
                  </div>

                  <div className="return-card-footer">
                    <div className="return-timeline">
                      <span className="timeline-label">Last Updated:</span>
                      <span className="timeline-date">
                        {formatDate(returnRequest.updatedAt || returnRequest.createdAt)}
                      </span>
                    </div>
                    {returnRequest.status === 'PENDING' && (
                      <button
                        className="btn btn-outline-sm cancel-return-btn"
                        onClick={() => handleCancel(returnRequest.id)}
                        disabled={cancellingId === returnRequest.id}
                      >
                        {cancellingId === returnRequest.id ? 'Cancelling...' : 'Cancel Request'}
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};

export default MyReturns;
