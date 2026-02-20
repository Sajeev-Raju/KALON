import { useState, useEffect } from 'react';
import { siteConfigAPI } from '../services/api';
import { FiPlus, FiEdit, FiTrash2 } from 'react-icons/fi';
import toast from 'react-hot-toast';
import './SiteConfig.css';

const SiteConfig = () => {
  const [configs, setConfigs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingConfig, setEditingConfig] = useState(null);
  const [formData, setFormData] = useState({
    configKey: '',
    configValue: '',
    configType: 'TEXT',
    description: '',
    displayOrder: 0,
  });

  useEffect(() => {
    fetchConfigs();
  }, []);

  const fetchConfigs = async () => {
    try {
      const response = await siteConfigAPI.getAll();
      setConfigs(response.data.data || []);
    } catch (error) {
      toast.error('Failed to load configurations');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      await siteConfigAPI.createOrUpdate(formData);
      toast.success(editingConfig ? 'Configuration updated' : 'Configuration created');
      setShowForm(false);
      setEditingConfig(null);
      setFormData({
        configKey: '',
        configValue: '',
        configType: 'TEXT',
        description: '',
        displayOrder: 0,
      });
      fetchConfigs();
    } catch (error) {
      toast.error('Operation failed');
    }
  };

  const handleEdit = (config) => {
    setEditingConfig(config);
    setFormData({
      configKey: config.configKey,
      configValue: config.configValue,
      configType: config.configType,
      description: config.description || '',
      displayOrder: config.displayOrder || 0,
    });
    setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this configuration?')) {
      return;
    }
    try {
      await siteConfigAPI.delete(id);
      toast.success('Configuration deleted');
      fetchConfigs();
    } catch (error) {
      toast.error('Failed to delete configuration');
    }
  };

  const commonConfigs = [
    { key: 'cashback_message', description: 'Cashback banner message', value: 'Earn 10% Cashback on Every App Order' },
    { key: 'cashback_enabled', description: 'Enable cashback banner', value: 'true', type: 'BOOLEAN' },
    { key: 'sidebar_product_types_count', description: 'Number of product types to show in sidebar', value: '8', type: 'NUMBER' },
  ];

  const handleAddCommon = async (config) => {
    try {
      await siteConfigAPI.createOrUpdate({
        configKey: config.key,
        configValue: config.value,
        configType: config.type || 'TEXT',
        description: config.description,
      });
      toast.success('Configuration added');
      fetchConfigs();
    } catch (error) {
      toast.error('Failed to add configuration');
    }
  };

  return (
    <div className="site-config-page">
      <div className="page-header">
        <div>
          <h1>Site Configuration</h1>
          <p>Manage site-wide settings and configurations</p>
        </div>
        <button className="btn-primary" onClick={() => setShowForm(true)}>
          <FiPlus /> Add Configuration
        </button>
      </div>

      {/* Quick Add Common Configs */}
      <div className="quick-add-section">
        <h3>Quick Add Common Configurations</h3>
        <div className="quick-add-grid">
          {commonConfigs.map((config) => {
            const exists = configs.some(c => c.configKey === config.key);
            return (
              <div key={config.key} className="quick-add-card">
                <h4>{config.description}</h4>
                <p>Key: {config.key}</p>
                <button
                  className="btn-sm"
                  onClick={() => handleAddCommon(config)}
                  disabled={exists}
                >
                  {exists ? 'Added' : 'Add'}
                </button>
              </div>
            );
          })}
        </div>
      </div>

      {showForm && (
        <div className="config-form-modal">
          <div className="modal-content">
            <h2>{editingConfig ? 'Edit Configuration' : 'Create Configuration'}</h2>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Config Key *</label>
                <input
                  type="text"
                  required
                  value={formData.configKey}
                  onChange={(e) => setFormData({ ...formData, configKey: e.target.value })}
                  disabled={!!editingConfig}
                />
              </div>
              <div className="form-group">
                <label>Config Type *</label>
                <select
                  required
                  value={formData.configType}
                  onChange={(e) => setFormData({ ...formData, configType: e.target.value })}
                >
                  <option value="TEXT">Text</option>
                  <option value="JSON">JSON</option>
                  <option value="BOOLEAN">Boolean</option>
                  <option value="NUMBER">Number</option>
                </select>
              </div>
              <div className="form-group">
                <label>Config Value *</label>
                <textarea
                  rows="4"
                  required
                  value={formData.configValue}
                  onChange={(e) => setFormData({ ...formData, configValue: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Description</label>
                <input
                  type="text"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Display Order</label>
                <input
                  type="number"
                  value={formData.displayOrder}
                  onChange={(e) => setFormData({ ...formData, displayOrder: parseInt(e.target.value) || 0 })}
                />
              </div>
              <div className="form-actions">
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() => {
                    setShowForm(false);
                    setEditingConfig(null);
                  }}
                >
                  Cancel
                </button>
                <button type="submit" className="btn-primary">
                  {editingConfig ? 'Update' : 'Create'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {loading ? (
        <div className="loading">Loading configurations...</div>
      ) : (
        <div className="configs-table-container">
          <table className="configs-table">
            <thead>
              <tr>
                <th>Key</th>
                <th>Value</th>
                <th>Type</th>
                <th>Description</th>
                <th>Order</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {configs.length === 0 ? (
                <tr>
                  <td colSpan="6" className="empty-state">
                    No configurations found
                  </td>
                </tr>
              ) : (
                configs.map((config) => (
                  <tr key={config.id}>
                    <td className="config-key">{config.configKey}</td>
                    <td className="config-value">
                      {config.configType === 'JSON' ? (
                        <pre>{JSON.stringify(JSON.parse(config.configValue || '{}'), null, 2)}</pre>
                      ) : (
                        <span>{config.configValue?.substring(0, 100)}{config.configValue?.length > 100 ? '...' : ''}</span>
                      )}
                    </td>
                    <td>{config.configType}</td>
                    <td>{config.description || '-'}</td>
                    <td>{config.displayOrder}</td>
                    <td>
                      <div className="action-buttons">
                        <button className="btn-icon" onClick={() => handleEdit(config)}>
                          <FiEdit />
                        </button>
                        <button className="btn-icon danger" onClick={() => handleDelete(config.id)}>
                          <FiTrash2 />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default SiteConfig;



