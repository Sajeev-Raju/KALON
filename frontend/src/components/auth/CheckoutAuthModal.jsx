import { useState, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { FiX } from 'react-icons/fi';
import { login, register, clearError } from '../../redux/slices/authSlice';
import GoogleSignInButton from './GoogleSignInButton';
import toast from 'react-hot-toast';
import './CheckoutAuthModal.css';

const CheckoutAuthModal = ({ isOpen, onClose, onAuthSuccess }) => {
  const [mode, setMode] = useState('register'); // 'login' or 'register'
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    confirmPassword: '',
    phoneNumber: '',
  });

  const dispatch = useDispatch();
  const { isAuthenticated, loading, error } = useSelector((state) => state.auth);

  useEffect(() => {
    if (isAuthenticated && isOpen) {
      onAuthSuccess();
    }
  }, [isAuthenticated, isOpen, onAuthSuccess]);

  useEffect(() => {
    if (error) {
      toast.error(error);
      dispatch(clearError());
    }
  }, [error, dispatch]);

  // Prevent body scroll when modal is open
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }
    return () => {
      document.body.style.overflow = '';
    };
  }, [isOpen]);

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const validatePhone = (phone) => {
    if (!phone) return true; // optional field
    const cleaned = phone.replace(/[\s\-()]/g, '');
    // Indian mobile number: 10 digits starting with 6-9, optionally prefixed with +91
    return /^(\+91)?[6-9]\d{9}$/.test(cleaned);
  };

  const validatePasswordStrength = (password) => {
    if (password.length < 8) return 'Password must be at least 8 characters';
    if (!/[A-Z]/.test(password)) return 'Password must contain at least one uppercase letter';
    if (!/[a-z]/.test(password)) return 'Password must contain at least one lowercase letter';
    if (!/\d/.test(password)) return 'Password must contain at least one number';
    return null;
  };

  const handleSubmit = (e) => {
    e.preventDefault();

    if (mode === 'login') {
      if (!formData.email || !formData.password) {
        toast.error('Please fill in all fields');
        return;
      }
      dispatch(login({ email: formData.email, password: formData.password }));
    } else {
      if (!formData.firstName || !formData.lastName || !formData.email || !formData.password) {
        toast.error('Please fill in all required fields');
        return;
      }
      if (formData.password !== formData.confirmPassword) {
        toast.error('Passwords do not match');
        return;
      }
      const passwordError = validatePasswordStrength(formData.password);
      if (passwordError) {
        toast.error(passwordError);
        return;
      }
      if (!validatePhone(formData.phoneNumber)) {
        toast.error('Please enter a valid 10-digit Indian mobile number');
        return;
      }
      dispatch(register({
        firstName: formData.firstName,
        lastName: formData.lastName,
        email: formData.email,
        password: formData.password,
        phoneNumber: formData.phoneNumber,
      }));
    }
  };

  const switchMode = () => {
    setMode(mode === 'login' ? 'register' : 'login');
    setFormData({
      firstName: '',
      lastName: '',
      email: formData.email,
      password: '',
      confirmPassword: '',
      phoneNumber: '',
    });
  };

  if (!isOpen) return null;

  return (
    <div className="checkout-auth-overlay" onClick={onClose}>
      <div className="checkout-auth-modal" onClick={(e) => e.stopPropagation()}>
        <button className="modal-close-btn" onClick={onClose}>
          <FiX />
        </button>

        <div className="modal-header">
          <h2>{mode === 'login' ? 'Welcome Back' : 'Create Account'}</h2>
          <p>
            {mode === 'login'
              ? 'Login to complete your purchase'
              : 'Quick registration to complete your purchase'}
          </p>
        </div>

        <form onSubmit={handleSubmit} className="modal-form">
          {mode === 'register' && (
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">First Name *</label>
                <input
                  type="text"
                  name="firstName"
                  value={formData.firstName}
                  onChange={handleChange}
                  placeholder="First name"
                  className="form-input"
                />
              </div>
              <div className="form-group">
                <label className="form-label">Last Name *</label>
                <input
                  type="text"
                  name="lastName"
                  value={formData.lastName}
                  onChange={handleChange}
                  placeholder="Last name"
                  className="form-input"
                />
              </div>
            </div>
          )}

          <div className="form-group">
            <label className="form-label">Email *</label>
            <input
              type="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              placeholder="Enter your email"
              className="form-input"
            />
          </div>

          {mode === 'register' && (
            <div className="form-group">
              <label className="form-label">Phone Number</label>
              <input
                type="tel"
                name="phoneNumber"
                value={formData.phoneNumber}
                onChange={handleChange}
                placeholder="Enter your phone number"
                className="form-input"
              />
            </div>
          )}

          <div className="form-group">
            <label className="form-label">Password *</label>
            <input
              type="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              placeholder={mode === 'login' ? 'Enter your password' : 'Create a password'}
              className="form-input"
            />
          </div>

          {mode === 'register' && (
            <div className="form-group">
              <label className="form-label">Confirm Password *</label>
              <input
                type="password"
                name="confirmPassword"
                value={formData.confirmPassword}
                onChange={handleChange}
                placeholder="Confirm your password"
                className="form-input"
              />
            </div>
          )}

          <button
            type="submit"
            className="btn btn-primary btn-full btn-lg"
            disabled={loading}
          >
            {loading
              ? (mode === 'login' ? 'Logging in...' : 'Creating Account...')
              : (mode === 'login' ? 'Login & Continue' : 'Register & Continue')}
          </button>
        </form>

        <div className="auth-divider">
          <span>OR</span>
        </div>

        <GoogleSignInButton />

        <p className="modal-footer">
          {mode === 'login' ? (
            <>Don't have an account? <button type="button" className="link-btn" onClick={switchMode}>Create Account</button></>
          ) : (
            <>Already have an account? <button type="button" className="link-btn" onClick={switchMode}>Login</button></>
          )}
        </p>
      </div>
    </div>
  );
};

export default CheckoutAuthModal;
