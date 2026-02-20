import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import ProductCard from '../common/ProductCard';
import { productAPI } from '../../services/api';
import './ProductSection.css';

const ProductSection = ({ title, type, viewAllLink }) => {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchProducts = async () => {
      try {
        let response;
        switch (type) {
          case 'featured':
            response = await productAPI.getFeatured();
            break;
          case 'new-arrivals':
            response = await productAPI.getNewArrivals();
            break;
          case 'best-sellers':
            response = await productAPI.getBestSellers();
            break;
          default:
            response = await productAPI.getAll({ page: 0, size: 8 });
        }
        setProducts(response.data.data || []);
      } catch (error) {
        console.error('Error fetching products:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchProducts();
  }, [type]);

  if (loading) {
    return (
      <section className="product-section">
        <div className="container">
          <h2 className="section-title">{title}</h2>
          <div className="product-grid loading">
            {[...Array(4)].map((_, index) => (
              <div key={index} className="product-skeleton">
                <div className="skeleton-image" />
                <div className="skeleton-text" />
                <div className="skeleton-text short" />
              </div>
            ))}
          </div>
        </div>
      </section>
    );
  }

  if (products.length === 0) {
    return null;
  }

  return (
    <section className="product-section">
      <div className="container">
        <div className="section-header">
          <h2 className="section-title">{title}</h2>
          {viewAllLink && (
            <Link to={viewAllLink} className="view-all-link">
              View All →
            </Link>
          )}
        </div>
        <div className="product-grid">
          {products.slice(0, 8).map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      </div>
    </section>
  );
};

export default ProductSection;
