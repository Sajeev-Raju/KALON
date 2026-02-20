import { createContext, useContext, useState, useEffect } from 'react';
import { siteConfigAPI } from '../services/api';

const SiteConfigContext = createContext({});

export const useSiteConfig = () => useContext(SiteConfigContext);

const injectGoogleAnalytics = (gaId) => {
  if (!gaId || document.querySelector(`script[src*="googletagmanager.com/gtag/js?id=${gaId}"]`)) return;

  const script = document.createElement('script');
  script.async = true;
  script.src = `https://www.googletagmanager.com/gtag/js?id=${gaId}`;
  document.head.appendChild(script);

  const inlineScript = document.createElement('script');
  inlineScript.textContent = `
    window.dataLayer = window.dataLayer || [];
    function gtag(){dataLayer.push(arguments);}
    gtag('js', new Date());
    gtag('config', '${gaId}');
  `;
  document.head.appendChild(inlineScript);
};

export const SiteConfigProvider = ({ children }) => {
  const [config, setConfig] = useState({});
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    const fetchConfig = async () => {
      try {
        const response = await siteConfigAPI.getActive();
        const configs = response.data.data || [];
        const configMap = {};
        configs.forEach((c) => {
          configMap[c.configKey] = c.configValue;
        });
        setConfig(configMap);

        // Inject Google Analytics if configured
        if (configMap.google_analytics_id) {
          injectGoogleAnalytics(configMap.google_analytics_id);
        }
      } catch (error) {
        console.warn('Failed to load site config, using defaults');
      } finally {
        setLoaded(true);
      }
    };
    fetchConfig();
  }, []);

  return (
    <SiteConfigContext.Provider value={{ config, loaded }}>
      {children}
    </SiteConfigContext.Provider>
  );
};

export default SiteConfigContext;
