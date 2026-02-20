# Google OAuth Setup Guide

This guide explains how to set up Google Sign-In for the Kalon e-commerce platform.

## Steps to Enable Google Sign-In

### 1. Create Google OAuth 2.0 Credentials

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable **Google+ API** (or Google Identity Services)
4. Go to **APIs & Services** > **Credentials**
5. Click **Create Credentials** > **OAuth client ID**
6. Choose **Web application** as the application type
7. Add authorized JavaScript origins:
   - `http://localhost:5173` (for Vite dev server)
   - `http://localhost:3000` (if using different port)
   - Your production domain (when deploying)
8. Add authorized redirect URIs (same as above)
9. Copy the **Client ID**

### 2. Configure Frontend

1. Create a `.env` file in the `frontend` directory:
   ```env
   VITE_GOOGLE_CLIENT_ID=your-google-client-id-here
   ```

2. Restart the frontend development server:
   ```bash
   npm run dev
   ```

### 3. Testing

1. Navigate to the registration or login page
2. Click "Continue with Google"
3. Sign in with your Google account
4. The user will be automatically registered/logged in

## Notes

- Email addresses are unique across the platform (both regular and Google OAuth users)
- Users registered with Google OAuth don't need a password
- If a user tries to register with an email that already exists, they'll receive an error message
- Google OAuth users can link their account if they previously registered with email/password



