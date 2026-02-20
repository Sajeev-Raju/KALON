import { useState, useEffect } from 'react';
import { returnAPI } from '../services/api';
import toast from 'react-hot-toast';
import Pagination from '../components/common/Pagination';
import { getReturnStatusColor } from '../utils/statusColors';
import './Returns.css';

const Returns = () => {
  const [returns, setReturns] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [selectedReturn, setSelectedReturn] = useState(null);
  const [showModal, setShowModal] = useState(false);
  const [adminNotes, setAdminNotes] = useState('');
  const [processing, setProcessing] = useState(false);
  const [statusFilter, setStatusFilter] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');

  // Debounce search input — waits 400ms after user stops typing
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(searchTerm);
    }, 400);
    return () => clearTimeout(timer);
  }, [searchTerm]);

  useEffect(() => {
    fetchReturns();
  }, [page, statusFilter, typeFilter, debouncedSearch]);

  useEffect(() => {
    setPage(0);
  }, [debouncedSearch]);

  const fetchReturns = async () => {
    try {
      setLoading(true);
      const params = { page, size: 20 };
      if (debouncedSearch.trim()) {
        params.search = debouncedSearch.trim();
      } else {
        if (statusFilter) params.status = statusFilter;
        if (typeFilter) params.type = typeFilter;
      }
      const response = await returnAPI.getAll(params);
      const data = response.data.data;
      setReturns(data.content || []);
      setTotalPages(data.totalPages || 0);
    } catch (error) {
      toast.error('Failed to load return requests');
    } finally {
      setLoading(false);
    }
  };

  const handleViewDetails = async (returnRequest) => {
    try {
      const response = await returnAPI.getById(returnRequest.id);
      setSelectedReturn(response.data.data);
      setAdminNotes('');
      setShowModal(true);
    } catch (error) {
      toast.error('Failed to load return details');
    }
  };

  const handleApprove = async () => {
    if (!selectedReturn) return;

    try {
      setProcessing(true);
      await returnAPI.approve(selectedReturn.id, adminNotes);
      toast.success('Return request approved');
      setShowModal(false);
      fetchReturns();
    } catch (error) {
      toast.error('Failed to approve return request');
    } finally {
      setProcessing(false);
    }
  };

  const handleReject = async () => {
    if (!selectedReturn) return;

    try {
      setProcessing(true);
      await returnAPI.reject(selectedReturn.id, adminNotes);
      toast.success('Return request rejected');
      setShowModal(false);
      fetchReturns();
    } catch (error) {
      toast.error('Failed to reject return request');
    } finally {
      setProcessing(false);
    }
  };

  const handleProcess = async () => {
    if (!selectedReturn) return;

    try {
      setProcessing(true);
      await returnAPI.process(selectedReturn.id);
      toast.success('Return request is now being processed');
      setShowModal(false);
      fetchReturns();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to process return request');
    } finally {
      setProcessing(false);
    }
  };

  const handleComplete = async () => {
    if (!selectedReturn) return;

    try {
      setProcessing(true);
      await returnAPI.complete(selectedReturn.id);
      toast.success('Return request completed');
      setShowModal(false);
      fetchReturns();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to complete return request');
    } finally {
      setProcessing(false);
    }
  };

  const getStatusColor = getReturnStatusColor;

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('en-IN', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    });
  };

  return (
    <div className="returns-page">
      <div className="page-header">
        <div>
          <h1>Return Requests</h1>
          <p>Manage customer return and exchange requests</p>
        </div>
      </div>

      <div className="returns-toolbar" style={{ display: 'flex', gap: '12px', marginBottom: '16px', alignItems: 'center', flexWrap: 'wrap' }}>
        <input
          type="text"
          placeholder="Search by order # or product..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          style={{ padding: '8px 12px', border: '2px solid #e0e0e0', borderRadius: '8px', fontSize: '14px', minWidth: '220px' }}
        />
        <select
          value={statusFilter}
          onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
          style={{ padding: '8px 12px', border: '2px solid #e0e0e0', borderRadius: '8px', fontSize: '14px' }}
        >
          <option value="">All Statuses</option>
          <option value="PENDING">Pending</option>
          <option value="APPROVED">Approved</option>
          <option value="REJECTED">Rejected</option>
          <option value="PROCESSING">Processing</option>
          <option value="COMPLETED">Completed</option>
        </select>
        <select
          value={typeFilter}
          onChange={(e) => { setTypeFilter(e.target.value); setPage(0); }}
          style={{ padding: '8px 12px', border: '2px solid #e0e0e0', borderRadius: '8px', fontSize: '14px' }}
        >
          <option value="">All Types</option>
          <option value="RETURN">Return</option>
          <option value="EXCHANGE">Exchange</option>
        </select>
      </div>

      {loading ? (
        <div className="loading">Loading return requests...</div>
      ) : (
        <>
          <div className="returns-table-container">
            <table className="returns-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Order #</th>
                  <th>Product</th>
                  <th>Type</th>
                  <th>Reason</th>
                  <th>Status</th>
                  <th>Date</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {returns.length === 0 ? (
                  <tr>
                    <td colSpan="8" className="empty-state">
                      No return requests found
                    </td>
                  </tr>
                ) : (
                  returns.map((returnRequest) => (
                    <tr key={returnRequest.id}>
                      <td className="return-id">#{returnRequest.id}</td>
                      <td className="order-number">{returnRequest.orderNumber}</td>
                      <td className="product-info">
                        {returnRequest.orderItem && (
                          <div className="product-cell">
                            <img
                              src={returnRequest.orderItem.productImage || '/placeholder.jpg'}
                              alt={returnRequest.orderItem.productName}
                            />
                            <div>
                              <div className="product-name">
                                {returnRequest.orderItem.productName}
                              </div>
                              <div className="product-variant">
                                {returnRequest.orderItem.size} / {returnRequest.orderItem.color}
                              </div>
                            </div>
                          </div>
                        )}
                      </td>
                      <td>
                        <span
                          className="type-badge"
                          style={{
                            backgroundColor:
                              returnRequest.requestType === 'RETURN' ? '#e6f7ff' : '#f6ffed',
                            color:
                              returnRequest.requestType === 'RETURN' ? '#1890ff' : '#52c41a',
                          }}
                        >
                          {returnRequest.requestType}
                        </span>
                      </td>
                      <td className="reason-cell">{returnRequest.reason}</td>
                      <td>
                        <span
                          className="status-badge"
                          style={{ backgroundColor: getStatusColor(returnRequest.status) }}
                        >
                          {returnRequest.status}
                        </span>
                      </td>
                      <td>{formatDate(returnRequest.createdAt)}</td>
                      <td>
                        <button
                          className="btn-link"
                          onClick={() => handleViewDetails(returnRequest)}
                        >
                          View
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
        </>
      )}

      {/* Return Detail Modal */}
      {showModal && selectedReturn && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Return Request #{selectedReturn.id}</h2>
              <button className="close-btn" onClick={() => setShowModal(false)}>
                &times;
              </button>
            </div>

            <div className="modal-body">
              <div className="detail-section">
                <h3>Request Information</h3>
                <div className="detail-grid">
                  <div className="detail-item">
                    <span className="label">Order Number:</span>
                    <span className="value">{selectedReturn.orderNumber}</span>
                  </div>
                  <div className="detail-item">
                    <span className="label">Type:</span>
                    <span className="value">{selectedReturn.requestType}</span>
                  </div>
                  <div className="detail-item">
                    <span className="label">Status:</span>
                    <span
                      className="value status-badge"
                      style={{ backgroundColor: getStatusColor(selectedReturn.status) }}
                    >
                      {selectedReturn.status}
                    </span>
                  </div>
                  <div className="detail-item">
                    <span className="label">Created:</span>
                    <span className="value">{formatDate(selectedReturn.createdAt)}</span>
                  </div>
                </div>
              </div>

              {selectedReturn.orderItem && (
                <div className="detail-section">
                  <h3>Product Details</h3>
                  <div className="product-detail">
                    <img
                      src={selectedReturn.orderItem.productImage || '/placeholder.jpg'}
                      alt={selectedReturn.orderItem.productName}
                    />
                    <div>
                      <h4>{selectedReturn.orderItem.productName}</h4>
                      <p>
                        Size: {selectedReturn.orderItem.size} | Color:{' '}
                        {selectedReturn.orderItem.color}
                      </p>
                      <p>Quantity: {selectedReturn.orderItem.quantity}</p>
                      <p>Price: ₹{selectedReturn.orderItem.totalPrice?.toLocaleString()}</p>
                    </div>
                  </div>
                </div>
              )}

              <div className="detail-section">
                <h3>Reason for Return</h3>
                <p className="reason-text">{selectedReturn.reason}</p>
              </div>

              {selectedReturn.requestType === 'RETURN' && selectedReturn.refundAmount && (
                <div className="detail-section">
                  <h3>Refund Information</h3>
                  <div className="detail-grid">
                    <div className="detail-item">
                      <span className="label">Refund Amount:</span>
                      <span className="value">
                        ₹{selectedReturn.refundAmount?.toLocaleString()}
                      </span>
                    </div>
                    <div className="detail-item">
                      <span className="label">Refund Status:</span>
                      <span className="value">{selectedReturn.refundStatus || 'Pending'}</span>
                    </div>
                  </div>
                </div>
              )}

              {selectedReturn.status === 'PENDING' && (
                <div className="detail-section">
                  <h3>Admin Notes</h3>
                  <textarea
                    value={adminNotes}
                    onChange={(e) => setAdminNotes(e.target.value)}
                    placeholder="Add notes (optional)"
                    rows="3"
                  />
                </div>
              )}

              {selectedReturn.adminNotes && (
                <div className="detail-section">
                  <h3>Admin Notes</h3>
                  <p className="admin-notes">{selectedReturn.adminNotes}</p>
                </div>
              )}
            </div>

            <div className="modal-footer">
              {selectedReturn.status === 'PENDING' && (
                <>
                  <button
                    className="btn btn-danger"
                    onClick={handleReject}
                    disabled={processing}
                  >
                    {processing ? 'Processing...' : 'Reject'}
                  </button>
                  <button
                    className="btn btn-success"
                    onClick={handleApprove}
                    disabled={processing}
                  >
                    {processing ? 'Processing...' : 'Approve'}
                  </button>
                </>
              )}
              {selectedReturn.status === 'APPROVED' && (
                <button
                  className="btn btn-primary"
                  onClick={handleProcess}
                  disabled={processing}
                >
                  {processing ? 'Processing...' : 'Process (Initiate Refund/Exchange)'}
                </button>
              )}
              {selectedReturn.status === 'PROCESSING' && (
                <button
                  className="btn btn-primary"
                  onClick={handleComplete}
                  disabled={processing}
                >
                  {processing ? 'Processing...' : 'Mark as Completed'}
                </button>
              )}
              <button className="btn btn-secondary" onClick={() => setShowModal(false)}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Returns;
