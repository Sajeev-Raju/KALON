import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { productAPI, categoryAPI } from '../services/api';
import { FiPlus, FiEdit, FiTrash2, FiSearch } from 'react-icons/fi';
import toast from 'react-hot-toast';
import Pagination from '../components/common/Pagination';
import './Products.css';

const Products = () => {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [minPrice, setMinPrice] = useState('');
  const [maxPrice, setMaxPrice] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [categories, setCategories] = useState([]);
  const [showFilters, setShowFilters] = useState(false);
  const navigate = useNavigate();

  // Debounce search input — waits 400ms after user stops typing
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(searchTerm);
    }, 400);
    return () => clearTimeout(timer);
  }, [searchTerm]);

  useEffect(() => {
    fetchCategories();
  }, []);

  useEffect(() => {
    fetchProducts();
  }, [page, debouncedSearch, minPrice, maxPrice, categoryId]);

  useEffect(() => {
    setPage(0);
  }, [debouncedSearch, minPrice, maxPrice, categoryId]);

  const fetchCategories = async () => {
    try {
      const response = await categoryAPI.getAll();
      setCategories(response.data.data || []);
    } catch (error) {
      // silently fail — categories are optional filter
    }
  };

  const fetchProducts = async () => {
    try {
      setLoading(true);
      const params = { page, size: 20 };
      if (debouncedSearch.trim()) params.search = debouncedSearch.trim();
      if (minPrice) params.minPrice = minPrice;
      if (maxPrice) params.maxPrice = maxPrice;
      if (categoryId) params.categoryId = categoryId;
      const response = await productAPI.getAll(params);
      const data = response.data.data;
      setProducts(data.content || []);
      setTotalPages(data.totalPages || 0);
    } catch (error) {
      toast.error('Failed to load products');
    } finally {
      setLoading(false);
    }
  };

  const clearFilters = () => {
    setMinPrice('');
    setMaxPrice('');
    setCategoryId('');
    setSearchTerm('');
    setPage(0);
  };

  const hasActiveFilters = minPrice || maxPrice || categoryId;

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to deactivate this product? It will be hidden from the store but data will be preserved.')) {
      return;
    }

    try {
      await productAPI.delete(id);
      toast.success('Product deactivated successfully');
      fetchProducts();
    } catch (error) {
      toast.error('Failed to deactivate product');
    }
  };

  const filteredProducts = products;

  return (
    <div className="products-page">
      <div className="page-header">
        <div>
          <h1>Products</h1>
          <p>Manage your product catalog</p>
        </div>
        <button
          className="btn-primary"
          onClick={() => navigate('/admin/products/new')}
        >
          <FiPlus /> Add Product
        </button>
      </div>

      <div className="products-toolbar" style={{ display: 'flex', gap: '12px', marginBottom: '16px', alignItems: 'center', flexWrap: 'wrap' }}>
        <div className="search-box">
          <FiSearch />
          <input
            type="text"
            placeholder="Search products..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
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
      </div>

      {showFilters && (
        <div className="advanced-filters" style={{
          display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
          gap: '12px', padding: '16px', background: '#f8f9fa', borderRadius: '8px', marginBottom: '16px',
        }}>
          <div>
            <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px', color: '#555' }}>Category</label>
            <select
              value={categoryId}
              onChange={(e) => setCategoryId(e.target.value)}
              style={{ width: '100%', padding: '8px 12px', border: '2px solid #e0e0e0', borderRadius: '8px', fontSize: '14px' }}
            >
              <option value="">All Categories</option>
              {categories.map((cat) => (
                <option key={cat.id} value={cat.id}>{cat.name}</option>
              ))}
            </select>
          </div>
          <div>
            <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px', color: '#555' }}>Min Price</label>
            <input
              type="number"
              placeholder="e.g. 500"
              value={minPrice}
              onChange={(e) => setMinPrice(e.target.value)}
              style={{ width: '100%', padding: '8px 12px', border: '2px solid #e0e0e0', borderRadius: '8px', fontSize: '14px' }}
            />
          </div>
          <div>
            <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px', color: '#555' }}>Max Price</label>
            <input
              type="number"
              placeholder="e.g. 10000"
              value={maxPrice}
              onChange={(e) => setMaxPrice(e.target.value)}
              style={{ width: '100%', padding: '8px 12px', border: '2px solid #e0e0e0', borderRadius: '8px', fontSize: '14px' }}
            />
          </div>
        </div>
      )}

      {loading ? (
        <div className="loading">Loading products...</div>
      ) : (
        <>
          <div className="products-table-container">
            <table className="products-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Name</th>
                  <th>Category</th>
                  <th>Price</th>
                  <th>Stock</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredProducts.length === 0 ? (
                  <tr>
                    <td colSpan="7" className="empty-state">
                      No products found
                    </td>
                  </tr>
                ) : (
                  filteredProducts.map((product) => {
                    const totalStock = product.variants?.reduce(
                      (sum, v) => sum + (v.stockQuantity || 0),
                      0
                    ) || 0;

                    return (
                      <tr key={product.id}>
                        <td>{product.id}</td>
                        <td>
                          <div className="product-info">
                            {product.images?.[0]?.imageUrl && (
                              <img
                                src={product.images[0].imageUrl}
                                alt={product.name}
                                className="product-thumb"
                              />
                            )}
                            <span>{product.name}</span>
                          </div>
                        </td>
                        <td>{product.categoryName}</td>
                        <td>₹{product.price?.toLocaleString()}</td>
                        <td>{totalStock}</td>
                        <td>
                          <span
                            className={`status-badge ${
                              product.isActive ? 'active' : 'inactive'
                            }`}
                          >
                            {product.isActive ? 'Active' : 'Inactive'}
                          </span>
                        </td>
                        <td>
                          <div className="action-buttons">
                            <button
                              className="btn-icon"
                              onClick={() =>
                                navigate(`/admin/products/${product.id}/edit`)
                              }
                              title="Edit"
                            >
                              <FiEdit />
                            </button>
                            <button
                              className="btn-icon danger"
                              onClick={() => handleDelete(product.id)}
                              title="Deactivate"
                            >
                              <FiTrash2 />
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })
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

export default Products;



