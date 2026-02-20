import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add auth token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('adminToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor to handle errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('adminToken');
      localStorage.removeItem('adminUser');
      window.location.href = '/admin/login';
    }
    return Promise.reject(error);
  }
);

// Auth APIs
export const authAPI = {
  login: (data) => api.post('/auth/login', data),
};

// Admin Dashboard APIs
export const adminAPI = {
  getDashboardStats: () => api.get('/admin/dashboard/stats'),
};

// Product APIs
export const productAPI = {
  getAll: (params) => api.get('/admin/products', { params }),
  getById: (id) => api.get(`/products/${id}`),
  create: (data) => api.post('/admin/products', data),
  update: (id, data) => api.put(`/admin/products/${id}`, data),
  delete: (id) => api.delete(`/admin/products/${id}`),
};

// Category APIs
export const categoryAPI = {
  getAll: () => api.get('/admin/categories'),
  create: (data) => api.post('/admin/categories', data),
  update: (id, data) => api.put(`/admin/categories/${id}`, data),
  delete: (id) => api.delete(`/admin/categories/${id}`),
};

// Order APIs
export const orderAPI = {
  getAll: (params) => api.get('/admin/orders', { params }),
  getById: (id) => api.get(`/admin/orders/${id}`),
  updateStatus: (id, status) => api.put(`/admin/orders/${id}/status`, null, { params: { status } }),
  exportOrders: (format, params) => api.get('/admin/orders/export', {
    params: { format, ...params },
    responseType: 'blob',
  }),
};

// User APIs
// Note: delete() deactivates the user (sets isActive=false) rather than permanently deleting
export const userAPI = {
  getAll: (params) => api.get('/admin/users', { params }),
  getById: (id) => api.get(`/admin/users/${id}`),
  update: (id, data) => api.put(`/admin/users/${id}`, data),
  toggleStatus: (id) => api.put(`/admin/users/${id}/toggle-status`),
  delete: (id) => api.delete(`/admin/users/${id}`),
};

// Site Config APIs (Admin)
export const siteConfigAPI = {
  getAll: () => api.get('/admin/site-config'),
  getByKey: (key) => api.get(`/admin/site-config/${key}`),
  createOrUpdate: (data) => api.post('/admin/site-config', data),
  delete: (id) => api.delete(`/admin/site-config/${id}`),
};

// File Upload API (Admin)
export const uploadAPI = {
  upload: (file, onUploadProgress) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post('/admin/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      onUploadProgress,
    });
  },
};

// Return Request APIs (Admin)
export const returnAPI = {
  getAll: (params) => api.get('/admin/returns', { params }),
  getById: (id) => api.get(`/admin/returns/${id}`),
  approve: (id, notes) => api.post(`/admin/returns/${id}/approve`, null, { params: { adminNotes: notes } }),
  reject: (id, notes) => api.post(`/admin/returns/${id}/reject`, null, { params: { adminNotes: notes } }),
  process: (id) => api.post(`/admin/returns/${id}/process`),
  complete: (id) => api.post(`/admin/returns/${id}/complete`),
};

// Activity Log APIs (Admin)
export const activityLogAPI = {
  getAll: (params) => api.get('/admin/activity-logs', { params }),
  getAdmins: () => api.get('/admin/activity-logs/admins'),
};

// Profile APIs (for logged-in admin)
export const profileAPI = {
  getProfile: () => api.get('/user/profile'),
  updateProfile: (data) => api.put('/user/profile', data),
  changePassword: (data) => api.post('/user/change-password', data),
};

export default api;

