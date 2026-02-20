import { useEffect } from 'react';
import { useSiteConfig } from '../../context/SiteConfigContext';

const BASE_URL = import.meta.env.VITE_BASE_URL || 'http://localhost:5173';

/**
 * Ensures a URL is absolute. Relative paths get BASE_URL prepended.
 */
const toAbsoluteUrl = (url) => {
  if (!url) return '';
  if (url.startsWith('http://') || url.startsWith('https://')) return url;
  return `${BASE_URL}${url.startsWith('/') ? '' : '/'}${url}`;
};

const SEO = ({
  title,
  description,
  keywords,
  canonicalUrl,
  ogTitle,
  ogDescription,
  ogImage,
  ogType = 'website',
  ogUrl,
  jsonLd,
}) => {
  const { config } = useSiteConfig();

  // Derive defaults from SiteConfig, falling back to static values
  const siteName = config.site_meta_title || 'KALON';
  const defaultDescription = config.site_meta_description
    || 'Discover the latest fashion trends at KALON. Shop men\'s and women\'s clothing, sneakers, and accessories.';
  const defaultOgImage = config.default_og_image || '';

  useEffect(() => {
    const fullTitle = title ? `${title} | ${siteName}` : siteName;
    const metaDescription = description || defaultDescription;
    const metaOgTitle = ogTitle || title || siteName;
    const metaOgDescription = ogDescription || metaDescription;
    const metaOgImage = toAbsoluteUrl(ogImage || defaultOgImage);
    const metaOgUrl = ogUrl || canonicalUrl || window.location.href;

    // Title
    document.title = fullTitle;

    // Helper to set or create a meta tag
    const setMeta = (attr, attrValue, content) => {
      if (!content) return;
      let el = document.querySelector(`meta[${attr}="${attrValue}"]`);
      if (!el) {
        el = document.createElement('meta');
        el.setAttribute(attr, attrValue);
        document.head.appendChild(el);
      }
      el.setAttribute('content', content);
    };

    // Standard meta tags
    setMeta('name', 'description', metaDescription);
    if (keywords) {
      setMeta('name', 'keywords', keywords);
    }

    // Canonical URL
    let canonical = document.querySelector('link[rel="canonical"]');
    if (canonicalUrl) {
      if (!canonical) {
        canonical = document.createElement('link');
        canonical.setAttribute('rel', 'canonical');
        document.head.appendChild(canonical);
      }
      canonical.setAttribute('href', canonicalUrl);
    } else if (canonical) {
      canonical.remove();
    }

    // Open Graph tags
    setMeta('property', 'og:title', metaOgTitle);
    setMeta('property', 'og:description', metaOgDescription);
    setMeta('property', 'og:type', ogType);
    setMeta('property', 'og:url', metaOgUrl);
    setMeta('property', 'og:site_name', siteName);
    if (metaOgImage) {
      setMeta('property', 'og:image', metaOgImage);
    }

    // Twitter Card tags
    setMeta('name', 'twitter:card', 'summary_large_image');
    setMeta('name', 'twitter:title', metaOgTitle);
    setMeta('name', 'twitter:description', metaOgDescription);
    if (metaOgImage) {
      setMeta('name', 'twitter:image', metaOgImage);
    }

    // JSON-LD structured data
    document.querySelectorAll('script[data-seo-jsonld]').forEach(el => el.remove());

    if (jsonLd) {
      const schemas = Array.isArray(jsonLd) ? jsonLd : [jsonLd];
      schemas.forEach((schema, index) => {
        const script = document.createElement('script');
        script.type = 'application/ld+json';
        script.setAttribute('data-seo-jsonld', `seo-${index}`);
        script.textContent = JSON.stringify(schema);
        document.head.appendChild(script);
      });
    }

    // Cleanup JSON-LD on unmount
    return () => {
      document.querySelectorAll('script[data-seo-jsonld]').forEach(el => el.remove());
    };
  }, [title, description, keywords, canonicalUrl, ogTitle, ogDescription, ogImage, ogType, ogUrl, jsonLd, config]);

  return null;
};

export { BASE_URL, toAbsoluteUrl };
export default SEO;
