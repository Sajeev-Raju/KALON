export const initializeGoogleSignIn = (callback) => {
  if (window.google) {
    window.google.accounts.id.initialize({
      client_id: import.meta.env.VITE_GOOGLE_CLIENT_ID || '', // Will be set in .env
      callback: callback,
    });
  }
};

export const promptGoogleSignIn = () => {
  if (window.google) {
    window.google.accounts.id.prompt();
  }
};

export const renderGoogleButton = (elementId, callback) => {
  if (window.google && document.getElementById(elementId)) {
    window.google.accounts.id.renderButton(
      document.getElementById(elementId),
      {
        theme: 'outline',
        size: 'large',
        text: 'continue_with',
        width: '100%',
      }
    );
  }
};

export const decodeGoogleToken = (credential) => {
  try {
    const base64Url = credential.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    
    return JSON.parse(jsonPayload);
  } catch (error) {
    console.error('Error decoding Google token:', error);
    throw error;
  }
};



