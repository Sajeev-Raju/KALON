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
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Track whether a token refresh is in progress to avoid concurrent refreshes
let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

// Response interceptor with automatic token refresh
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // If 401 and we haven't already retried this request
    if (error.response?.status === 401 && !originalRequest._retry) {
      // Don't try to refresh if the failing request IS the refresh or login request
      if (originalRequest.url?.includes('/auth/refresh') ||
          originalRequest.url?.includes('/auth/login') ||
          originalRequest.url?.includes('/auth/register') ||
          originalRequest.url?.includes('/auth/google')) {
        return Promise.reject(error);
      }

      if (isRefreshing) {
        // Queue this request until the refresh completes
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((token) => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            return api(originalRequest);
          })
          .catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      const refreshToken = localStorage.getItem('refreshToken');
      if (!refreshToken) {
        isRefreshing = false;
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
        window.location.href = '/login';
        return Promise.reject(error);
      }

      try {
        const response = await axios.post(`${API_BASE_URL}/auth/refresh`, {
          refreshToken,
        });
        const { data } = response.data;
        const newToken = data.token;
        const newRefreshToken = data.refreshToken;

        localStorage.setItem('token', newToken);
        localStorage.setItem('refreshToken', newRefreshToken);
        // Update stored user data
        localStorage.setItem('user', JSON.stringify(data));

        api.defaults.headers.common.Authorization = `Bearer ${newToken}`;
        originalRequest.headers.Authorization = `Bearer ${newToken}`;

        processQueue(null, newToken);
        return api(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
        window.location.href = '/login';
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

// Auth APIs
export const authAPI = {
  login: (data) => api.post('/auth/login', data),
  register: (data) => api.post('/auth/register', data),
  loginWithGoogle: (data) => api.post('/auth/google', data),
  refresh: (refreshToken) => api.post('/auth/refresh', { refreshToken }),
  logout: (refreshToken) => api.post('/auth/logout', refreshToken ? { refreshToken } : null),
  forgotPassword: (data) => api.post('/auth/forgot-password', data),
  resetPassword: (data) => api.post('/auth/reset-password', data),
};

// Product APIs
export const productAPI = {
  getAll: (params) => api.get('/products', { params }),
  getById: (id) => api.get(`/products/${id}`),
  getBySlug: (slug) => api.get(`/products/slug/${slug}`),
  getByCategory: (categoryId, params) => api.get(`/products/category/${categoryId}`, { params }),
  getByCategorySlug: (slug, params) => api.get(`/products/category/slug/${slug}`, { params }),
  getByGender: (gender, params) => api.get(`/products/gender/${gender}`, { params }),
  search: (keyword, params) => api.get('/products/search', { params: { keyword, ...params } }),
  getFeatured: () => api.get('/products/featured'),
  getNewArrivals: () => api.get('/products/new-arrivals'),
  getBestSellers: () => api.get('/products/best-sellers'),
  filter: (params) => api.get('/products/filter', { params }),
  // New unified search and filter endpoint
  searchAndFilter: (params) => api.get('/products/search-filter', { params }),
  // Get available filter options
  getFilterOptions: () => api.get('/products/filter-options'),
  // Get faceted filter options with counts
  getFacetedFilterOptions: () => api.get('/products/faceted-filter-options'),
  // Get search suggestions for autocomplete
  getSuggestions: (keyword) => api.get('/products/suggestions', { params: { keyword } }),
  // Advanced search with full-text and fuzzy matching
  advancedSearch: (keyword, params) => api.get('/products/advanced-search', { params: { keyword, ...params } }),
};

// Category APIs
export const categoryAPI = {
  getAll: () => api.get('/categories'),
  getRoot: () => api.get('/categories/root'),
  getById: (id) => api.get(`/categories/${id}`),
  getBySlug: (slug) => api.get(`/categories/slug/${slug}`),
  getByGender: (gender) => api.get(`/categories/gender/${gender}`),
  getSubcategories: (parentId) => api.get(`/categories/${parentId}/subcategories`),
};

// Cart APIs
export const cartAPI = {
  get: () => api.get('/cart'),
  add: (data) => api.post('/cart/add', data),
  updateQuantity: (itemId, quantity) => api.put(`/cart/items/${itemId}`, null, { params: { quantity } }),
  remove: (itemId) => api.delete(`/cart/items/${itemId}`),
  clear: () => api.delete('/cart/clear'),
  validate: () => api.get('/cart/validate'),
};

// Wishlist APIs
export const wishlistAPI = {
  get: () => api.get('/wishlist'),
  check: (productId) => api.get(`/wishlist/check/${productId}`),
  add: (productId) => api.post(`/wishlist/add/${productId}`),
  remove: (productId) => api.delete(`/wishlist/remove/${productId}`),
  toggle: (productId) => api.post(`/wishlist/toggle/${productId}`),
};

// Order APIs
export const orderAPI = {
  getAll: () => api.get('/orders'),
  getPaginated: (params) => api.get('/orders/paginated', { params }),
  getById: (orderId) => api.get(`/orders/${orderId}`),
  track: (orderNumber) => api.get(`/orders/track/${orderNumber}`),
  create: (data) => api.post('/orders', data),
  cancel: (orderId) => api.post(`/orders/${orderId}/cancel`),
  retryPayment: (orderId) => api.post(`/orders/${orderId}/retry-payment`),
  getPaymentAttempts: (orderId) => api.get(`/orders/${orderId}/payment-attempts`),
  getPaymentMethods: () => api.get('/orders/payment-methods'),
  createRazorpayOrder: (data) => api.post('/orders/razorpay/create', data),
  verifyRazorpayPayment: (data) => api.post('/orders/razorpay/verify', data),
};

// Return Request APIs
export const returnAPI = {
  create: (data) => api.post('/returns', data),
  getAll: () => api.get('/returns'),
  getById: (returnRequestId) => api.get(`/returns/${returnRequestId}`),
  cancel: (returnRequestId) => api.post(`/returns/${returnRequestId}/cancel`),
};

// Address APIs
export const addressAPI = {
  getAll: () => api.get('/addresses'),
  getById: (addressId) => api.get(`/addresses/${addressId}`),
  create: (data) => api.post('/addresses', data),
  update: (addressId, data) => api.put(`/addresses/${addressId}`, data),
  delete: (addressId) => api.delete(`/addresses/${addressId}`),
};

// Review APIs
export const reviewAPI = {
  getProductReviews: (productId, params) => api.get(`/reviews/product/${productId}`, { params }),
  getUserReviews: () => api.get('/reviews/user'),
  getUserReviewForProduct: (productId) => api.get(`/reviews/user/product/${productId}`),
  create: (data) => api.post('/reviews', data),
  update: (reviewId, data) => api.put(`/reviews/${reviewId}`, data),
  delete: (reviewId) => api.delete(`/reviews/${reviewId}`),
};

// User Profile APIs
export const userAPI = {
  getProfile: () => api.get('/user/profile'),
  updateProfile: (data) => api.put('/user/profile', data),
  changePassword: (data) => api.post('/user/change-password', data),
};

// Site Config APIs (public)
export const siteConfigAPI = {
  getActive: () => api.get('/site-config'),
  getByKey: (key) => api.get(`/site-config/${key}`),
};

export default api;
