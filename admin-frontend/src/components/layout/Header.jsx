import { useSelector } from 'react-redux';
import { Link } from 'react-router-dom';
import './Header.css';

const Header = () => {
  const { user } = useSelector((state) => state.auth);

  return (
    <header className="admin-header">
      <div className="header-content">
        <h2 className="page-title">Admin Dashboard</h2>
        <Link to="/admin/profile" className="header-user">
          <div className="user-info">
            <span className="user-name">{user?.firstName} {user?.lastName}</span>
            <span className="user-role">Administrator</span>
          </div>
          <div className="user-avatar">
            {user?.firstName?.charAt(0)}{user?.lastName?.charAt(0)}
          </div>
        </Link>
      </div>
    </header>
  );
};

export default Header;



