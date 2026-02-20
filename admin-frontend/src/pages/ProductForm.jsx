import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { productAPI, categoryAPI, uploadAPI } from '../services/api';
import toast from 'react-hot-toast';
import { FiX, FiUpload, FiStar, FiPlus, FiTrash2 } from 'react-icons/fi';
import './ProductForm.css';

const ProductForm = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    slug: '',
    description: '',
    price: '',
    originalPrice: '',
    discountPercentage: '',
    categoryId: '',
    brandName: '',
    material: '',
    careInstructions: '',
    countryOfOrigin: '',
    isFeatured: false,
    isNewArrival: false,
    isBestSeller: false,
    isReturnable: true,
    metaTitle: '',
    metaDescription: '',
    metaKeywords: '',
    ogImage: '',
  });

  const [calculatedPrice, setCalculatedPrice] = useState(null);
  const [images, setImages] = useState([]);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [variants, setVariants] = useState([]);
  const [validationErrors, setValidationErrors] = useState({});
  const [variantErrors, setVariantErrors] = useState({});

  const calculatePriceValue = (originalPrice, discountPercentage) => {
    if (!originalPrice) return null;
    const original = parseFloat(originalPrice);
    if (isNaN(original) || original <= 0) return null;
    
    if (!discountPercentage || discountPercentage === '' || discountPercentage === '0') {
      return Math.round(original).toString();
    }
    
    const discount = parseFloat(discountPercentage);
    if (isNaN(discount) || discount < 0 || discount > 100) return Math.round(original).toString();
    
    const calculated = original - (original * discount / 100);
    return Math.round(calculated).toString();
  };

  useEffect(() => {
    fetchCategories();
    if (id) {
      fetchProduct();
    }
  }, [id]);

  const fetchCategories = async () => {
    try {
      const response = await categoryAPI.getAll();
      setCategories(response.data.data || []);
    } catch (error) {
      toast.error('Failed to load categories');
    }
  };

  const fetchProduct = async () => {
    try {
      const response = await productAPI.getById(id);
      const product = response.data.data;
      const originalPrice = product.originalPrice || product.price || '';
      const discountPercentage = product.discountPercentage || '';
      const price = product.price || '';
      
      setFormData({
        name: product.name || '',
        slug: product.slug || '',
        description: product.description || '',
        price: price,
        originalPrice: product.originalPrice || product.price || '',
        discountPercentage: discountPercentage,
        categoryId: product.categoryId || '',
        brandName: product.brandName || '',
        material: product.material || '',
        careInstructions: product.careInstructions || '',
        countryOfOrigin: product.countryOfOrigin || '',
        isFeatured: product.isFeatured || false,
        isNewArrival: product.isNewArrival || false,
        isBestSeller: product.isBestSeller || false,
        isReturnable: product.isReturnable !== undefined ? product.isReturnable : true,
        metaTitle: product.metaTitle || '',
        metaDescription: product.metaDescription || '',
        metaKeywords: product.metaKeywords || '',
        ogImage: product.ogImage || '',
      });
      
      // Calculate and display calculated price if discount exists
      if (originalPrice && discountPercentage) {
        const calc = calculatePriceValue(originalPrice, discountPercentage);
        setCalculatedPrice(calc);
      } else {
        setCalculatedPrice(null);
      }
      
      // Load images
      const apiBase = import.meta.env.VITE_API_BASE_URL || '';
      if (product.images && product.images.length > 0) {
      setImages(product.images.map(img => ({
        id: img.id,
        imageUrl: img.imageUrl.startsWith('http') ? img.imageUrl : `${apiBase}${img.imageUrl}`,
        altText: img.altText || product.name || '',
        isPrimary: img.isPrimary || false,
        displayOrder: img.displayOrder || 0
      })));
      } else {
        setImages([]);
      }

      // Load variants
      if (product.variants && product.variants.length > 0) {
        setVariants(product.variants.map(v => ({
          id: v.id,
          size: v.size || '',
          color: v.color || '',
          colorCode: v.colorCode || '',
          sku: v.sku || '',
          stockQuantity: v.stockQuantity ?? 0,
          isAvailable: v.isAvailable !== undefined ? v.isAvailable : true,
        })));
      } else {
        setVariants([]);
      }
    } catch (error) {
      toast.error('Failed to load product');
    }
  };
  
  const handleImageUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    
    if (!file.type.startsWith('image/')) {
      toast.error('Please select an image file');
      return;
    }
    
    if (file.size > 10 * 1024 * 1024) {
      toast.error('Image size should be less than 10MB');
      return;
    }
    
    setUploading(true);
    setUploadProgress(0);
    try {
      const response = await uploadAPI.upload(file, (progressEvent) => {
        const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total);
        setUploadProgress(percent);
      });
      const imageUrl = response.data.data;
      const apiBase = import.meta.env.VITE_API_BASE_URL || '';
      const newImage = {
        id: null,
        imageUrl: imageUrl.startsWith('http') ? imageUrl : `${apiBase}${imageUrl}`,
        altText: formData.name || 'Product image',
        isPrimary: images.length === 0,
        displayOrder: images.length
      };
      setImages([...images, newImage]);
      toast.success('Image uploaded successfully');
    } catch (error) {
      toast.error('Failed to upload image');
    } finally {
      setUploading(false);
      setUploadProgress(0);
      e.target.value = '';
    }
  };
  
  const handleRemoveImage = (index) => {
    const newImages = images.filter((_, i) => i !== index);
    // Ensure at least one image is primary if images exist
    if (newImages.length > 0 && !newImages.some(img => img.isPrimary)) {
      newImages[0].isPrimary = true;
    }
    setImages(newImages);
  };
  
  const handleSetPrimary = (index) => {
    const newImages = images.map((img, i) => ({
      ...img,
      isPrimary: i === index
    }));
    setImages(newImages);
  };

  const generateSlug = (name) => {
    return name
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/(^-|-$)/g, '');
  };

  const validateForm = () => {
    const errors = {};
    const vErrors = {};
    const price = parseFloat(formData.price);
    const originalPrice = parseFloat(formData.originalPrice);

    if (!formData.name || formData.name.trim() === '') {
      errors.name = 'Product name is required';
    }
    if (!formData.slug || formData.slug.trim() === '') {
      errors.slug = 'Slug is required';
    } else if (!/^[a-z0-9]+(-[a-z0-9]+)*$/.test(formData.slug)) {
      errors.slug = 'Slug must be lowercase, alphanumeric, separated by hyphens';
    }
    if (!formData.categoryId) {
      errors.categoryId = 'Category is required';
    }
    if (isNaN(price) || price <= 0) {
      errors.price = 'Price must be greater than 0';
    }
    if (formData.originalPrice && (isNaN(originalPrice) || originalPrice <= 0)) {
      errors.originalPrice = 'Original price must be greater than 0';
    }
    if (!isNaN(price) && !isNaN(originalPrice) && originalPrice > 0 && price > originalPrice) {
      errors.price = 'Final price cannot be higher than original price';
    }
    if (images.length === 0) {
      errors.images = 'At least one product image is required';
    }

    // Require at least one variant with size and color
    const completeVariants = variants.filter(v => v.size && v.color);
    if (completeVariants.length === 0) {
      errors.variants = 'At least one variant with size and color is required';
    }

    // Per-row validation
    const seen = new Map();
    let hasRowError = false;
    variants.forEach((v, i) => {
      const rowErrors = {};
      if (!v.size) rowErrors.size = 'Size is required';
      if (!v.color) rowErrors.color = 'Color is required';
      if (v.stockQuantity < 0) rowErrors.stock = 'Cannot be negative';

      // Check duplicate only if this row has both size and color
      if (v.size && v.color) {
        const key = `${v.size.trim().toLowerCase()}|${v.color.trim().toLowerCase()}`;
        if (seen.has(key)) {
          rowErrors.duplicate = 'Duplicate of row ' + (seen.get(key) + 1);
        } else {
          seen.set(key, i);
        }
      }

      if (Object.keys(rowErrors).length > 0) {
        vErrors[i] = rowErrors;
        hasRowError = true;
      }
    });

    if (hasRowError && !errors.variants) {
      errors.variants = 'Fix variant errors below';
    }

    setValidationErrors(errors);
    setVariantErrors(vErrors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) {
      toast.error('Please fix the validation errors before submitting');
      return;
    }

    setLoading(true);

    try {
      const apiBase = import.meta.env.VITE_API_BASE_URL || '';
      const data = {
        ...formData,
        price: parseFloat(formData.price),
        originalPrice: parseFloat(formData.originalPrice) || null,
        discountPercentage: formData.discountPercentage ? parseFloat(formData.discountPercentage) : null,
        categoryId: parseInt(formData.categoryId),
        images: images.map((img, index) => {
          // Always store relative paths (e.g., /uploads/xxx) in the database
          let imageUrl = img.imageUrl;
          if (apiBase && imageUrl.startsWith(apiBase)) {
            imageUrl = imageUrl.replace(apiBase, '');
          } else if (imageUrl.startsWith('http')) {
            // Extract path from full URL if it contains our upload path
            const uploadMatch = imageUrl.match(/\/uploads\/.+$/);
            if (uploadMatch) {
              imageUrl = uploadMatch[0];
            }
          }
          return {
            id: img.id,
            imageUrl: imageUrl,
            altText: img.altText || formData.name,
            isPrimary: img.isPrimary || index === 0,
            displayOrder: index
          };
        }),
        variants: variants
          .filter(v => v.size && v.color)
          .map(v => ({
            id: v.id || null,
            size: v.size.trim(),
            color: v.color.trim(),
            colorCode: v.colorCode || null,
            sku: v.sku || null,
            stockQuantity: parseInt(v.stockQuantity) || 0,
            isAvailable: v.isAvailable,
          }))
      };

      if (id) {
        await productAPI.update(id, data);
        toast.success('Product updated successfully');
      } else {
        await productAPI.create(data);
        toast.success('Product created successfully');
      }
      navigate('/admin/products');
    } catch (error) {
      toast.error(error.response?.data?.message || 'Operation failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="product-form-page">
      <div className="page-header">
        <h1>{id ? 'Edit Product' : 'Create Product'}</h1>
        <button className="btn-secondary" onClick={() => navigate('/admin/products')}>
          Back to Products
        </button>
      </div>

      <form onSubmit={handleSubmit} className="product-form">
        <div className="form-section">
          <h3>Basic Information</h3>
          <div className="form-row">
            <div className="form-group">
              <label>Product Name *</label>
              <input
                type="text"
                required
                value={formData.name}
                onChange={(e) => {
                  setFormData({ ...formData, name: e.target.value });
                  if (!id) {
                    setFormData((prev) => ({
                      ...prev,
                      slug: generateSlug(e.target.value),
                    }));
                  }
                }}
              />
            </div>
            <div className="form-group">
              <label>Slug *</label>
              <input
                type="text"
                required
                placeholder="url-friendly-identifier (auto-generated from name)"
                value={formData.slug}
                onChange={(e) =>
                  setFormData({ ...formData, slug: e.target.value })
                }
                style={validationErrors.slug ? { borderColor: '#f56565' } : {}}
              />
              {validationErrors.slug ? (
                <small style={{ color: '#f56565', fontSize: '12px', marginTop: '4px', display: 'block' }}>
                  {validationErrors.slug}
                </small>
              ) : (
                <small style={{ color: '#666', fontSize: '12px', marginTop: '4px', display: 'block' }}>
                  URL-friendly identifier (e.g., "blue-cotton-t-shirt"). Auto-generated from product name but can be edited.
                </small>
              )}
            </div>
          </div>
          <div className="form-group">
            <label>Description</label>
            <textarea
              rows="4"
              value={formData.description}
              onChange={(e) =>
                setFormData({ ...formData, description: e.target.value })
              }
            />
          </div>
        </div>

        <div className="form-section">
          <h3>Pricing</h3>
          <div className="form-row">
            <div className="form-group">
              <label>Original Price (₹) *</label>
              <input
                type="number"
                step="0.01"
                min="0"
                required
                value={formData.originalPrice}
                onChange={(e) => {
                  const newOriginalPrice = e.target.value;
                  const calc = calculatePriceValue(newOriginalPrice, formData.discountPercentage);
                  setCalculatedPrice(calc);
                  setFormData(prev => ({
                    ...prev,
                    originalPrice: newOriginalPrice,
                    price: calc || newOriginalPrice
                  }));
                }}
              />
            </div>
            <div className="form-group">
              <label>Discount (%)</label>
              <input
                type="number"
                min="0"
                max="100"
                step="0.01"
                value={formData.discountPercentage}
                onChange={(e) => {
                  const newDiscount = e.target.value;
                  const calc = calculatePriceValue(formData.originalPrice, newDiscount);
                  setCalculatedPrice(calc);
                  setFormData(prev => ({
                    ...prev,
                    discountPercentage: newDiscount,
                    price: calc || prev.price
                  }));
                }}
              />
            </div>
            <div className="form-group">
              <label>Final Price (₹) *</label>
              <input
                type="number"
                step="0.01"
                min="0"
                required
                value={formData.price}
                onChange={(e) => {
                  setFormData({ ...formData, price: e.target.value });
                  setCalculatedPrice(null);
                }}
                readOnly={calculatedPrice !== null}
                style={calculatedPrice !== null ? { backgroundColor: '#f5f5f5', cursor: 'not-allowed' } : {}}
              />
              {calculatedPrice !== null && (
                <small style={{ color: '#28a745', fontSize: '12px', marginTop: '4px', display: 'block', fontWeight: '500' }}>
                  ✓ Auto-calculated from Original Price - Discount
                </small>
              )}
            </div>
          </div>
        </div>

        <div className="form-section">
          <h3>Product Images</h3>
          <div className="image-upload-section">
            <div className="image-upload-grid">
              {images.map((image, index) => (
                <div key={index} className="image-preview-item">
                  <div className="image-preview">
                    <img src={image.imageUrl} alt={image.altText} />
                    {image.isPrimary && (
                      <div className="primary-badge">
                        <FiStar /> Primary
                      </div>
                    )}
                    <button
                      type="button"
                      className="remove-image-btn"
                      onClick={() => handleRemoveImage(index)}
                    >
                      <FiX />
                    </button>
                  </div>
                  <button
                    type="button"
                    className={`set-primary-btn ${image.isPrimary ? 'active' : ''}`}
                    onClick={() => handleSetPrimary(index)}
                    disabled={image.isPrimary}
                  >
                    {image.isPrimary ? 'Primary' : 'Set as Primary'}
                  </button>
                </div>
              ))}
              <div className="image-upload-placeholder">
                {uploading ? (
                  <div className="upload-progress-container" style={{ padding: '16px', textAlign: 'center' }}>
                    <FiUpload style={{ fontSize: '24px', color: '#667eea', marginBottom: '8px' }} />
                    <div className="upload-progress-bar">
                      <div
                        className="upload-progress-fill"
                        style={{ width: `${uploadProgress}%` }}
                      />
                    </div>
                    <div className="upload-progress-text">
                      Uploading... {uploadProgress}%
                    </div>
                  </div>
                ) : (
                  <label htmlFor="image-upload" className="upload-label">
                    <FiUpload />
                    <span>Upload Image</span>
                    <input
                      id="image-upload"
                      type="file"
                      accept="image/*"
                      onChange={handleImageUpload}
                      disabled={uploading}
                      style={{ display: 'none' }}
                    />
                  </label>
                )}
              </div>
            </div>
            <small style={{ color: '#666', fontSize: '12px', marginTop: '12px', display: 'block' }}>
              Upload product images. The first image will be set as primary. You can change the primary image by clicking "Set as Primary".
            </small>
            {validationErrors.images && (
              <small style={{ color: '#f56565', fontSize: '12px', marginTop: '4px', display: 'block' }}>
                {validationErrors.images}
              </small>
            )}
          </div>
        </div>

        <div className="form-section">
          <h3>Category & Brand</h3>
          <div className="form-row">
            <div className="form-group">
              <label>Category *</label>
              <select
                required
                value={formData.categoryId}
                onChange={(e) =>
                  setFormData({ ...formData, categoryId: e.target.value })
                }
              >
                <option value="">Select Category</option>
                {categories.map((cat) => (
                  <option key={cat.id} value={cat.id}>
                    {cat.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label>Brand Name</label>
              <input
                type="text"
                value={formData.brandName}
                onChange={(e) =>
                  setFormData({ ...formData, brandName: e.target.value })
                }
              />
            </div>
          </div>
        </div>

        <div className="form-section">
          <h3>Product Details</h3>
          <div className="form-row">
            <div className="form-group">
              <label>Material</label>
              <input
                type="text"
                placeholder="e.g. 100% Cotton, Polyester Blend"
                value={formData.material}
                onChange={(e) =>
                  setFormData({ ...formData, material: e.target.value })
                }
              />
            </div>
            <div className="form-group">
              <label>Country of Origin</label>
              <input
                type="text"
                placeholder="e.g. India"
                value={formData.countryOfOrigin}
                onChange={(e) =>
                  setFormData({ ...formData, countryOfOrigin: e.target.value })
                }
              />
            </div>
          </div>
          <div className="form-group">
            <label>Care Instructions</label>
            <textarea
              rows="3"
              placeholder="e.g. Machine wash cold, tumble dry low"
              value={formData.careInstructions}
              onChange={(e) =>
                setFormData({ ...formData, careInstructions: e.target.value })
              }
            />
          </div>
        </div>

        <div className="form-section">
          <h3>Product Variants (Sizes, Colors & Stock)</h3>
          <div className="variants-section">
            {variants.length > 0 && (
              <div className="variants-table-container">
                <table className="variants-table">
                  <thead>
                    <tr>
                      <th>Size</th>
                      <th>Color</th>
                      <th>Color Code</th>
                      <th>SKU</th>
                      <th>Stock</th>
                      <th>Available</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {variants.map((variant, index) => {
                      const rowErr = variantErrors[index] || {};
                      const hasError = Object.keys(rowErr).length > 0;
                      return (
                      <tr key={index} style={hasError ? { backgroundColor: '#fff8f8' } : {}}>
                        <td>
                          <input
                            type="text"
                            value={variant.size}
                            placeholder="e.g. S, M, L, XL"
                            style={rowErr.size ? { borderColor: '#f56565' } : {}}
                            onChange={(e) => {
                              const updated = [...variants];
                              updated[index] = { ...updated[index], size: e.target.value };
                              setVariants(updated);
                            }}
                          />
                          {rowErr.size && <small className="variant-error">{rowErr.size}</small>}
                        </td>
                        <td>
                          <input
                            type="text"
                            value={variant.color}
                            placeholder="e.g. Red, Blue"
                            style={rowErr.color ? { borderColor: '#f56565' } : {}}
                            onChange={(e) => {
                              const updated = [...variants];
                              updated[index] = { ...updated[index], color: e.target.value };
                              setVariants(updated);
                            }}
                          />
                          {rowErr.color && <small className="variant-error">{rowErr.color}</small>}
                          {rowErr.duplicate && <small className="variant-error">{rowErr.duplicate}</small>}
                        </td>
                        <td>
                          <div className="color-code-input">
                            <input
                              type="text"
                              value={variant.colorCode}
                              placeholder="#FF0000"
                              onChange={(e) => {
                                const updated = [...variants];
                                updated[index] = { ...updated[index], colorCode: e.target.value };
                                setVariants(updated);
                              }}
                            />
                            {variant.colorCode && (
                              <span
                                className="color-preview"
                                style={{ backgroundColor: variant.colorCode }}
                              />
                            )}
                          </div>
                        </td>
                        <td>
                          <input
                            type="text"
                            value={variant.sku}
                            placeholder="Auto or manual"
                            onChange={(e) => {
                              const updated = [...variants];
                              updated[index] = { ...updated[index], sku: e.target.value };
                              setVariants(updated);
                            }}
                          />
                        </td>
                        <td>
                          <input
                            type="number"
                            min="0"
                            value={variant.stockQuantity}
                            style={rowErr.stock ? { borderColor: '#f56565' } : {}}
                            onChange={(e) => {
                              const updated = [...variants];
                              updated[index] = { ...updated[index], stockQuantity: parseInt(e.target.value) || 0 };
                              setVariants(updated);
                            }}
                          />
                          {rowErr.stock && <small className="variant-error">{rowErr.stock}</small>}
                        </td>
                        <td>
                          <input
                            type="checkbox"
                            checked={variant.isAvailable}
                            onChange={(e) => {
                              const updated = [...variants];
                              updated[index] = { ...updated[index], isAvailable: e.target.checked };
                              setVariants(updated);
                            }}
                          />
                        </td>
                        <td>
                          <button
                            type="button"
                            className="btn-icon danger"
                            onClick={() => {
                              setVariants(variants.filter((_, i) => i !== index));
                            }}
                            title="Remove variant"
                          >
                            <FiTrash2 />
                          </button>
                        </td>
                      </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
            <button
              type="button"
              className="btn-add-variant"
              onClick={() => {
                setVariants([...variants, {
                  id: null,
                  size: '',
                  color: '',
                  colorCode: '',
                  sku: '',
                  stockQuantity: 0,
                  isAvailable: true,
                }]);
              }}
            >
              <FiPlus /> Add Variant
            </button>
            {variants.length === 0 && (
              <small style={{ color: '#999', fontSize: '12px', marginTop: '8px', display: 'block' }}>
                Add at least one variant (size/color combination) so customers can purchase this product.
              </small>
            )}
            {validationErrors.variants && (
              <small style={{ color: '#f56565', fontSize: '12px', marginTop: '4px', display: 'block' }}>
                {validationErrors.variants}
              </small>
            )}
          </div>
        </div>

        <div className="form-section">
          <h3>Flags</h3>
          <div className="checkbox-group">
            <label>
              <input
                type="checkbox"
                checked={formData.isFeatured}
                onChange={(e) =>
                  setFormData({ ...formData, isFeatured: e.target.checked })
                }
              />
              Featured Product
            </label>
            <label>
              <input
                type="checkbox"
                checked={formData.isNewArrival}
                onChange={(e) =>
                  setFormData({ ...formData, isNewArrival: e.target.checked })
                }
              />
              New Arrival
            </label>
            <label>
              <input
                type="checkbox"
                checked={formData.isBestSeller}
                onChange={(e) =>
                  setFormData({ ...formData, isBestSeller: e.target.checked })
                }
              />
              Best Seller
            </label>
            <label>
              <input
                type="checkbox"
                checked={formData.isReturnable}
                onChange={(e) =>
                  setFormData({ ...formData, isReturnable: e.target.checked })
                }
              />
              Allow Returns/Exchanges
            </label>
          </div>
        </div>

        <div className="form-section">
          <h3>SEO</h3>
          <div className="form-group">
            <label>Meta Title</label>
            <input
              type="text"
              maxLength={255}
              value={formData.metaTitle}
              onChange={(e) =>
                setFormData({ ...formData, metaTitle: e.target.value })
              }
              placeholder="Page title for search engines"
            />
            <small>{formData.metaTitle.length}/255</small>
          </div>
          <div className="form-group">
            <label>Meta Description</label>
            <textarea
              rows="3"
              maxLength={500}
              value={formData.metaDescription}
              onChange={(e) =>
                setFormData({ ...formData, metaDescription: e.target.value })
              }
              placeholder="Brief description for search engine results"
            />
            <small>{formData.metaDescription.length}/500</small>
          </div>
          <div className="form-group">
            <label>Meta Keywords</label>
            <input
              type="text"
              maxLength={500}
              value={formData.metaKeywords}
              onChange={(e) =>
                setFormData({ ...formData, metaKeywords: e.target.value })
              }
              placeholder="Comma-separated keywords"
            />
          </div>
          <div className="form-group">
            <label>OG Image URL</label>
            <input
              type="text"
              maxLength={500}
              value={formData.ogImage}
              onChange={(e) =>
                setFormData({ ...formData, ogImage: e.target.value })
              }
              placeholder="Image URL for social media sharing"
            />
          </div>
        </div>

        <div className="form-actions">
          <button type="button" className="btn-secondary" onClick={() => navigate('/admin/products')}>
            Cancel
          </button>
          <button type="submit" className="btn-primary" disabled={loading}>
            {loading ? 'Saving...' : id ? 'Update Product' : 'Create Product'}
          </button>
        </div>
      </form>
    </div>
  );
};

export default ProductForm;

