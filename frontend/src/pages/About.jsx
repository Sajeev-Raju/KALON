import SEO, { BASE_URL } from '../components/common/SEO';
import './About.css';

const About = () => {
  return (
    <div className="about-page">
      <SEO
        title="About Us"
        description="Learn about KALON - where fashion meets quality. Founded with a vision to make premium fashion accessible to everyone."
        canonicalUrl={`${BASE_URL}/about`}
      />
      <div className="container">
        <div className="about-hero">
          <h1>About KALON</h1>
          <p className="hero-subtitle">Where Fashion Meets Quality</p>
        </div>

        <section className="about-section">
          <div className="section-content">
            <h2>Our Story</h2>
            <p>
              KALON was born from a simple vision: to make premium fashion accessible to everyone. 
              Founded in 2020, we started as a small team with a big dream - to create clothing that 
              combines style, quality, and affordability.
            </p>
            <p>
              Today, we've grown into one of India's most trusted fashion brands, serving over 1 million 
              happy customers. Our journey has been guided by our core values: quality craftsmanship, 
              sustainable practices, and customer-first approach.
            </p>
          </div>
        </section>

        <section className="values-section">
          <h2>Our Values</h2>
          <div className="values-grid">
            <div className="value-card">
              <h3>Quality First</h3>
              <p>We source the finest materials and work with skilled artisans to ensure every piece meets our high standards of quality.</p>
            </div>
            <div className="value-card">
              <h3>Sustainability</h3>
              <p>We're committed to sustainable fashion practices, from eco-friendly materials to ethical manufacturing processes.</p>
            </div>
            <div className="value-card">
              <h3>Customer Focus</h3>
              <p>Your satisfaction is our priority. We listen to your feedback and continuously improve our products and services.</p>
            </div>
            <div className="value-card">
              <h3>Innovation</h3>
              <p>We stay ahead of fashion trends and embrace innovation in design, technology, and customer experience.</p>
            </div>
          </div>
        </section>

        <section className="mission-section">
          <div className="mission-content">
            <h2>Our Mission</h2>
            <p>
              To empower people to express their unique style through high-quality, affordable fashion 
              that doesn't compromise on ethics or the environment.
            </p>
          </div>
        </section>

        <section className="stats-section">
          <h2>KALON by the Numbers</h2>
          <div className="stats-grid">
            <div className="stat-card">
              <div className="stat-number">1M+</div>
              <div className="stat-label">Happy Customers</div>
            </div>
            <div className="stat-card">
              <div className="stat-number">10K+</div>
              <div className="stat-label">Products</div>
            </div>
            <div className="stat-card">
              <div className="stat-number">500+</div>
              <div className="stat-label">Cities Served</div>
            </div>
            <div className="stat-card">
              <div className="stat-number">4.8★</div>
              <div className="stat-label">Average Rating</div>
            </div>
          </div>
        </section>

        <section className="team-section">
          <h2>Join Our Journey</h2>
          <p>
            We're always looking for passionate individuals to join our team. If you share our values 
            and want to make a difference in the fashion industry, we'd love to hear from you.
          </p>
          <a href="/careers" className="btn btn-primary">View Careers</a>
        </section>
      </div>
    </div>
  );
};

export default About;



