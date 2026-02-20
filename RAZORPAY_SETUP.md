# Razorpay Payment Integration Setup Guide

This guide explains how to set up Razorpay payment integration for the Kalon e-commerce platform.

## Backend Setup

### 1. Get Razorpay API Keys

1. Sign up/Login to [Razorpay Dashboard](https://dashboard.razorpay.com/)
2. Go to **Settings** > **API Keys**
3. Generate **Key ID** and **Key Secret**
4. Copy both keys

### 2. Configure Backend

1. Open `backend/src/main/resources/application.properties`
2. Update the Razorpay configuration:
   ```properties
   razorpay.key.id=your_razorpay_key_id
   razorpay.key.secret=your_razorpay_key_secret
   razorpay.currency=INR
   ```
3. Replace `your_razorpay_key_id` and `your_razorpay_key_secret` with your actual keys

### 3. Backend Endpoints

- `POST /api/orders/razorpay/create` - Creates a Razorpay order
- `POST /api/orders/razorpay/verify` - Verifies payment and completes the order

## Frontend Setup

The frontend uses Razorpay Checkout.js script which is loaded dynamically. No additional configuration needed.

## Payment Flow

1. User proceeds to checkout
2. User selects delivery address
3. Frontend calls `/api/orders/razorpay/create` to create a Razorpay order
4. Razorpay checkout popup opens
5. User completes payment
6. Frontend calls `/api/orders/razorpay/verify` with payment details
7. Backend verifies payment signature and completes the order
8. User is redirected to order confirmation page

## Testing

### Test Mode

Razorpay provides test credentials for testing:
- Test Key ID and Secret are available in Razorpay Dashboard > Settings > API Keys
- Use test cards: https://razorpay.com/docs/payments/test-payments/

### Production Mode

1. Activate your Razorpay account
2. Get production API keys
3. Update `application.properties` with production keys
4. Ensure your domain is whitelisted in Razorpay Dashboard

## Security Notes

- Never expose your Razorpay Secret Key in frontend code
- Always verify payment signature on the backend
- Use HTTPS in production
- Store keys securely (consider using environment variables or secrets management)



