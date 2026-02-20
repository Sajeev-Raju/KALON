import { useState } from 'react';
import { FiChevronDown, FiChevronUp } from 'react-icons/fi';
import SEO, { BASE_URL } from '../components/common/SEO';
import './FAQ.css';

const FAQ = () => {
  const [openIndex, setOpenIndex] = useState(null);

  const faqs = [
    {
      category: 'Orders & Shipping',
      questions: [
        {
          q: 'How long does shipping take?',
          a: 'Standard shipping takes 5-7 business days. Express shipping (2-3 business days) is available at checkout for an additional fee. International shipping may take 10-15 business days.',
        },
        {
          q: 'What are the shipping charges?',
          a: 'We offer FREE shipping on orders above ₹999. For orders below ₹999, shipping charges are ₹99. Express shipping charges vary based on your location.',
        },
        {
          q: 'Can I track my order?',
          a: 'Yes! Once your order ships, you\'ll receive a tracking number via email and SMS. You can track your order status in "My Account" or on our Track Order page.',
        },
        {
          q: 'Do you ship internationally?',
          a: 'Currently, we ship within India only. We\'re working on expanding to international markets soon.',
        },
      ],
    },
    {
      category: 'Returns & Refunds',
      questions: [
        {
          q: 'What is your return policy?',
          a: 'You can return items within 30 days of delivery. Items must be unused, unwashed, and in original packaging with tags attached. Please check our Returns & Refunds page for detailed information.',
        },
        {
          q: 'How do I return an item?',
          a: 'Log into your account, go to "My Orders", select the item you want to return, and follow the return process. You\'ll receive a return label via email.',
        },
        {
          q: 'When will I get my refund?',
          a: 'Refunds are processed within 3-5 business days after we receive your return. It may take an additional 5-10 business days to appear in your account.',
        },
      ],
    },
    {
      category: 'Products',
      questions: [
        {
          q: 'What is your size guide?',
          a: 'Each product page includes a detailed size guide. You can also find our general size guide in the product description section. We recommend measuring yourself and comparing with our size chart for the best fit.',
        },
        {
          q: 'Are your products authentic?',
          a: 'Yes, all our products are 100% authentic and sourced directly from authorized suppliers. We guarantee the authenticity of every item sold on our platform.',
        },
        {
          q: 'Do you offer plus sizes?',
          a: 'Yes, we offer extended sizes for select styles. Check the product page for available sizes. You can also filter products by size in our catalog.',
        },
        {
          q: 'How do I care for my clothing?',
          a: 'Care instructions are provided on each product page and on the care label attached to the garment. Generally, we recommend following the washing instructions on the label to maintain the quality of your items.',
        },
      ],
    },
    {
      category: 'Payment',
      questions: [
        {
          q: 'What payment methods do you accept?',
          a: 'We accept all major credit/debit cards, UPI, net banking, and cash on delivery (COD). All payment information is encrypted and secure.',
        },
        {
          q: 'Is my payment information secure?',
          a: 'Yes, we use industry-standard SSL encryption to protect your payment information. We never store your full card details on our servers.',
        },
        {
          q: 'Do you offer installment options?',
          a: 'Yes, we offer EMI options for orders above ₹3000. You can select the EMI option at checkout and choose from available tenure options.',
        },
      ],
    },
    {
      category: 'Account & Orders',
      questions: [
        {
          q: 'How do I create an account?',
          a: 'Click on "Sign Up" in the header, fill in your details, and verify your email. You can also create an account during checkout.',
        },
        {
          q: 'I forgot my password. How do I reset it?',
          a: 'Click on "Forgot Password" on the login page, enter your email address, and follow the instructions sent to your email to reset your password.',
        },
        {
          q: 'Can I change my order after placing it?',
          a: 'You can cancel or modify your order within 2 hours of placing it. After that, the order will be processed and cannot be changed. Contact our customer service for assistance.',
        },
        {
          q: 'How do I view my order history?',
          a: 'Log into your account and click on "My Orders" to view all your past and current orders with their status and tracking information.',
        },
      ],
    },
  ];

  const toggleQuestion = (index) => {
    setOpenIndex(openIndex === index ? null : index);
  };

  return (
    <div className="faq-page">
      <SEO
        title="FAQs"
        description="Find answers to common questions about shopping with KALON - shipping, returns, payments, and more."
        canonicalUrl={`${BASE_URL}/faq`}
      />
      <div className="container">
        <div className="faq-header">
          <h1>Frequently Asked Questions</h1>
          <p>Find answers to common questions about shopping with KALON</p>
        </div>

        <div className="faq-content">
          {faqs.map((category, catIndex) => (
            <div key={catIndex} className="faq-category">
              <h2>{category.category}</h2>
              <div className="faq-list">
                {category.questions.map((faq, index) => {
                  const globalIndex = faqs
                    .slice(0, catIndex)
                    .reduce((sum, cat) => sum + cat.questions.length, 0) + index;
                  const isOpen = openIndex === globalIndex;

                  return (
                    <div key={index} className={`faq-item ${isOpen ? 'open' : ''}`}>
                      <button
                        className="faq-question"
                        onClick={() => toggleQuestion(globalIndex)}
                      >
                        <span>{faq.q}</span>
                        {isOpen ? <FiChevronUp /> : <FiChevronDown />}
                      </button>
                      {isOpen && (
                        <div className="faq-answer">
                          <p>{faq.a}</p>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          ))}
        </div>

        <div className="faq-contact">
          <h3>Still have questions?</h3>
          <p>Can't find the answer you're looking for? Please get in touch with our friendly team.</p>
          <a href="/contact" className="btn btn-primary">Contact Us</a>
        </div>
      </div>
    </div>
  );
};

export default FAQ;



