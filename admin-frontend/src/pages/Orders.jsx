import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { orderAPI } from '../services/api';
import toast from 'react-hot-toast';
import { FiDownload } from 'react-icons/fi';
import Pagination from '../components/common/Pagination';
import { getOrderStatusColor } from '../utils/statusColors';
import './Orders.css';

const Orders = () => {
  const navigate = useNavigate();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [sortBy, setSortBy] = useState('createdAt');
  const [sortDir, setSortDir] = useState('desc');
  const [statusFilter, setStatusFilter] = useState('');
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [minAmount, setMinAmount] = useState('');
  const [maxAmount, setMaxAmount] = useState('');
  const [showFilters, setShowFilters] = useState(false);
  const [exporting, setExporting] = useState(null);

  useEffect(() => {
    fetchOrders();
  }, [page, sortBy, sortDir, statusFilter, dateFrom, dateTo, minAmount, maxAmount]);

  const fetchOrders = async () => {
    try {
      setLoading(true);
      const params = { page, size: 20, sort: `${sortBy},${sortDir}` };
      if (statusFilter) params.status = statusFilter;
      if (dateFrom) params.dateFrom = dateFrom;
      if (dateTo) params.dateTo = dateTo;
      if (minAmount) params.minAmount = minAmount;
      if (maxAmount) params.maxAmount = maxAmount;
      const response = await orderAPI.getAll(params);
      const data = response.data.data;
      setOrders(data.content || []);
      setTotalPages(data.totalPages || 0);
    } catch (error) {
      toast.error('Failed to load orders');
    } finally {
      setLoading(false);
    }
  };

  const clearFilters = () => {
    setStatusFilter('');
    setDateFrom('');
    setDateTo('');
    setMinAmount('');
    setMaxAmount('');
    setPage(0);
  };

  const hasActiveFilters = statusFilter || dateFrom || dateTo || minAmount || maxAmount;

  const handleSort = (field) => {
    if (sortBy === field) {
      setSortDir(sortDir === 'asc' ? 'desc' : 'asc');
    } else {
      setSortBy(field);
      setSortDir('desc');
    }
    setPage(0);
  };

  const getSortIndicator = (field) => {
    if (sortBy !== field) return '';
    return sortDir === 'asc' ? ' \u25B2' : ' \u25BC';
  };

  const handleStatusUpdate = async (orderId, newStatus) => {
    try {
      await orderAPI.updateStatus(orderId, newStatus);
      toast.success('Order status updated');
      fetchOrders();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to update order status');
    }
  };

  const handleExport = async (format) => {
    try {
      setExporting(format);
      const params = {};
      if (statusFilter) params.status = statusFilter;
      if (dateFrom) params.dateFrom = dateFrom;
      if (dateTo) params.dateTo = dateTo;
      if (minAmount) params.minAmount = minAmount;
      if (maxAmount) params.maxAmount = maxAmount;

      const response = await orderAPI.exportOrders(format, params);

      const disposition = response.headers['content-disposition'];
      let filename = `orders_export.${format}`;
      if (disposition) {
        const match = disposition.match(/filename="?(.+?)"?$/);
        if (match) filename = match[1];
      }

      const blob = new Blob([response.data]);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);

      toast.success(`${format.toUpperCase()} export downloaded`);
    } catch (error) {
      toast.error(`Failed to export as ${format.toUpperCase()}`);
    } finally {
      setExporting(null);
    }
  };

  const getStatusColor = getOrderStatusColor;

  return (
    <div className="orders-page">
      <div className="page-header">
        <div>
          <h1>Orders</h1>
          <p>Manage customer orders</p>
        </div>
      </div>

      <div className="orders-toolbar" style={{ display: 'flex', gap: '12px', marginBottom: '16px', alignItems: 'center', flexWrap: 'wrap' }}>
        <select
          value={statusFilter}
          onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
          className="status-filter"
          style={{ padding: '8px 12px', border: '2px solid #e0e0e0', borderRadius: '8px', fontSize: '14px' }}
        >
          <option value="">All Statuses</option>
          <option value="PENDING">Pending</option>
          <option value="CONFIRMED">Confirmed</option>
          <option value="PROCESSING">Processing</option>
          <option value="SHIPPED">Shipped</option>
          <option value="DELIVERED">Delivered</option>
          <option value="CANCELLED">Cancelled</option>
          <option value="RETURNED">Returned</option>
        </select>
        <select
          value={`${sortBy},${sortDir}`}
          onChange={(e) => {
            const [field, dir] = e.target.value.split(',');
            setSortBy(field);
            setSortDir(dir);
            setPage(0);
          }}
          style={{ padding: '8px 12px', border: '2px solid #e0e0e0', borderRadius: '8px', fontSize: '14px' }}
        >
          <option value="createdAt,desc">Newest First</option>
          <option value="createdAt,asc">Oldest First</option>
          <option value="totalAmount,desc">Highest Amount</option>
          <option value="totalAmount,asc">Lowest Amount</option>
        </select>
        <button
          onClick={() => setShowFilters(!showFilters)}
          style={{
            padding: '8px 16px', border: '2px solid #e0e0e0', borderRadius: '8px', fontSize: '14px',
            background: hasActiveFilters ? '#667eea' : '#fff',
            color: hasActiveFilters ? '#fff' : '#333', cursor: 'pointer',
          }}
        >
          Filters {hasActiveFilters && '(Active)'}
        </button>
        {hasActiveFilters && (
          <button
            onClick={clearFilters}
            style={{ padding: '8px 12px', border: 'none', background: 'none', color: '#f56565', cursor: 'pointer', fontSize: '14px' }}
          >
            Clear All
          </button>
        )}
        <div style={{ marginLeft: 'auto', display: 'flex', gap: '8px' }}>
          <button
            onClick={() => handleExport('csv')}
            disabled={exporting !== null}
            style={{
              padding: '8px 14px', border: '2px solid #e0e0e0', borderRadius: '8px',
              fontSize: '13px', cursor: 'pointer', background: '#fff', color: '#333',
              display: 'flex', alignItems: 'center', gap: '6px', opacity: exporting ? 0.6 : 1,
            }}
          >
            <FiDownload size={14} />
            {exporting === 'csv' ? 'Exporting...' : 'CSV'}
          </button>
          <button
            onClick={() => handleExport('xlsx')}
            disabled={exporting !== null}
            style={{
              padding: '8px 14px', border: '2px solid #e0e0e0', borderRadius: '8px',
              fontSize: '13px', cursor: 'pointer', background: '#fff', color: '#333',
              display: 'flex', alignItems: 'center', gap: '6px', opacity: exporting ? 0.6 : 1,
            }}
          >
            <FiDownload size={14} />
            {exporting === 'xlsx' ? 'Exporting...' : 'Excel'}
          </button>
          <button
            onClick={() => handleExport('pdf')}
            disabled={exporting !== null}
            style={{
              padding: '8px 14px', border: '2px solid #e0e0e0', borderRadius: '8px',
              fontSize: '13px', cursor: 'pointer', background: '#fff', color: '#333',
              display: 'flex', alignItems: 'center', gap: '6px', opacity: exporting ? 0.6 : 1,
            }}
          >
            <FiDownload size={14} />
            {exporting === 'pdf' ? 'Exporting...' : 'PDF'}
          </button>
        </div>
      </div>

      {showFilters && (
        <div className="advanced-filters" style={{
          display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
          gap: '12px', padding: '16px', background: '#f8f9fa', borderRadius: '8px', marginBottom: '16px',
        }}>
          <div>
            <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px', color: '#555' }}>Date From</label>
            <input
              type="date"
              value={dateFrom}
              onChange={(e) => { setDateFrom(e.target.value); setPage(0); }}
              style={{ width: '100%', padding: '8px 12px', border: '2px solid #e0e0e0', borderRadius: '8px', fontSize: '14px' }}
            />
          </div>
          <div>
            <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px', color: '#555' }}>Date To</label>
            <input
              type="date"
              value={dateTo}
              onChange={(e) => { setDateTo(e.target.value); setPage(0); }}
              style={{ width: '100%', padding: '8px 12px', border: '2px solid #e0e0e0', borderRadius: '8px', fontSize: '14px' }}
            />
          </div>
          <div>
            <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px', color: '#555' }}>Min Amount</label>
            <input
              type="number"
              placeholder="e.g. 500"
              value={minAmount}
              onChange={(e) => { setMinAmount(e.target.value); setPage(0); }}
              style={{ width: '100%', padding: '8px 12px', border: '2px solid #e0e0e0', borderRadius: '8px', fontSize: '14px' }}
            />
          </div>
          <div>
            <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px', color: '#555' }}>Max Amount</label>
            <input
              type="number"
              placeholder="e.g. 10000"
              value={maxAmount}
              onChange={(e) => { setMaxAmount(e.target.value); setPage(0); }}
              style={{ width: '100%', padding: '8px 12px', border: '2px solid #e0e0e0', borderRadius: '8px', fontSize: '14px' }}
            />
          </div>
        </div>
      )}

      {loading ? (
        <div className="loading">Loading orders...</div>
      ) : (
        <>
          <div className="orders-table-container">
            <table className="orders-table">
              <thead>
                <tr>
                  <th style={{ cursor: 'pointer' }} onClick={() => handleSort('orderNumber')}>
                    Order #{getSortIndicator('orderNumber')}
                  </th>
                  <th>Customer</th>
                  <th>Items</th>
                  <th style={{ cursor: 'pointer' }} onClick={() => handleSort('totalAmount')}>
                    Total{getSortIndicator('totalAmount')}
                  </th>
                  <th style={{ cursor: 'pointer' }} onClick={() => handleSort('status')}>
                    Status{getSortIndicator('status')}
                  </th>
                  <th style={{ cursor: 'pointer' }} onClick={() => handleSort('createdAt')}>
                    Date{getSortIndicator('createdAt')}
                  </th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {orders.length === 0 ? (
                  <tr>
                    <td colSpan="7" className="empty-state">
                      No orders found
                    </td>
                  </tr>
                ) : (
                  orders.map((order) => (
                    <tr key={order.id}>
                      <td className="order-number">{order.orderNumber}</td>
                      <td>
                        <div>
                          <div>{order.shippingAddress?.fullName}</div>
                          <div className="text-muted">{order.shippingAddress?.phoneNumber}</div>
                        </div>
                      </td>
                      <td>{order.items?.length || 0} item(s)</td>
                      <td>₹{order.totalAmount?.toLocaleString()}</td>
                      <td>
                        <span
                          className="order-status-badge"
                          style={{
                            backgroundColor: `${getStatusColor(order.status)}18`,
                            color: getStatusColor(order.status),
                            border: `1.5px solid ${getStatusColor(order.status)}40`,
                          }}
                        >
                          {order.status}
                        </span>
                      </td>
                      <td>
                        {new Date(order.createdAt).toLocaleDateString()}
                      </td>
                      <td>
                        <button
                          className="btn-link"
                          onClick={() => navigate(`/admin/orders/${order.id}`)}
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
    </div>
  );
};

export default Orders;

