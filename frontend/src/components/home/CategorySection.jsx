import { Link } from 'react-router-dom';
import './CategorySection.css';

const categories = [
  {
    id: 1,
    name: 'T-Shirts',
    slug: 'men-t-shirts',
    image: 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=400',
    gender: 'men',
  },
  {
    id: 2,
    name: 'Hoodies',
    slug: 'men-hoodies',
    image: 'https://images.unsplash.com/photo-1556821840-3a63f95609a7?w=400',
    gender: 'men',
  },
  {
    id: 3,
    name: 'Joggers',
    slug: 'men-joggers',
    image: 'https://images.unsplash.com/photo-1552902865-b72c031ac5ea?w=400',
    gender: 'men',
  },
  {
    id: 4,
    name: 'Shirts',
    slug: 'men-shirts',
    image: 'https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=400',
    gender: 'men',
  },
  {
    id: 5,
    name: 'Dresses',
    slug: 'women-dresses',
    image: 'https://images.unsplash.com/photo-1595777457583-95e059d581b8?w=400',
    gender: 'women',
  },
  {
    id: 6,
    name: 'Sneakers',
    slug: 'sneakers',
    image: 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=400',
    gender: 'unisex',
  },
];

const CategorySection = () => {
  return (
    <section className="category-section">
      <div className="container">
        <h2 className="section-title">Shop By Category</h2>
        <div className="category-grid">
          {categories.map((category) => (
            <Link
              key={category.id}
              to={`/category/${category.slug}`}
              className="category-card"
            >
              <div className="category-image">
                <img src={category.image} alt={category.name} />
              </div>
              <div className="category-name">
                <span>{category.name}</span>
              </div>
            </Link>
          ))}
        </div>
      </div>
    </section>
  );
};

export default CategorySection;
