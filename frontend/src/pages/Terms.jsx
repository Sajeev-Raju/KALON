import SEO, { BASE_URL } from '../components/common/SEO';
import './Terms.css';

const Terms = () => {
  return (
    <div className="terms-page">
      <SEO
        title="Terms & Conditions"
        description="Read the terms and conditions for using the KALON website and services."
        canonicalUrl={`${BASE_URL}/terms`}
      />
      <div className="container">
        <div className="terms-header">
          <h1>Terms & Conditions</h1>
          <p className="last-updated">Last Updated: January 1, 2024</p>
        </div>

        <div className="terms-content">
          <section>
            <h2>1. Agreement to Terms</h2>
            <p>
              By accessing and using the KALON website and services, you accept and agree to be bound by the 
              terms and provision of this agreement. If you do not agree to abide by the above, please do not 
              use this service.
            </p>
          </section>

          <section>
            <h2>2. Use License</h2>
            <p>
              Permission is granted to temporarily access the materials on KALON's website for personal, 
              non-commercial transitory viewing only. This is the grant of a license, not a transfer of title, 
              and under this license you may not:
            </p>
            <ul>
              <li>Modify or copy the materials</li>
              <li>Use the materials for any commercial purpose or for any public display</li>
              <li>Attempt to reverse engineer any software contained on the website</li>
              <li>Remove any copyright or other proprietary notations from the materials</li>
            </ul>
          </section>

          <section>
            <h2>3. Products and Services</h2>
            <p>
              We strive to provide accurate descriptions of our products. However, we do not warrant that 
              product descriptions or other content on this site is accurate, complete, reliable, current, 
              or error-free. If a product offered by us is not as described, your sole remedy is to return 
              it in unused condition.
            </p>
          </section>

          <section>
            <h2>4. Pricing and Payment</h2>
            <p>
              All prices are listed in Indian Rupees (INR) and are subject to change without notice. We 
              reserve the right to modify prices at any time. Payment must be received before we ship your 
              order. We accept all major credit cards, debit cards, UPI, net banking, and cash on delivery.
            </p>
          </section>

          <section>
            <h2>5. Returns and Refunds</h2>
            <p>
              Please refer to our Returns & Refunds policy for detailed information about our return and 
              refund procedures. Items must be returned within 30 days of delivery in their original condition 
              with tags attached.
            </p>
          </section>

          <section>
            <h2>6. User Accounts</h2>
            <p>
              When you create an account with us, you must provide accurate, complete, and current information. 
              You are responsible for safeguarding the password and for all activities that occur under your 
              account. You must notify us immediately of any unauthorized use of your account.
            </p>
          </section>

          <section>
            <h2>7. Intellectual Property</h2>
            <p>
              The content on this website, including but not limited to text, graphics, logos, images, and 
              software, is the property of KALON or its content suppliers and is protected by copyright and 
              other intellectual property laws.
            </p>
          </section>

          <section>
            <h2>8. Limitation of Liability</h2>
            <p>
              In no event shall KALON or its suppliers be liable for any damages (including, without limitation, 
              damages for loss of data or profit, or due to business interruption) arising out of the use or 
              inability to use the materials on KALON's website.
            </p>
          </section>

          <section>
            <h2>9. Governing Law</h2>
            <p>
              These terms and conditions are governed by and construed in accordance with the laws of India, 
              and you irrevocably submit to the exclusive jurisdiction of the courts in Mumbai, Maharashtra.
            </p>
          </section>

          <section>
            <h2>10. Changes to Terms</h2>
            <p>
              We reserve the right to revise these terms of service at any time without notice. By using this 
              website you are agreeing to be bound by the then current version of these terms of service.
            </p>
          </section>

          <section>
            <h2>11. Contact Information</h2>
            <p>
              If you have any questions about these Terms & Conditions, please contact us at:
            </p>
            <p>
              <strong>Email:</strong> legal@kalon.com<br />
              <strong>Phone:</strong> +91 1800-123-4567<br />
              <strong>Address:</strong> 123 Fashion Street, Mumbai, Maharashtra 400001, India
            </p>
          </section>
        </div>
      </div>
    </div>
  );
};

export default Terms;



