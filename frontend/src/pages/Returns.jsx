import SEO, { BASE_URL } from '../components/common/SEO';
import './Returns.css';

const Returns = () => {
  return (
    <div className="returns-page">
      <SEO
        title="Returns & Refunds"
        description="Learn about KALON's return and refund policy. Easy 30-day returns and hassle-free refunds."
        canonicalUrl={`${BASE_URL}/returns`}
      />
      <div className="container">
        <div className="returns-header">
          <h1>Returns & Refunds</h1>
          <p>We want you to love your purchase. If you don't, we're here to help.</p>
        </div>

        <div className="returns-content">
          <section className="policy-section">
            <h2>Return Policy</h2>
            <div className="policy-card">
              <h3>30-Day Return Window</h3>
              <p>You have 30 days from the date of delivery to return items for a full refund or exchange.</p>
            </div>
            <div className="policy-card">
              <h3>Condition of Items</h3>
              <p>Items must be unused, unwashed, and in their original packaging with tags attached.</p>
            </div>
            <div className="policy-card">
              <h3>What Can Be Returned</h3>
              <ul>
                <li>All clothing items (unless marked as final sale)</li>
                <li>Accessories</li>
                <li>Items with manufacturing defects</li>
              </ul>
            </div>
            <div className="policy-card">
              <h3>What Cannot Be Returned</h3>
              <ul>
                <li>Underwear and intimate apparel (for hygiene reasons)</li>
                <li>Items marked as "Final Sale"</li>
                <li>Items without original tags</li>
                <li>Damaged items due to customer misuse</li>
              </ul>
            </div>
          </section>

          <section className="process-section">
            <h2>How to Return</h2>
            <div className="process-steps">
              <div className="step">
                <div className="step-number">1</div>
                <div className="step-content">
                  <h3>Initiate Return</h3>
                  <p>Log into your account and go to "My Orders". Select the item you want to return and click "Return Item".</p>
                </div>
              </div>
              <div className="step">
                <div className="step-number">2</div>
                <div className="step-content">
                  <h3>Print Return Label</h3>
                  <p>Once approved, you'll receive a return shipping label via email. Print it and attach it to your package.</p>
                </div>
              </div>
              <div className="step">
                <div className="step-number">3</div>
                <div className="step-content">
                  <h3>Pack & Ship</h3>
                  <p>Pack the item securely in its original packaging with all tags attached. Drop it off at any courier service.</p>
                </div>
              </div>
              <div className="step">
                <div className="step-number">4</div>
                <div className="step-content">
                  <h3>Receive Refund</h3>
                  <p>Once we receive and process your return (3-5 business days), we'll issue a refund to your original payment method.</p>
                </div>
              </div>
            </div>
          </section>

          <section className="refund-section">
            <h2>Refund Policy</h2>
            <div className="policy-card">
              <h3>Processing Time</h3>
              <p>Refunds are processed within 3-5 business days after we receive your return. It may take an additional 5-10 business days for the refund to appear in your account, depending on your bank or credit card company.</p>
            </div>
            <div className="policy-card">
              <h3>Refund Method</h3>
              <p>Refunds are issued to the original payment method used for the purchase. If you paid via credit/debit card, the refund will go back to that card. For cash on delivery orders, refunds are processed via bank transfer.</p>
            </div>
            <div className="policy-card">
              <h3>Shipping Costs</h3>
              <p>Original shipping charges are non-refundable unless the item was defective or we made an error with your order.</p>
            </div>
          </section>

          <section className="exchange-section">
            <h2>Exchanges</h2>
            <div className="policy-card">
              <h3>Size/Color Exchanges</h3>
              <p>We offer exchanges for different sizes or colors of the same item, subject to availability. Simply initiate a return and select "Exchange" as your preference. We'll process your exchange once we receive your returned item.</p>
            </div>
          </section>

          <section className="contact-section">
            <h2>Need Help?</h2>
            <p>If you have questions about returns or refunds, please contact our customer service team:</p>
            <div className="contact-info">
              <p><strong>Email:</strong> returns@kalon.com</p>
              <p><strong>Phone:</strong> +91 1800-123-4567</p>
              <p><strong>Hours:</strong> Mon - Sat, 9:00 AM - 6:00 PM IST</p>
            </div>
          </section>
        </div>
      </div>
    </div>
  );
};

export default Returns;



