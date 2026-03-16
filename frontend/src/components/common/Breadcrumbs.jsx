import { Link } from 'react-router-dom';
import { FiChevronRight } from 'react-icons/fi';
import './Breadcrumbs.css';

const Breadcrumbs = ({ items }) => {
  if (!items || items.length === 0) return null;

  return (
    <nav className="breadcrumbs" aria-label="Breadcrumb">
      <ol className="breadcrumbs-list">
        <li className="breadcrumbs-item">
          <Link to="/">Home</Link>
        </li>
        {items.map((item, index) => (
          <li key={index} className="breadcrumbs-item">
            <FiChevronRight className="breadcrumbs-separator" />
            {index === items.length - 1 ? (
              <span className="breadcrumbs-current" aria-current="page">{item.label}</span>
            ) : (
              <Link to={item.path}>{item.label}</Link>
            )}
          </li>
        ))}
      </ol>
    </nav>
  );
};

export default Breadcrumbs;
