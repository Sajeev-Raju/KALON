import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useSelector, useDispatch } from 'react-redux';
import { logoutAsync, syncUserProfile } from '../redux/slices/authSlice';
import { userAPI } from '../services/api';
import { FiUser, FiShoppingBag, FiMapPin, FiLock, FiLogOut, FiRefreshCw, FiEdit2, FiStar } from 'react-icons/fi';
import toast from 'react-hot-toast';
import './Account.css';

const Account = () => {
  const { user, isAuthenticated } = useSelector((state) => state.auth);
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('overview');

  // Profile edit state
  const [isEditing, setIsEditing] = useState(false);
  const [profileData, setProfileData] = useState({
    firstName: '',
    lastName: '',
    phoneNumber: '',
  });
  const [savingProfile, setSavingProfile] = useState(false);

  // Password change state
  const [passwordData, setPasswordData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });
  const [changingPassword, setChangingPassword] = useState(false);

  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/login');
    }
  }, [isAuthenticated, navigate]);

  useEffect(() => {
    if (user) {
      setProfileData({
        firstName: user.firstName || '',
        lastName: user.lastName || '',
        phoneNumber: user.phoneNumber || '',
      });
    }
  }, [user]);

  const handleLogout = () => {
    dispatch(logoutAsync());
    navigate('/');
  };

  const handleProfileChange = (e) => {
    const { name, value } = e.target;
    setProfileData((prev) => ({ ...prev, [name]: value }));
  };

  const handleProfileSubmit = async (e) => {
    e.preventDefault();

    if (!profileData.firstName.trim()) {
      toast.error('First name is required');
      return;
    }

    try {
      setSavingProfile(true);
      await userAPI.updateProfile(profileData);
      dispatch(syncUserProfile());
      toast.success('Profile updated successfully');
      setIsEditing(false);
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to update profile');
    } finally {
      setSavingProfile(false);
    }
  };

  const handleCancelEdit = () => {
    setProfileData({
      firstName: user?.firstName || '',
      lastName: user?.lastName || '',
      phoneNumber: user?.phoneNumber || '',
    });
    setIsEditing(false);
  };

  const handlePasswordChange = (e) => {
    const { name, value } = e.target;
    setPasswordData((prev) => ({ ...prev, [name]: value }));
  };

  const handlePasswordSubmit = async (e) => {
    e.preventDefault();

    if (passwordData.newPassword !== passwordData.confirmPassword) {
      toast.error('New passwords do not match');
      return;
    }

    if (passwordData.newPassword.length < 8) {
      toast.error('Password must be at least 8 characters');
      return;
    }
    if (!/[A-Z]/.test(passwordData.newPassword)) {
      toast.error('Password must contain at least one uppercase letter');
      return;
    }
    if (!/[a-z]/.test(passwordData.newPassword)) {
      toast.error('Password must contain at least one lowercase letter');
      return;
    }
    if (!/\d/.test(passwordData.newPassword)) {
      toast.error('Password must contain at least one digit');
      return;
    }

    try {
      setChangingPassword(true);
      await userAPI.changePassword(passwordData);
      toast.success('Password changed successfully');
      setPasswordData({
        currentPassword: '',
        newPassword: '',
        confirmPassword: '',
      });
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to change password');
    } finally {
      setChangingPassword(false);
    }
  };

  if (!isAuthenticated) {
    return null;
  }

  return (
    <div className="account-page">
      <div className="container">
        <div className="account-header">
          <h1>My Account</h1>
          <p>Welcome back, {user?.firstName}!</p>
        </div>

        <div className="account-content">
          <div className="account-sidebar">
            <nav className="account-nav">
              <button
                className={`nav-item ${activeTab === 'overview' ? 'active' : ''}`}
                onClick={() => setActiveTab('overview')}
              >
                <FiUser /> Overview
              </button>
              <Link to="/orders" className="nav-item">
                <FiShoppingBag /> My Orders
              </Link>
              <Link to="/my-returns" className="nav-item">
                <FiRefreshCw /> My Returns
              </Link>
              <Link to="/my-reviews" className="nav-item">
                <FiStar /> My Reviews
              </Link>
              <Link to="/addresses" className="nav-item">
                <FiMapPin /> Addresses
              </Link>
              <button
                className={`nav-item ${activeTab === 'password' ? 'active' : ''}`}
                onClick={() => setActiveTab('password')}
              >
                <FiLock /> Change Password
              </button>
              <button className="nav-item logout" onClick={handleLogout}>
                <FiLogOut /> Logout
              </button>
            </nav>
          </div>

          <div className="account-main">
            {activeTab === 'overview' && (
              <div className="account-section">
                <div className="section-header">
                  <h2>Account Overview</h2>
                  {!isEditing && (
                    <button className="btn btn-outline btn-sm" onClick={() => setIsEditing(true)}>
                      <FiEdit2 /> Edit Profile
                    </button>
                  )}
                </div>

                {isEditing ? (
                  <div className="form-card">
                    <form onSubmit={handleProfileSubmit}>
                      <div className="form-row-inline">
                        <div className="form-group">
                          <label>First Name *</label>
                          <input
                            type="text"
                            name="firstName"
                            value={profileData.firstName}
                            onChange={handleProfileChange}
                            required
                          />
                        </div>
                        <div className="form-group">
                          <label>Last Name</label>
                          <input
                            type="text"
                            name="lastName"
                            value={profileData.lastName}
                            onChange={handleProfileChange}
                          />
                        </div>
                      </div>
                      <div className="form-group">
                        <label>Phone Number</label>
                        <input
                          type="tel"
                          name="phoneNumber"
                          value={profileData.phoneNumber}
                          onChange={handleProfileChange}
                          placeholder="Enter phone number"
                        />
                      </div>
                      <div className="form-group">
                        <label>Email</label>
                        <input
                          type="email"
                          value={user?.email || ''}
                          disabled
                          className="input-disabled"
                        />
                        <span className="form-hint">Email cannot be changed</span>
                      </div>
                      <div className="form-actions-inline">
                        <button type="button" className="btn btn-outline" onClick={handleCancelEdit}>
                          Cancel
                        </button>
                        <button type="submit" className="btn btn-primary" disabled={savingProfile}>
                          {savingProfile ? 'Saving...' : 'Save Changes'}
                        </button>
                      </div>
                    </form>
                  </div>
                ) : (
                  <>
                    <div className="info-card">
                      <div className="info-row">
                        <span className="label">Full Name:</span>
                        <span className="value">{user?.firstName} {user?.lastName}</span>
                      </div>
                      <div className="info-row">
                        <span className="label">Email:</span>
                        <span className="value">{user?.email}</span>
                      </div>
                      {user?.phoneNumber && (
                        <div className="info-row">
                          <span className="label">Phone:</span>
                          <span className="value">{user.phoneNumber}</span>
                        </div>
                      )}
                    </div>
                    <div className="quick-actions">
                      <Link to="/orders" className="action-card">
                        <FiShoppingBag className="action-icon" />
                        <h3>My Orders</h3>
                        <p>View order history</p>
                      </Link>
                      <Link to="/wishlist" className="action-card">
                        <FiUser className="action-icon" />
                        <h3>Wishlist</h3>
                        <p>Saved items</p>
                      </Link>
                      <Link to="/addresses" className="action-card">
                        <FiMapPin className="action-icon" />
                        <h3>Addresses</h3>
                        <p>Manage addresses</p>
                      </Link>
                    </div>
                  </>
                )}
              </div>
            )}

            {activeTab === 'password' && (
              <div className="account-section">
                <h2>Change Password</h2>
                <div className="form-card">
                  <form onSubmit={handlePasswordSubmit}>
                    <div className="form-group">
                      <label>Current Password</label>
                      <input
                        type="password"
                        name="currentPassword"
                        value={passwordData.currentPassword}
                        onChange={handlePasswordChange}
                        required
                      />
                    </div>
                    <div className="form-group">
                      <label>New Password</label>
                      <input
                        type="password"
                        name="newPassword"
                        value={passwordData.newPassword}
                        onChange={handlePasswordChange}
                        minLength={8}
                        required
                      />
                    </div>
                    <div className="form-group">
                      <label>Confirm New Password</label>
                      <input
                        type="password"
                        name="confirmPassword"
                        value={passwordData.confirmPassword}
                        onChange={handlePasswordChange}
                        minLength={8}
                        required
                      />
                    </div>
                    <button type="submit" className="btn btn-primary" disabled={changingPassword}>
                      {changingPassword ? 'Updating...' : 'Update Password'}
                    </button>
                  </form>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Account;
