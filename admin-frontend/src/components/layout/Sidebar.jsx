import { NavLink } from 'react-router-dom';
import {
  FiLayout,
  FiPackage,
  FiFolder,
  FiShoppingCart,
  FiUsers,
  FiSettings,
  FiLogOut,
  FiRefreshCw,
  FiActivity,
  FiUser,
} from 'react-icons/fi';
import { useDispatch } from 'react-redux';
import { logout } from '../../redux/slices/authSlice';
import { useNavigate } from 'react-router-dom';
import './Sidebar.css';

const Sidebar = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();

  const handleLogout = () => {
    dispatch(logout());
    navigate('/admin/login');
  };

  const menuItems = [
    { path: '/admin/dashboard', icon: FiLayout, label: 'Dashboard' },
    { path: '/admin/products', icon: FiPackage, label: 'Products' },
    { path: '/admin/categories', icon: FiFolder, label: 'Categories' },
    { path: '/admin/orders', icon: FiShoppingCart, label: 'Orders' },
    { path: '/admin/returns', icon: FiRefreshCw, label: 'Returns' },
    { path: '/admin/users', icon: FiUsers, label: 'Users' },
    { path: '/admin/activity-logs', icon: FiActivity, label: 'Activity Logs' },
    { path: '/admin/site-config', icon: FiSettings, label: 'Site Config' },
    { path: '/admin/profile', icon: FiUser, label: 'My Profile' },
  ];

  return (
    <aside className="sidebar">
      <div className="sidebar-header">
        <h1>KALON</h1>
        <span className="sidebar-subtitle">Admin Panel</span>
      </div>
      
      <nav className="sidebar-nav">
        {menuItems.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) =>
                `nav-link ${isActive ? 'active' : ''}`
              }
            >
              <Icon className="nav-icon" />
              <span>{item.label}</span>
            </NavLink>
          );
        })}
      </nav>
      
      <div className="sidebar-footer">
        <button onClick={handleLogout} className="logout-button">
          <FiLogOut className="nav-icon" />
          <span>Logout</span>
        </button>
      </div>
    </aside>
  );
};

export default Sidebar;

