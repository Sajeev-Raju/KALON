import { useState, useEffect } from 'react';
import { activityLogAPI } from '../services/api';
import toast from 'react-hot-toast';
import Pagination from '../components/common/Pagination';
import './ActivityLogs.css';

const ACTION_COLORS = {
  CREATE: '#48bb78',
  UPDATE: '#4299e1',
  DELETE: '#f56565',
  STATUS_CHANGE: '#ed8936',
  APPROVE: '#38b2ac',
  REJECT: '#e53e3e',
  PROCESS: '#667eea',
  COMPLETE: '#38a169',
  UPLOAD: '#9f7aea',
  LOGIN: '#718096',
};

const RESOURCE_LABELS = {
  PRODUCT: 'Product',
  CATEGORY: 'Category',
  ORDER: 'Order',
  USER: 'User',
  RETURN_REQUEST: 'Return',
  REVIEW: 'Review',
  SITE_CONFIG: 'Config',
  FILE: 'File',
  AUTH: 'Auth',
};

const ActivityLogs = () => {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [actionFilter, setActionFilter] = useState('');
  const [resourceTypeFilter, setResourceTypeFilter] = useState('');
  const [adminFilter, setAdminFilter] = useState('');
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [admins, setAdmins] = useState([]);

  // Debounce search input
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(searchTerm);
    }, 400);
    return () => clearTimeout(timer);
  }, [searchTerm]);

  useEffect(() => {
    fetchAdmins();
  }, []);

  useEffect(() => {
    fetchLogs();
  }, [page, actionFilter, resourceTypeFilter, adminFilter, dateFrom, dateTo, debouncedSearch]);

  useEffect(() => {
    setPage(0);
  }, [actionFilter, resourceTypeFilter, adminFilter, dateFrom, dateTo, debouncedSearch]);

  const fetchAdmins = async () => {
    try {
      const response = await activityLogAPI.getAdmins();
      setAdmins(response.data.data || []);
    } catch (error) {
      // silently fail — admin list is optional
    }
  };

  const fetchLogs = async () => {
    try {
      setLoading(true);
      const params = { page, size: 20 };
      if (debouncedSearch.trim()) {
        params.search = debouncedSearch.trim();
      } else {
        if (actionFilter) params.action = actionFilter;
        if (resourceTypeFilter) params.resourceType = resourceTypeFilter;
        if (adminFilter) params.adminId = adminFilter;
        if (dateFrom) params.dateFrom = dateFrom;
        if (dateTo) params.dateTo = dateTo;
      }
      const response = await activityLogAPI.getAll(params);
      const data = response.data.data;
      setLogs(data.content || []);
      setTotalPages(data.totalPages || 0);
    } catch (error) {
      toast.error('Failed to load activity logs');
    } finally {
      setLoading(false);
    }
  };

  const clearFilters = () => {
    setActionFilter('');
    setResourceTypeFilter('');
    setAdminFilter('');
    setDateFrom('');
    setDateTo('');
    setSearchTerm('');
    setPage(0);
  };

  const hasActiveFilters = actionFilter || resourceTypeFilter || adminFilter || dateFrom || dateTo || searchTerm;

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleString('en-IN', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <div className="activity-logs-page">
      <div className="page-header">
        <div>
          <h1>Activity Logs</h1>
          <p>Track all admin actions</p>
        </div>
      </div>

      <div className="logs-toolbar" style={{ display: 'flex', gap: '12px', marginBottom: '16px', alignItems: 'center', flexWrap: 'wrap' }}>
        <input
          type="text"
          placeholder="Search by name or details..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          style={{ padding: '8px 12px', border: '2px solid #e0e0e0', borderRadius: '8px', fontSize: '14px', minWidth: '220px' }}
        />
        <select
          value={adminFilter}
          onChange={(e) => setAdminFilter(e.target.value)}
          style={{ padding: '8px 12px', border: '2px solid #e0e0e0', borderRadius: '8px', fontSize: '14px' }}
        >
          <option value="">All Admins</option>
          {admins.map((admin) => (
            <option key={admin.id} value={admin.id}>{admin.name} ({admin.email})</option>
          ))}
        </select>
        <select
          value={actionFilter}
          onChange={(e) => setActionFilter(e.target.value)}
          style={{ padding: '8px 12px', border: '2px solid #e0e0e0', borderRadius: '8px', fontSize: '14px' }}
        >
          <option value="">All Actions</option>
          <option value="CREATE">Create</option>
          <option value="UPDATE">Update</option>
          <option value="DELETE">Delete</option>
          <option value="STATUS_CHANGE">Status Change</option>
          <option value="APPROVE">Approve</option>
          <option value="REJECT">Reject</option>
          <option value="PROCESS">Process</option>
          <option value="COMPLETE">Complete</option>
          <option value="UPLOAD">Upload</option>
          <option value="LOGIN">Login</option>
        </select>
        <select
          value={resourceTypeFilter}
          onChange={(e) => setResourceTypeFilter(e.target.value)}
          style={{ padding: '8px 12px', border: '2px solid #e0e0e0', borderRadius: '8px', fontSize: '14px' }}
        >
          <option value="">All Resources</option>
          <option value="PRODUCT">Product</option>
          <option value="CATEGORY">Category</option>
          <option value="ORDER">Order</option>
          <option value="USER">User</option>
          <option value="RETURN_REQUEST">Return Request</option>
          <option value="REVIEW">Review</option>
          <option value="SITE_CONFIG">Site Config</option>
          <option value="FILE">File</option>
        </select>
        <input
          type="date"
          value={dateFrom}
          onChange={(e) => setDateFrom(e.target.value)}
          style={{ padding: '8px 12px', border: '2px solid #e0e0e0', borderRadius: '8px', fontSize: '14px' }}
          title="Date From"
        />
        <input
          type="date"
          value={dateTo}
          onChange={(e) => setDateTo(e.target.value)}
          style={{ padding: '8px 12px', border: '2px solid #e0e0e0', borderRadius: '8px', fontSize: '14px' }}
          title="Date To"
        />
        {hasActiveFilters && (
          <button
            onClick={clearFilters}
            style={{ padding: '8px 12px', border: 'none', background: 'none', color: '#f56565', cursor: 'pointer', fontSize: '14px' }}
          >
            Clear All
          </button>
        )}
      </div>

      {loading ? (
        <div className="loading">Loading activity logs...</div>
      ) : (
        <>
          <div className="logs-table-container">
            <table className="logs-table">
              <thead>
                <tr>
                  <th>Time</th>
                  <th>Admin</th>
                  <th>Action</th>
                  <th>Resource</th>
                  <th>Name / ID</th>
                  <th>Details</th>
                </tr>
              </thead>
              <tbody>
                {logs.length === 0 ? (
                  <tr>
                    <td colSpan="6" className="empty-state">
                      No activity logs found
                    </td>
                  </tr>
                ) : (
                  logs.map((log) => (
                    <tr key={log.id}>
                      <td className="log-time">{formatDate(log.createdAt)}</td>
                      <td>
                        <div className="admin-info">
                          <span className="admin-name">{log.adminName}</span>
                          <span className="admin-email">{log.adminEmail}</span>
                        </div>
                      </td>
                      <td>
                        <span
                          className="action-badge"
                          style={{
                            backgroundColor: `${ACTION_COLORS[log.action] || '#666'}18`,
                            color: ACTION_COLORS[log.action] || '#666',
                            border: `1.5px solid ${ACTION_COLORS[log.action] || '#666'}40`,
                          }}
                        >
                          {log.action.replace('_', ' ')}
                        </span>
                      </td>
                      <td>
                        <span className="resource-badge">
                          {RESOURCE_LABELS[log.resourceType] || log.resourceType}
                        </span>
                      </td>
                      <td className="resource-name">
                        {log.resourceName || (log.resourceId ? `#${log.resourceId}` : '-')}
                      </td>
                      <td className="log-details">{log.details || '-'}</td>
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

export default ActivityLogs;
