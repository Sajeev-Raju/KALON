export const ORDER_STATUS_COLORS = {
  PENDING: '#ed8936',
  CONFIRMED: '#4299e1',
  PROCESSING: '#667eea',
  SHIPPED: '#48bb78',
  DELIVERED: '#38b2ac',
  CANCELLED: '#f56565',
  RETURNED: '#9f7aea',
};

export const RETURN_STATUS_COLORS = {
  PENDING: '#ed8936',
  APPROVED: '#48bb78',
  REJECTED: '#f56565',
  PROCESSING: '#4299e1',
  COMPLETED: '#38b2ac',
};

export const getOrderStatusColor = (status) => {
  return ORDER_STATUS_COLORS[status] || '#666';
};

export const getReturnStatusColor = (status) => {
  return RETURN_STATUS_COLORS[status] || '#666';
};
