import SEO, { BASE_URL } from '../components/common/SEO';
import './Privacy.css';

const Privacy = () => {
  return (
    <div className="privacy-page">
      <SEO
        title="Privacy Policy"
        description="Learn how KALON collects, uses, and protects your personal information."
        canonicalUrl={`${BASE_URL}/privacy`}
      />
      <div className="container">
        <div className="privacy-header">
          <h1>Privacy Policy</h1>
          <p className="last-updated">Last Updated: January 1, 2024</p>
        </div>

        <div className="privacy-content">
          <section>
            <h2>1. Introduction</h2>
            <p>
              At KALON, we respect your privacy and are committed to protecting your personal data. This 
              privacy policy explains how we collect, use, and safeguard your information when you visit 
              our website or use our services.
            </p>
          </section>

          <section>
            <h2>2. Information We Collect</h2>
            <p>We collect several types of information from and about users of our website:</p>
            <ul>
              <li><strong>Personal Information:</strong> Name, email address, phone number, shipping address, billing address, payment information</li>
              <li><strong>Account Information:</strong> Username, password, account preferences</li>
              <li><strong>Order Information:</strong> Order history, purchase details, product preferences</li>
              <li><strong>Technical Information:</strong> IP address, browser type, device information, browsing behavior</li>
              <li><strong>Communication Data:</strong> Correspondence with our customer service team</li>
            </ul>
          </section>

          <section>
            <h2>3. How We Use Your Information</h2>
            <p>We use the information we collect for various purposes:</p>
            <ul>
              <li>To process and fulfill your orders</li>
              <li>To communicate with you about your orders, products, and services</li>
              <li>To provide customer support and respond to your inquiries</li>
              <li>To send you marketing communications (with your consent)</li>
              <li>To improve our website, products, and services</li>
              <li>To detect and prevent fraud and abuse</li>
              <li>To comply with legal obligations</li>
            </ul>
          </section>

          <section>
            <h2>4. How We Share Your Information</h2>
            <p>
              We do not sell your personal information. We may share your information with:
            </p>
            <ul>
              <li><strong>Service Providers:</strong> Third-party vendors who perform services on our behalf (payment processing, shipping, email delivery)</li>
              <li><strong>Business Partners:</strong> Trusted partners who help us operate our business</li>
              <li><strong>Legal Requirements:</strong> When required by law or to protect our rights and safety</li>
              <li><strong>Business Transfers:</strong> In connection with a merger, acquisition, or sale of assets</li>
            </ul>
          </section>

          <section>
            <h2>5. Data Security</h2>
            <p>
              We implement appropriate technical and organizational measures to protect your personal data 
              against unauthorized access, alteration, disclosure, or destruction. However, no method of 
              transmission over the internet is 100% secure, and we cannot guarantee absolute security.
            </p>
          </section>

          <section>
            <h2>6. Your Rights</h2>
            <p>You have the following rights regarding your personal data:</p>
            <ul>
              <li><strong>Access:</strong> Request access to your personal data</li>
              <li><strong>Correction:</strong> Request correction of inaccurate data</li>
              <li><strong>Deletion:</strong> Request deletion of your personal data</li>
              <li><strong>Objection:</strong> Object to processing of your personal data</li>
              <li><strong>Data Portability:</strong> Request transfer of your data to another service</li>
              <li><strong>Withdraw Consent:</strong> Withdraw consent for data processing</li>
            </ul>
          </section>

          <section>
            <h2>7. Cookies and Tracking Technologies</h2>
            <p>
              We use cookies and similar tracking technologies to track activity on our website and hold 
              certain information. You can instruct your browser to refuse all cookies or to indicate when 
              a cookie is being sent.
            </p>
          </section>

          <section>
            <h2>8. Children's Privacy</h2>
            <p>
              Our services are not intended for individuals under the age of 18. We do not knowingly collect 
              personal information from children. If we become aware that we have collected personal information 
              from a child, we will take steps to delete such information.
            </p>
          </section>

          <section>
            <h2>9. Changes to This Privacy Policy</h2>
            <p>
              We may update this privacy policy from time to time. We will notify you of any changes by 
              posting the new policy on this page and updating the "Last Updated" date.
            </p>
          </section>

          <section>
            <h2>10. Contact Us</h2>
            <p>
              If you have any questions about this Privacy Policy, please contact us:
            </p>
            <p>
              <strong>Email:</strong> privacy@kalon.com<br />
              <strong>Phone:</strong> +91 1800-123-4567<br />
              <strong>Address:</strong> 123 Fashion Street, Mumbai, Maharashtra 400001, India
            </p>
          </section>
        </div>
      </div>
    </div>
  );
};

export default Privacy;



