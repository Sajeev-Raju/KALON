import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { addressAPI } from '../services/api';
import { FiPlus, FiEdit2, FiTrash2, FiMapPin, FiCheck } from 'react-icons/fi';
import toast from 'react-hot-toast';
import './Addresses.css';

const ADDRESS_TYPES = [
  { value: 'HOME', label: 'Home' },
  { value: 'WORK', label: 'Work' },
  { value: 'OTHER', label: 'Other' },
];

const Addresses = () => {
  const navigate = useNavigate();
  const { isAuthenticated } = useSelector((state) => state.auth);
  const [addresses, setAddresses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingAddress, setEditingAddress] = useState(null);
  const [formData, setFormData] = useState({
    fullName: '',
    phoneNumber: '',
    addressLine1: '',
    addressLine2: '',
    city: '',
    state: '',
    postalCode: '',
    isDefault: false,
    addressType: 'HOME',
  });
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    fetchAddresses();
  }, [isAuthenticated, navigate]);

  const fetchAddresses = async () => {
    try {
      setLoading(true);
      const response = await addressAPI.getAll();
      setAddresses(response.data.data || []);
    } catch (error) {
      toast.error('Failed to load addresses');
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }));
  };

  const resetForm = () => {
    setFormData({
      fullName: '',
      phoneNumber: '',
      addressLine1: '',
      addressLine2: '',
      city: '',
      state: '',
      postalCode: '',
      isDefault: false,
      addressType: 'HOME',
    });
    setEditingAddress(null);
    setShowForm(false);
  };

  const handleEdit = (address) => {
    setEditingAddress(address);
    setFormData({
      fullName: address.fullName || '',
      phoneNumber: address.phoneNumber || '',
      addressLine1: address.addressLine1 || '',
      addressLine2: address.addressLine2 || '',
      city: address.city || '',
      state: address.state || '',
      postalCode: address.postalCode || '',
      isDefault: address.isDefault || false,
      addressType: address.addressType || 'HOME',
    });
    setShowForm(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!formData.fullName || !formData.phoneNumber || !formData.addressLine1 ||
        !formData.city || !formData.state || !formData.postalCode) {
      toast.error('Please fill in all required fields');
      return;
    }

    try {
      setSaving(true);
      if (editingAddress) {
        await addressAPI.update(editingAddress.id, formData);
        toast.success('Address updated successfully');
      } else {
        await addressAPI.create(formData);
        toast.success('Address added successfully');
      }
      resetForm();
      fetchAddresses();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to save address');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (addressId) => {
    if (!window.confirm('Are you sure you want to delete this address?')) {
      return;
    }

    try {
      setAddresses((prev) => prev.filter((a) => a.id !== addressId));
      await addressAPI.delete(addressId);
      toast.success('Address deleted successfully');
      fetchAddresses();
    } catch (error) {
      toast.error('Failed to delete address');
      fetchAddresses();
    }
  };

  const getAddressTypeLabel = (type) => {
    const found = ADDRESS_TYPES.find((t) => t.value === type);
    return found ? found.label : type;
  };

  if (!isAuthenticated) {
    return null;
  }

  return (
    <div className="addresses-page">
      <div className="container">
        <div className="addresses-header">
          <div>
            <h1>My Addresses</h1>
            <p>Manage your saved addresses</p>
          </div>
          {!showForm && (
            <button className="btn btn-primary" onClick={() => setShowForm(true)}>
              <FiPlus /> Add New Address
            </button>
          )}
        </div>

        {showForm && (
          <div className="address-form-container">
            <h2>{editingAddress ? 'Edit Address' : 'Add New Address'}</h2>
            <form className="address-form" onSubmit={handleSubmit}>
              <div className="form-row">
                <div className="form-group">
                  <label>Full Name *</label>
                  <input
                    type="text"
                    name="fullName"
                    value={formData.fullName}
                    onChange={handleInputChange}
                    placeholder="Enter full name"
                    required
                  />
                </div>
                <div className="form-group">
                  <label>Phone Number *</label>
                  <input
                    type="tel"
                    name="phoneNumber"
                    value={formData.phoneNumber}
                    onChange={handleInputChange}
                    placeholder="Enter phone number"
                    required
                  />
                </div>
              </div>

              <div className="form-group">
                <label>Address Line 1 *</label>
                <input
                  type="text"
                  name="addressLine1"
                  value={formData.addressLine1}
                  onChange={handleInputChange}
                  placeholder="House no., Building, Street"
                  required
                />
              </div>

              <div className="form-group">
                <label>Address Line 2</label>
                <input
                  type="text"
                  name="addressLine2"
                  value={formData.addressLine2}
                  onChange={handleInputChange}
                  placeholder="Area, Landmark (Optional)"
                />
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>City *</label>
                  <input
                    type="text"
                    name="city"
                    value={formData.city}
                    onChange={handleInputChange}
                    placeholder="Enter city"
                    required
                  />
                </div>
                <div className="form-group">
                  <label>State *</label>
                  <input
                    type="text"
                    name="state"
                    value={formData.state}
                    onChange={handleInputChange}
                    placeholder="Enter state"
                    required
                  />
                </div>
                <div className="form-group">
                  <label>Pincode *</label>
                  <input
                    type="text"
                    name="postalCode"
                    value={formData.postalCode}
                    onChange={handleInputChange}
                    placeholder="Enter pincode"
                    required
                  />
                </div>
              </div>

              <div className="form-group">
                <label>Address Type</label>
                <div className="address-type-options">
                  {ADDRESS_TYPES.map((type) => (
                    <label
                      key={type.value}
                      className={`address-type-option ${formData.addressType === type.value ? 'selected' : ''}`}
                    >
                      <input
                        type="radio"
                        name="addressType"
                        value={type.value}
                        checked={formData.addressType === type.value}
                        onChange={handleInputChange}
                      />
                      {type.label}
                    </label>
                  ))}
                </div>
              </div>

              <div className="form-group checkbox-group">
                <label>
                  <input
                    type="checkbox"
                    name="isDefault"
                    checked={formData.isDefault}
                    onChange={handleInputChange}
                  />
                  Set as default address
                </label>
              </div>

              <div className="form-actions">
                <button
                  type="button"
                  className="btn btn-outline"
                  onClick={resetForm}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={saving}
                >
                  {saving ? 'Saving...' : editingAddress ? 'Update Address' : 'Save Address'}
                </button>
              </div>
            </form>
          </div>
        )}

        {loading ? (
          <div className="loading">Loading addresses...</div>
        ) : addresses.length === 0 && !showForm ? (
          <div className="empty-addresses">
            <FiMapPin className="empty-icon" />
            <h2>No saved addresses</h2>
            <p>Add an address to make checkout faster.</p>
            <button className="btn btn-primary" onClick={() => setShowForm(true)}>
              <FiPlus /> Add Address
            </button>
          </div>
        ) : (
          <div className="addresses-grid">
            {addresses.map((address) => (
              <div
                key={address.id}
                className={`address-card ${address.isDefault ? 'default' : ''}`}
              >
                <div className="address-card-header">
                  {address.addressType && (
                    <span className="address-type-badge">
                      {getAddressTypeLabel(address.addressType)}
                    </span>
                  )}
                  {address.isDefault && (
                    <span className="default-badge">
                      <FiCheck /> Default
                    </span>
                  )}
                </div>
                <div className="address-content">
                  <h3>{address.fullName}</h3>
                  <p>{address.addressLine1}</p>
                  {address.addressLine2 && <p>{address.addressLine2}</p>}
                  <p>
                    {address.city}, {address.state} - {address.postalCode}
                  </p>
                  <p className="phone">Phone: {address.phoneNumber}</p>
                </div>
                <div className="address-actions">
                  <button
                    className="btn-icon"
                    onClick={() => handleEdit(address)}
                    title="Edit"
                  >
                    <FiEdit2 />
                  </button>
                  <button
                    className="btn-icon delete"
                    onClick={() => handleDelete(address.id)}
                    title="Delete"
                  >
                    <FiTrash2 />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default Addresses;
