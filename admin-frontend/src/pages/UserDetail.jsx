import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { userAPI } from '../services/api';
import { FiArrowLeft, FiUser, FiMail, FiPhone, FiShoppingBag } from 'react-icons/fi';
import toast from 'react-hot-toast';
import './UserDetail.css';

const UserDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchUser();
  }, [id]);

  const fetchUser = async () => {
    try {
      setLoading(true);
      const response = await userAPI.getById(id);
      setUser(response.data.data);
    } catch (error) {
      toast.error('Failed to load user details');
      navigate('/admin/users');
    } finally {
      setLoading(false);
    }
  };

  const handleToggleStatus = async () => {
    try {
      await userAPI.toggleStatus(id);
      toast.success('User status updated');
      fetchUser();
    } catch (error) {
      toast.error('Failed to update user status');
    }
  };

  if (loading) {
    return <div className="user-detail-loading">Loading user details...</div>;
  }

  if (!user) {
    return <div className="user-detail-error">User not found</div>;
  }

  return (
    <div className="user-detail-page">
      <div className="user-detail-header">
        <button className="back-button" onClick={() => navigate('/admin/users')}>
          <FiArrowLeft /> Back to Users
        </button>
        <div className="header-actions">
          <h1>User Details</h1>
          <button
            className={`status-toggle ${user.isActive ? 'active' : 'inactive'}`}
            onClick={handleToggleStatus}
          >
            {user.isActive ? 'Deactivate User' : 'Activate User'}
          </button>
        </div>
      </div>

      <div className="user-detail-content">
        <div className="user-info-grid">
          {/* User Information */}
          <div className="info-card">
            <h3>
              <FiUser /> User Information
            </h3>
            <div className="info-row">
              <span className="label">User ID:</span>
              <span className="value">{user.id}</span>
            </div>
            <div className="info-row">
              <span className="label">Full Name:</span>
              <span className="value">
                {user.firstName} {user.lastName}
              </span>
            </div>
            <div className="info-row">
              <span className="label">Email:</span>
              <span className="value">{user.email}</span>
            </div>
            {user.phoneNumber && (
              <div className="info-row">
                <span className="label">Phone:</span>
                <span className="value">{user.phoneNumber}</span>
              </div>
            )}
            <div className="info-row">
              <span className="label">Role:</span>
              <span className={`role-badge ${user.role?.toLowerCase()}`}>
                {user.role}
              </span>
            </div>
            <div className="info-row">
              <span className="label">Status:</span>
              <span
                className={`status-badge ${user.isActive ? 'active' : 'inactive'}`}
              >
                {user.isActive ? 'Active' : 'Inactive'}
              </span>
            </div>
            <div className="info-row">
              <span className="label">Member Since:</span>
              <span className="value">
                {new Date(user.createdAt).toLocaleDateString()}
              </span>
            </div>
            {user.updatedAt && (
              <div className="info-row">
                <span className="label">Last Updated:</span>
                <span className="value">
                  {new Date(user.updatedAt).toLocaleDateString()}
                </span>
              </div>
            )}
          </div>

          {/* Order Statistics */}
          <div className="info-card">
            <h3>
              <FiShoppingBag /> Order Statistics
            </h3>
            <div className="stat-row">
              <div className="stat-item">
                <span className="stat-label">Total Orders</span>
                <span className="stat-value">{user.orderCount || 0}</span>
              </div>
              <div className="stat-item">
                <span className="stat-label">Total Spent</span>
                <span className="stat-value">
                  ₹{(user.totalSpent || 0).toLocaleString()}
                </span>
              </div>
              {user.orderCount > 0 && (
                <div className="stat-item">
                  <span className="stat-label">Average Order</span>
                  <span className="stat-value">
                    ₹{Math.round((user.totalSpent || 0) / user.orderCount).toLocaleString()}
                  </span>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default UserDetail;



