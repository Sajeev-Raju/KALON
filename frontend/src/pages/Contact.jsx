import { useState } from 'react';
import { FiMail, FiPhone, FiMapPin, FiSend } from 'react-icons/fi';
import toast from 'react-hot-toast';
import SEO, { BASE_URL } from '../components/common/SEO';
import './Contact.css';

const Contact = () => {
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    subject: '',
    message: '',
  });

  const handleSubmit = (e) => {
    e.preventDefault();
    toast.success('Thank you for contacting us! We will get back to you soon.');
    setFormData({ name: '', email: '', subject: '', message: '' });
  };

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  return (
    <div className="contact-page">
      <SEO
        title="Contact Us"
        description="Get in touch with KALON. We'd love to hear from you. Reach us via email, phone, or send a message."
        canonicalUrl={`${BASE_URL}/contact`}
      />
      <div className="container">
        <div className="contact-header">
          <h1>Contact Us</h1>
          <p>We'd love to hear from you. Send us a message and we'll respond as soon as possible.</p>
        </div>

        <div className="contact-content">
          <div className="contact-info">
            <div className="info-card">
              <FiMail className="info-icon" />
              <h3>Email</h3>
              <p>support@kalon.com</p>
              <p>info@kalon.com</p>
            </div>
            <div className="info-card">
              <FiPhone className="info-icon" />
              <h3>Phone</h3>
              <p>+91 1800-123-4567</p>
              <p>Mon - Sat, 9:00 AM - 6:00 PM</p>
            </div>
            <div className="info-card">
              <FiMapPin className="info-icon" />
              <h3>Address</h3>
              <p>123 Fashion Street</p>
              <p>Mumbai, Maharashtra 400001</p>
              <p>India</p>
            </div>
          </div>

          <div className="contact-form-container">
            <form className="contact-form" onSubmit={handleSubmit}>
              <div className="form-group">
                <label htmlFor="name">Your Name *</label>
                <input
                  type="text"
                  id="name"
                  name="name"
                  required
                  value={formData.name}
                  onChange={handleChange}
                  placeholder="Enter your name"
                />
              </div>
              <div className="form-group">
                <label htmlFor="email">Your Email *</label>
                <input
                  type="email"
                  id="email"
                  name="email"
                  required
                  value={formData.email}
                  onChange={handleChange}
                  placeholder="Enter your email"
                />
              </div>
              <div className="form-group">
                <label htmlFor="subject">Subject *</label>
                <input
                  type="text"
                  id="subject"
                  name="subject"
                  required
                  value={formData.subject}
                  onChange={handleChange}
                  placeholder="What is this regarding?"
                />
              </div>
              <div className="form-group">
                <label htmlFor="message">Message *</label>
                <textarea
                  id="message"
                  name="message"
                  rows="6"
                  required
                  value={formData.message}
                  onChange={handleChange}
                  placeholder="Tell us more..."
                />
              </div>
              <button type="submit" className="btn btn-primary">
                <FiSend /> Send Message
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Contact;



