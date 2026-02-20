import SEO, { BASE_URL } from '../components/common/SEO';
import './Careers.css';

const Careers = () => {
  const positions = [
    {
      title: 'Senior Fashion Designer',
      department: 'Design',
      location: 'Mumbai, India',
      type: 'Full-time',
      description: 'We are looking for a creative Senior Fashion Designer to join our design team and help shape the future of KALON.',
    },
    {
      title: 'Digital Marketing Manager',
      department: 'Marketing',
      location: 'Remote',
      type: 'Full-time',
      description: 'Lead our digital marketing efforts and help grow our brand presence across all digital channels.',
    },
    {
      title: 'Customer Service Representative',
      department: 'Customer Service',
      location: 'Delhi, India',
      type: 'Full-time',
      description: 'Join our customer service team and help provide exceptional support to our valued customers.',
    },
    {
      title: 'Software Developer',
      department: 'Technology',
      location: 'Bangalore, India',
      type: 'Full-time',
      description: 'Build and maintain our e-commerce platform and internal tools using modern web technologies.',
    },
  ];

  return (
    <div className="careers-page">
      <SEO
        title="Careers"
        description="Join the KALON team. Explore open positions and be part of a team revolutionizing fashion."
        canonicalUrl={`${BASE_URL}/careers`}
      />
      <div className="container">
        <div className="careers-hero">
          <h1>Join the KALON Team</h1>
          <p className="hero-subtitle">Be part of a team that's revolutionizing fashion</p>
        </div>

        <section className="why-join-section">
          <h2>Why Work at KALON?</h2>
          <div className="benefits-grid">
            <div className="benefit-card">
              <h3>Growth Opportunities</h3>
              <p>We invest in our employees' growth with training programs and career development opportunities.</p>
            </div>
            <div className="benefit-card">
              <h3>Creative Environment</h3>
              <p>Work in a collaborative, innovative environment where your ideas are valued and encouraged.</p>
            </div>
            <div className="benefit-card">
              <h3>Competitive Benefits</h3>
              <p>Enjoy competitive salaries, health insurance, and other comprehensive benefits packages.</p>
            </div>
            <div className="benefit-card">
              <h3>Work-Life Balance</h3>
              <p>We believe in maintaining a healthy work-life balance with flexible working arrangements.</p>
            </div>
          </div>
        </section>

        <section className="open-positions-section">
          <h2>Open Positions</h2>
          <div className="positions-list">
            {positions.map((position, index) => (
              <div key={index} className="position-card">
                <div className="position-header">
                  <div>
                    <h3>{position.title}</h3>
                    <div className="position-meta">
                      <span className="meta-item">{position.department}</span>
                      <span className="meta-item">•</span>
                      <span className="meta-item">{position.location}</span>
                      <span className="meta-item">•</span>
                      <span className="meta-item">{position.type}</span>
                    </div>
                  </div>
                  <button className="btn btn-primary">Apply Now</button>
                </div>
                <p className="position-description">{position.description}</p>
              </div>
            ))}
          </div>
        </section>

        <section className="contact-section">
          <h2>Don't See a Position That Fits?</h2>
          <p>We're always looking for talented individuals. Send us your resume and we'll keep you in mind for future opportunities.</p>
          <a href="/contact" className="btn btn-primary">Send Us Your Resume</a>
        </section>
      </div>
    </div>
  );
};

export default Careers;



