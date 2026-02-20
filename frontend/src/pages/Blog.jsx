import { Link } from 'react-router-dom';
import SEO, { BASE_URL } from '../components/common/SEO';
import './Blog.css';

const Blog = () => {
  const posts = [
    {
      id: 1,
      title: 'Top 10 Fashion Trends for 2024',
      excerpt: 'Discover the latest fashion trends that are shaping the industry this year. From sustainable fashion to bold colors...',
      author: 'KALON Team',
      date: 'January 15, 2024',
      category: 'Fashion',
      image: 'https://via.placeholder.com/400x250?text=Fashion+Trends',
    },
    {
      id: 2,
      title: 'How to Build a Sustainable Wardrobe',
      excerpt: 'Learn how to create a sustainable wardrobe that doesn\'t compromise on style. Tips for conscious fashion choices...',
      author: 'Sarah Johnson',
      date: 'January 10, 2024',
      category: 'Sustainability',
      image: 'https://via.placeholder.com/400x250?text=Sustainable+Fashion',
    },
    {
      id: 3,
      title: 'Styling Tips: Dressing for Your Body Type',
      excerpt: 'Every body is beautiful. Learn how to dress in a way that makes you feel confident and comfortable...',
      author: 'KALON Team',
      date: 'January 5, 2024',
      category: 'Style Guide',
      image: 'https://via.placeholder.com/400x250?text=Style+Guide',
    },
    {
      id: 4,
      title: 'The History of Streetwear Culture',
      excerpt: 'Explore the evolution of streetwear from its origins to becoming a mainstream fashion movement...',
      author: 'Mike Chen',
      date: 'December 28, 2023',
      category: 'Culture',
      image: 'https://via.placeholder.com/400x250?text=Streetwear',
    },
  ];

  return (
    <div className="blog-page">
      <SEO
        title="Blog"
        description="Fashion insights, style tips, and the latest trends from KALON."
        canonicalUrl={`${BASE_URL}/blog`}
      />
      <div className="container">
        <div className="blog-header">
          <h1>KALON Blog</h1>
          <p>Fashion insights, style tips, and latest trends</p>
        </div>

        <div className="blog-grid">
          {posts.map((post) => (
            <article key={post.id} className="blog-card">
              <div className="blog-image">
                <img src={post.image} alt={post.title} />
                <span className="blog-category">{post.category}</span>
              </div>
              <div className="blog-content">
                <div className="blog-meta">
                  <span className="blog-author">{post.author}</span>
                  <span className="blog-date">{post.date}</span>
                </div>
                <h2 className="blog-title">{post.title}</h2>
                <p className="blog-excerpt">{post.excerpt}</p>
                <Link to={`/blog/${post.id}`} className="read-more">
                  Read More →
                </Link>
              </div>
            </article>
          ))}
        </div>

        <div className="blog-newsletter">
          <h2>Subscribe to Our Newsletter</h2>
          <p>Get the latest fashion tips, exclusive offers, and blog updates delivered to your inbox.</p>
          <form className="newsletter-form">
            <input type="email" placeholder="Enter your email" />
            <button type="submit" className="btn btn-primary">Subscribe</button>
          </form>
        </div>
      </div>
    </div>
  );
};

export default Blog;



