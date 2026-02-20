import { useState, useEffect } from 'react';
import { categoryAPI } from '../services/api';
import { FiPlus, FiEdit, FiTrash2 } from 'react-icons/fi';
import toast from 'react-hot-toast';
import './Categories.css';

const Categories = () => {
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingCategory, setEditingCategory] = useState(null);
  const [formData, setFormData] = useState({
    name: '',
    slug: '',
    description: '',
    imageUrl: '',
    genderType: '',
    displayOrder: 0,
    metaTitle: '',
    metaDescription: '',
    metaKeywords: '',
    ogImage: '',
  });

  useEffect(() => {
    fetchCategories();
  }, []);

  const fetchCategories = async () => {
    try {
      const response = await categoryAPI.getAll();
      setCategories(response.data.data || []);
    } catch (error) {
      toast.error('Failed to load categories');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingCategory) {
        await categoryAPI.update(editingCategory.id, formData);
        toast.success('Category updated successfully');
      } else {
        await categoryAPI.create(formData);
        toast.success('Category created successfully');
      }
      setShowForm(false);
      setEditingCategory(null);
      setFormData({
        name: '',
        slug: '',
        description: '',
        imageUrl: '',
        genderType: '',
        displayOrder: 0,
        metaTitle: '',
        metaDescription: '',
        metaKeywords: '',
        ogImage: '',
      });
      fetchCategories();
    } catch (error) {
      toast.error('Operation failed');
    }
  };

  const handleEdit = (category) => {
    setEditingCategory(category);
    setFormData({
      name: category.name || '',
      slug: category.slug || '',
      description: category.description || '',
      imageUrl: category.imageUrl || '',
      genderType: category.genderType || '',
      displayOrder: category.displayOrder || 0,
      metaTitle: category.metaTitle || '',
      metaDescription: category.metaDescription || '',
      metaKeywords: category.metaKeywords || '',
      ogImage: category.ogImage || '',
    });
    setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to deactivate this category?')) {
      return;
    }
    try {
      await categoryAPI.delete(id);
      toast.success('Category deactivated successfully');
      fetchCategories();
    } catch (error) {
      const message = error.response?.data?.message || 'Failed to deactivate category';
      toast.error(message, { duration: 5000 });
    }
  };

  return (
    <div className="categories-page">
      <div className="page-header">
        <div>
          <h1>Categories</h1>
          <p>Manage product categories</p>
        </div>
        <button className="btn-primary" onClick={() => setShowForm(true)}>
          <FiPlus /> Add Category
        </button>
      </div>

      {showForm && (
        <div className="category-form-modal">
          <div className="modal-content">
            <h2>{editingCategory ? 'Edit Category' : 'Create Category'}</h2>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Name *</label>
                <input
                  type="text"
                  required
                  value={formData.name}
                  onChange={(e) =>
                    setFormData({ ...formData, name: e.target.value })
                  }
                />
              </div>
              <div className="form-group">
                <label>Slug *</label>
                <input
                  type="text"
                  required
                  value={formData.slug}
                  onChange={(e) =>
                    setFormData({ ...formData, slug: e.target.value })
                  }
                />
              </div>
              <div className="form-group">
                <label>Description</label>
                <textarea
                  rows="3"
                  value={formData.description}
                  onChange={(e) =>
                    setFormData({ ...formData, description: e.target.value })
                  }
                />
              </div>
              <div className="form-group">
                <label>Gender Type</label>
                <select
                  value={formData.genderType}
                  onChange={(e) =>
                    setFormData({ ...formData, genderType: e.target.value })
                  }
                >
                  <option value="">Select</option>
                  <option value="MEN">Men</option>
                  <option value="WOMEN">Women</option>
                  <option value="UNISEX">Unisex</option>
                  <option value="KIDS">Kids</option>
                </select>
              </div>
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
              </div>
              <div className="form-group">
                <label>Meta Description</label>
                <textarea
                  rows="2"
                  maxLength={500}
                  value={formData.metaDescription}
                  onChange={(e) =>
                    setFormData({ ...formData, metaDescription: e.target.value })
                  }
                  placeholder="Brief description for search engine results"
                />
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
              <div className="form-actions">
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() => {
                    setShowForm(false);
                    setEditingCategory(null);
                  }}
                >
                  Cancel
                </button>
                <button type="submit" className="btn-primary">
                  {editingCategory ? 'Update' : 'Create'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {loading ? (
        <div className="loading">Loading categories...</div>
      ) : (
        <div className="categories-grid">
          {categories.map((category) => (
            <div key={category.id} className="category-card">
              <h3>{category.name}</h3>
              <p>{category.description || 'No description'}</p>
              <div className="category-meta">
                <span>Slug: {category.slug}</span>
                {category.genderType && (
                  <span>Type: {category.genderType}</span>
                )}
              </div>
              <div className="category-actions">
                <button
                  className="btn-icon"
                  onClick={() => handleEdit(category)}
                >
                  <FiEdit /> Edit
                </button>
                <button
                  className="btn-icon danger"
                  onClick={() => handleDelete(category.id)}
                >
                  <FiTrash2 /> Deactivate
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default Categories;



