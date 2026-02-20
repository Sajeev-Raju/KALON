import { useEffect, useState } from 'react';
import { adminAPI } from '../services/api';
import { FiUsers, FiPackage, FiShoppingCart, FiDollarSign, FiTrendingUp, FiAlertCircle } from 'react-icons/fi';
import { LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import toast from 'react-hot-toast';
import './Dashboard.css';

const Dashboard = () => {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchStats();
  }, []);

  const fetchStats = async () => {
    try {
      const response = await adminAPI.getDashboardStats();
      setStats(response.data.data);
    } catch (error) {
      toast.error('Failed to load dashboard stats');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <div className="dashboard-loading">Loading dashboard...</div>;
  }

  if (!stats) {
    return <div className="dashboard-error">Failed to load statistics</div>;
  }

  let revenueData = [];
  try {
    revenueData = Object.entries(stats.revenueByMonth || {}).map(([month, revenue]) => ({
      month: typeof month === 'string' ? month.substring(0, 3) : '',
      revenue: revenue != null && !isNaN(parseFloat(revenue)) ? parseFloat(revenue) : 0,
    }));
  } catch (e) {
    console.error('Error parsing revenue data:', e);
  }

  let ordersData = [];
  try {
    ordersData = Object.entries(stats.ordersByStatus || {}).map(([status, count]) => ({
      status: status || 'Unknown',
      count: typeof count === 'number' ? count : 0,
    }));
  } catch (e) {
    console.error('Error parsing orders data:', e);
  }

  const statCards = [
    {
      title: 'Total Users',
      value: stats.totalUsers || 0,
      icon: FiUsers,
      color: '#667eea',
    },
    {
      title: 'Total Products',
      value: stats.totalProducts || 0,
      icon: FiPackage,
      color: '#48bb78',
    },
    {
      title: 'Total Orders',
      value: stats.totalOrders || 0,
      icon: FiShoppingCart,
      color: '#ed8936',
    },
    {
      title: 'Total Revenue',
      value: `₹${(Number(stats.totalRevenue) || 0).toLocaleString()}`,
      icon: FiDollarSign,
      color: '#38b2ac',
    },
    {
      title: 'Today Revenue',
      value: `₹${(Number(stats.todayRevenue) || 0).toLocaleString()}`,
      icon: FiTrendingUp,
      color: '#9f7aea',
    },
    {
      title: 'Low Stock Items',
      value: stats.lowStockProducts || 0,
      icon: FiAlertCircle,
      color: '#f56565',
    },
  ];

  return (
    <div className="dashboard">
      <div className="dashboard-header">
        <h1>Dashboard Overview</h1>
        <p>Welcome to KALON Admin Panel</p>
      </div>

      <div className="stats-grid">
        {statCards.map((card, index) => {
          const Icon = card.icon;
          return (
            <div key={index} className="stat-card">
              <div className="stat-icon" style={{ background: `${card.color}20`, color: card.color }}>
                <Icon />
              </div>
              <div className="stat-content">
                <h3>{card.value}</h3>
                <p>{card.title}</p>
              </div>
            </div>
          );
        })}
      </div>

      <div className="dashboard-charts">
        <div className="chart-card">
          <h3>Revenue Trend (Last 6 Months)</h3>
          {revenueData.length > 0 ? (
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={revenueData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="month" />
                <YAxis />
                <Tooltip formatter={(value) => `₹${typeof value === 'number' ? value.toLocaleString() : '0'}`} />
                <Legend />
                <Line type="monotone" dataKey="revenue" stroke="#667eea" strokeWidth={2} />
              </LineChart>
            </ResponsiveContainer>
          ) : (
            <div style={{ padding: '40px', textAlign: 'center', color: '#999' }}>
              No revenue data available
            </div>
          )}
        </div>

        <div className="chart-card">
          <h3>Orders by Status</h3>
          {ordersData.length > 0 ? (
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={ordersData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="status" />
                <YAxis />
                <Tooltip />
                <Legend />
                <Bar dataKey="count" fill="#48bb78" />
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <div style={{ padding: '40px', textAlign: 'center', color: '#999' }}>
              No order data available
            </div>
          )}
        </div>
      </div>

      <div className="dashboard-info">
        <div className="info-card">
          <h3>Quick Stats</h3>
          <div className="info-item">
            <span>Pending Orders:</span>
            <strong>{stats.pendingOrders || 0}</strong>
          </div>
          <div className="info-item">
            <span>Completed Orders:</span>
            <strong>{stats.completedOrders || 0}</strong>
          </div>
          <div className="info-item">
            <span>This Month Revenue:</span>
            <strong>₹{(Number(stats.thisMonthRevenue) || 0).toLocaleString()}</strong>
          </div>
          <div className="info-item">
            <span>Total Categories:</span>
            <strong>{stats.totalCategories || 0}</strong>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;

