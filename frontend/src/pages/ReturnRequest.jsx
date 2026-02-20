import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { orderAPI, returnAPI, productAPI } from '../services/api';
import toast from 'react-hot-toast';
import './ReturnRequest.css';

const ReturnRequest = () => {
  const navigate = useNavigate();
  const { orderId, itemId } = useParams();
  const { user } = useSelector((state) => state.auth);

  const [order, setOrder] = useState(null);
  const [orderItem, setOrderItem] = useState(null);
  const [products, setProducts] = useState([]);
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [selectedVariant, setSelectedVariant] = useState(null);
  const [requestType, setRequestType] = useState('RETURN');
  const [reason, setReason] = useState('');
  const [loading, setLoading] = useState(false);
  const [loadingData, setLoadingData] = useState(true);
  const [existingReturn, setExistingReturn] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    loadOrderData();
  }, [orderId, itemId]);

  const loadOrderData = async () => {
    try {
      const response = await orderAPI.getById(parseInt(orderId));
      const orderData = response.data.data;
      setOrder(orderData);

      const item = orderData.items.find(item => item.id === parseInt(itemId));
      if (item) {
        setOrderItem(item);

        // Check if return already exists for this item
        if (item.returnRequested) {
          setExistingReturn(true);
        }
      } else {
        toast.error('Order item not found');
        navigate('/orders');
      }
    } catch (error) {
      toast.error('Failed to load order data');
      navigate('/orders');
    } finally {
      setLoadingData(false);
    }
  };

  const loadProducts = async (query = '') => {
    try {
      const params = { page: 0, size: 50 };
      if (query.trim()) {
        params.search = query.trim();
      }
      const response = await productAPI.getAll(params);
      setProducts(response.data.data?.content || response.data.data || []);
    } catch (error) {
      console.error('Failed to load products:', error);
    }
  };

  useEffect(() => {
    if (requestType === 'EXCHANGE') {
      loadProducts();
    }
  }, [requestType]);

  useEffect(() => {
    if (requestType === 'EXCHANGE') {
      const debounce = setTimeout(() => {
        loadProducts(searchQuery);
      }, 300);
      return () => clearTimeout(debounce);
    }
  }, [searchQuery]);

  const handleProductSelect = (productId) => {
    const product = products.find(p => p.id === parseInt(productId));
    setSelectedProduct(product);
    setSelectedVariant(null);
  };

  const handleVariantSelect = (variantId) => {
    const variant = selectedProduct?.variants?.find(v => v.id === parseInt(variantId));
    setSelectedVariant(variant);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!reason.trim()) {
      toast.error('Please provide a reason for return/exchange');
      return;
    }

    if (requestType === 'EXCHANGE') {
      if (!selectedProduct || !selectedVariant) {
        toast.error('Please select a product and variant for exchange');
        return;
      }
    }

    setLoading(true);
    try {
      await returnAPI.create({
        orderItemId: parseInt(itemId),
        requestType: requestType,
        reason: reason,
        exchangeProductId: requestType === 'EXCHANGE' ? selectedProduct.id : null,
        exchangeVariantId: requestType === 'EXCHANGE' ? selectedVariant.id : null,
      });

      toast.success('Return/Exchange request submitted successfully');
      navigate('/my-returns');
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to submit return request');
    } finally {
      setLoading(false);
    }
  };

  if (loadingData) {
    return <div className="return-request-page"><div className="loading">Loading...</div></div>;
  }

  if (!order || !orderItem) {
    return null;
  }

  if (existingReturn) {
    return (
      <div className="return-request-page">
        <div className="return-request-container">
          <h1>Return / Exchange Request</h1>
          <div className="existing-return-notice">
            <h2>Return Already Requested</h2>
            <p>A return/exchange request already exists for this item. You can track its status on the My Returns page.</p>
            <div className="request-actions">
              <button
                className="btn btn-secondary"
                onClick={() => navigate(`/orders/${orderId}`)}
              >
                Back to Order
              </button>
              <button
                className="btn btn-primary"
                onClick={() => navigate('/my-returns')}
              >
                View My Returns
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="return-request-page">
      <div className="return-request-container">
        <h1>Return / Exchange Request</h1>

        <div className="return-request-content">
          {/* Order Item Info */}
          <div className="request-section">
            <h2>Item to Return/Exchange</h2>
            <div className="order-item-card">
              <img src={orderItem.productImage || '/placeholder.jpg'} alt={orderItem.productName} />
              <div className="item-details">
                <h3>{orderItem.productName}</h3>
                <p>Size: {orderItem.size}</p>
                <p>Color: {orderItem.color}</p>
                <p>Quantity: {orderItem.quantity}</p>
                <p className="item-price">₹{orderItem.totalPrice}</p>
              </div>
            </div>
          </div>

          {/* Request Type Selection */}
          <div className="request-section">
            <h2>Request Type</h2>
            <div className="request-type-options">
              <label className="request-type-option">
                <input
                  type="radio"
                  name="requestType"
                  value="RETURN"
                  checked={requestType === 'RETURN'}
                  onChange={(e) => setRequestType(e.target.value)}
                />
                <div>
                  <strong>Return</strong>
                  <p>Get a refund for this item</p>
                </div>
              </label>
              <label className="request-type-option">
                <input
                  type="radio"
                  name="requestType"
                  value="EXCHANGE"
                  checked={requestType === 'EXCHANGE'}
                  onChange={(e) => setRequestType(e.target.value)}
                />
                <div>
                  <strong>Exchange</strong>
                  <p>Exchange for a different size/variant</p>
                </div>
              </label>
            </div>
          </div>

          {/* Exchange Product Selection */}
          {requestType === 'EXCHANGE' && (
            <div className="request-section">
              <h2>Select Replacement Product</h2>
              <div className="exchange-selectors">
                <div className="form-group">
                  <label>Search Products</label>
                  <input
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder="Type to search products..."
                    className="search-input"
                  />
                </div>
                <div className="form-group">
                  <label>Product</label>
                  <select
                    value={selectedProduct?.id || ''}
                    onChange={(e) => handleProductSelect(e.target.value)}
                  >
                    <option value="">Select a product</option>
                    {products.map((product) => (
                      <option key={product.id} value={product.id}>
                        {product.name}
                      </option>
                    ))}
                  </select>
                </div>

                {selectedProduct && selectedProduct.variants && (
                  <div className="form-group">
                    <label>Size & Color</label>
                    <select
                      value={selectedVariant?.id || ''}
                      onChange={(e) => handleVariantSelect(e.target.value)}
                    >
                      <option value="">Select variant</option>
                      {selectedProduct.variants
                        .filter(v => v.isAvailable && v.stockQuantity > 0)
                        .map((variant) => (
                          <option key={variant.id} value={variant.id}>
                            {variant.size} - {variant.color} (₹{variant.price})
                          </option>
                        ))}
                    </select>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Reason */}
          <div className="request-section">
            <h2>Reason</h2>
            <textarea
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="Please provide a reason for return/exchange (e.g., Size not fitting, Product damaged, Wrong item received, etc.)"
              rows="5"
              required
            />
          </div>

          {/* Submit Button */}
          <div className="request-actions">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => navigate(`/orders/${orderId}`)}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              onClick={handleSubmit}
              disabled={loading}
            >
              {loading ? 'Submitting...' : 'Submit Request'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ReturnRequest;
