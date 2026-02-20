import { useState, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { FiUser, FiMail, FiPhone, FiLock, FiSave, FiEye, FiEyeOff } from 'react-icons/fi';
import { fetchProfile, updateProfile } from '../redux/slices/authSlice';
import { profileAPI } from '../services/api';
import toast from 'react-hot-toast';
import './Profile.css';

const Profile = () => {
  const dispatch = useDispatch();
  const { user, loading } = useSelector((state) => state.auth);

  const [activeTab, setActiveTab] = useState('profile');
  const [profileForm, setProfileForm] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phoneNumber: '',
  });
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });
  const [showPasswords, setShowPasswords] = useState({
    current: false,
    new: false,
    confirm: false,
  });
  const [saving, setSaving] = useState(false);
  const [changingPassword, setChangingPassword] = useState(false);

  useEffect(() => {
    dispatch(fetchProfile());
  }, [dispatch]);

  useEffect(() => {
    if (user) {
      setProfileForm({
        firstName: user.firstName || '',
        lastName: user.lastName || '',
        email: user.email || '',
        phoneNumber: user.phoneNumber || '',
      });
    }
  }, [user]);

  const handleProfileChange = (e) => {
    const { name, value } = e.target;
    setProfileForm((prev) => ({ ...prev, [name]: value }));
  };

  const handlePasswordChange = (e) => {
    const { name, value } = e.target;
    setPasswordForm((prev) => ({ ...prev, [name]: value }));
  };

  const togglePasswordVisibility = (field) => {
    setShowPasswords((prev) => ({ ...prev, [field]: !prev[field] }));
  };

  const handleProfileSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);

    try {
      // Only send editable fields (exclude email as it cannot be changed)
      const { email, ...editableFields } = profileForm;
      await dispatch(updateProfile(editableFields)).unwrap();
      toast.success('Profile updated successfully');
    } catch (error) {
      toast.error(error || 'Failed to update profile');
    } finally {
      setSaving(false);
    }
  };

  const handlePasswordSubmit = async (e) => {
    e.preventDefault();

    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      toast.error('New password and confirm password do not match');
      return;
    }

    if (passwordForm.newPassword.length < 8) {
      toast.error('Password must be at least 8 characters');
      return;
    }

    const hasUpperCase = /[A-Z]/.test(passwordForm.newPassword);
    const hasLowerCase = /[a-z]/.test(passwordForm.newPassword);
    const hasDigit = /\d/.test(passwordForm.newPassword);

    if (!hasUpperCase || !hasLowerCase || !hasDigit) {
      toast.error('Password must contain at least one uppercase letter, one lowercase letter, and one digit');
      return;
    }

    setChangingPassword(true);

    try {
      await profileAPI.changePassword(passwordForm);
      toast.success('Password changed successfully');
      setPasswordForm({
        currentPassword: '',
        newPassword: '',
        confirmPassword: '',
      });
    } catch (error) {
      const errorMessage = error.response?.data?.message ||
                          error.response?.data?.error ||
                          'Failed to change password';
      toast.error(errorMessage);
    } finally {
      setChangingPassword(false);
    }
  };

  if (loading && !user) {
    return <div className="profile-loading">Loading profile...</div>;
  }

  return (
    <div className="profile-page">
      <div className="profile-header">
        <h1>My Profile</h1>
        <p>Manage your account settings and change your password</p>
      </div>

      <div className="profile-content">
        <div className="profile-sidebar">
          <div className="profile-avatar">
            <div className="avatar-circle">
              {user?.firstName?.charAt(0)}{user?.lastName?.charAt(0)}
            </div>
            <h3>{user?.firstName} {user?.lastName}</h3>
            <span className="role-badge">Administrator</span>
          </div>

          <nav className="profile-nav">
            <button
              className={`nav-button ${activeTab === 'profile' ? 'active' : ''}`}
              onClick={() => setActiveTab('profile')}
            >
              <FiUser />
              <span>Profile Information</span>
            </button>
            <button
              className={`nav-button ${activeTab === 'password' ? 'active' : ''}`}
              onClick={() => setActiveTab('password')}
            >
              <FiLock />
              <span>Change Password</span>
            </button>
          </nav>
        </div>

        <div className="profile-main">
          {activeTab === 'profile' && (
            <div className="profile-card">
              <div className="card-header">
                <h2>Profile Information</h2>
                <p>Update your personal information</p>
              </div>

              <form onSubmit={handleProfileSubmit} className="profile-form">
                <div className="form-row">
                  <div className="form-group">
                    <label htmlFor="firstName">
                      <FiUser className="label-icon" />
                      First Name
                    </label>
                    <input
                      type="text"
                      id="firstName"
                      name="firstName"
                      value={profileForm.firstName}
                      onChange={handleProfileChange}
                      placeholder="Enter first name"
                      required
                    />
                  </div>

                  <div className="form-group">
                    <label htmlFor="lastName">
                      <FiUser className="label-icon" />
                      Last Name
                    </label>
                    <input
                      type="text"
                      id="lastName"
                      name="lastName"
                      value={profileForm.lastName}
                      onChange={handleProfileChange}
                      placeholder="Enter last name"
                      required
                    />
                  </div>
                </div>

                <div className="form-group">
                  <label htmlFor="email">
                    <FiMail className="label-icon" />
                    Email Address
                  </label>
                  <input
                    type="email"
                    id="email"
                    name="email"
                    value={profileForm.email}
                    onChange={handleProfileChange}
                    placeholder="Enter email address"
                    required
                    disabled
                  />
                  <span className="input-hint">Email cannot be changed</span>
                </div>

                <div className="form-group">
                  <label htmlFor="phoneNumber">
                    <FiPhone className="label-icon" />
                    Phone Number
                  </label>
                  <input
                    type="tel"
                    id="phoneNumber"
                    name="phoneNumber"
                    value={profileForm.phoneNumber}
                    onChange={handleProfileChange}
                    placeholder="Enter phone number"
                  />
                </div>

                <div className="form-actions">
                  <button type="submit" className="save-button" disabled={saving}>
                    <FiSave />
                    {saving ? 'Saving...' : 'Save Changes'}
                  </button>
                </div>
              </form>
            </div>
          )}

          {activeTab === 'password' && (
            <div className="profile-card">
              <div className="card-header">
                <h2>Change Password</h2>
                <p>Ensure your account is using a strong password</p>
              </div>

              <form onSubmit={handlePasswordSubmit} className="profile-form">
                <div className="form-group">
                  <label htmlFor="currentPassword">
                    <FiLock className="label-icon" />
                    Current Password
                  </label>
                  <div className="password-input">
                    <input
                      type={showPasswords.current ? 'text' : 'password'}
                      id="currentPassword"
                      name="currentPassword"
                      value={passwordForm.currentPassword}
                      onChange={handlePasswordChange}
                      placeholder="Enter current password"
                      required
                    />
                    <button
                      type="button"
                      className="toggle-password"
                      onClick={() => togglePasswordVisibility('current')}
                    >
                      {showPasswords.current ? <FiEyeOff /> : <FiEye />}
                    </button>
                  </div>
                </div>

                <div className="form-group">
                  <label htmlFor="newPassword">
                    <FiLock className="label-icon" />
                    New Password
                  </label>
                  <div className="password-input">
                    <input
                      type={showPasswords.new ? 'text' : 'password'}
                      id="newPassword"
                      name="newPassword"
                      value={passwordForm.newPassword}
                      onChange={handlePasswordChange}
                      placeholder="Enter new password"
                      required
                    />
                    <button
                      type="button"
                      className="toggle-password"
                      onClick={() => togglePasswordVisibility('new')}
                    >
                      {showPasswords.new ? <FiEyeOff /> : <FiEye />}
                    </button>
                  </div>
                  <span className="input-hint">
                    Must be at least 8 characters with uppercase, lowercase, and a number
                  </span>
                </div>

                <div className="form-group">
                  <label htmlFor="confirmPassword">
                    <FiLock className="label-icon" />
                    Confirm New Password
                  </label>
                  <div className="password-input">
                    <input
                      type={showPasswords.confirm ? 'text' : 'password'}
                      id="confirmPassword"
                      name="confirmPassword"
                      value={passwordForm.confirmPassword}
                      onChange={handlePasswordChange}
                      placeholder="Confirm new password"
                      required
                    />
                    <button
                      type="button"
                      className="toggle-password"
                      onClick={() => togglePasswordVisibility('confirm')}
                    >
                      {showPasswords.confirm ? <FiEyeOff /> : <FiEye />}
                    </button>
                  </div>
                </div>

                <div className="form-actions">
                  <button type="submit" className="save-button" disabled={changingPassword}>
                    <FiLock />
                    {changingPassword ? 'Changing Password...' : 'Change Password'}
                  </button>
                </div>
              </form>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Profile;
