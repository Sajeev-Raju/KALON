import { useEffect, useRef, useCallback } from 'react';
import { useDispatch } from 'react-redux';
import { loginWithGoogle } from '../../redux/slices/authSlice';
import { decodeGoogleToken } from '../../utils/googleAuth';
import toast from 'react-hot-toast';
import './GoogleSignInButton.css';

const GoogleSignInButton = () => {
  const dispatch = useDispatch();
  const buttonRef = useRef(null);

  const handleGoogleSignIn = useCallback(async (response) => {
    try {
      const userData = decodeGoogleToken(response.credential);
      
      const googleAuthData = {
        idToken: response.credential,
        email: userData.email,
        firstName: userData.given_name || userData.name?.split(' ')[0] || '',
        lastName: userData.family_name || userData.name?.split(' ').slice(1).join(' ') || '',
        picture: userData.picture || '',
      };

      dispatch(loginWithGoogle(googleAuthData));
    } catch (error) {
      toast.error('Failed to process Google sign-in');
      console.error('Google sign-in error:', error);
    }
  }, [dispatch]);

  useEffect(() => {
    const initializeGoogle = () => {
      if (window.google && buttonRef.current) {
        const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID;
        if (!clientId) {
          console.warn('Google Client ID not configured. Please set VITE_GOOGLE_CLIENT_ID in .env file');
          if (buttonRef.current) {
            buttonRef.current.innerHTML = '<p style="text-align: center; color: #999; padding: 12px;">Google Sign-In not configured</p>';
          }
          return;
        }

        window.google.accounts.id.initialize({
          client_id: clientId,
          callback: handleGoogleSignIn,
        });

        window.google.accounts.id.renderButton(buttonRef.current, {
          theme: 'outline',
          size: 'large',
          text: 'continue_with',
          width: '100%',
        });
      }
    };

    // Wait for Google script to load
    if (window.google) {
      initializeGoogle();
    } else {
      const checkGoogle = setInterval(() => {
        if (window.google) {
          initializeGoogle();
          clearInterval(checkGoogle);
        }
      }, 100);

      // Cleanup after 5 seconds if Google doesn't load
      setTimeout(() => clearInterval(checkGoogle), 5000);
    }
  }, [handleGoogleSignIn]);

  return (
    <div className="google-signin-container">
      <div ref={buttonRef} id="google-signin-button"></div>
    </div>
  );
};

export default GoogleSignInButton;
