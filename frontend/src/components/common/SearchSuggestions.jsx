import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { FiSearch, FiTag, FiFolder } from 'react-icons/fi';
import { productAPI } from '../../services/api';
import './SearchSuggestions.css';

const SearchSuggestions = ({ query, onSelect, isVisible, onClose }) => {
  const [suggestions, setSuggestions] = useState({
    products: [],
    brands: [],
    categories: [],
  });
  const [loading, setLoading] = useState(false);
  const [selectedIndex, setSelectedIndex] = useState(-1);
  const containerRef = useRef(null);

  useEffect(() => {
    const fetchSuggestions = async () => {
      if (!query || query.trim().length < 2) {
        setSuggestions({ products: [], brands: [], categories: [] });
        return;
      }

      setLoading(true);
      try {
        const response = await productAPI.getSuggestions(query);
        if (response.data.data) {
          setSuggestions(response.data.data);
        }
      } catch (error) {
        console.error('Error fetching suggestions:', error);
      } finally {
        setLoading(false);
      }
    };

    const debounceTimer = setTimeout(fetchSuggestions, 300);
    return () => clearTimeout(debounceTimer);
  }, [query]);

  useEffect(() => {
    setSelectedIndex(-1);
  }, [query]);

  const navigate = useNavigate();

  const handleSelect = (type, value) => {
    if (type === 'product' || type === 'brand') {
      navigate(`/search?q=${encodeURIComponent(value)}`);
    } else if (type === 'category') {
      navigate(`/search?q=${encodeURIComponent(value)}`);
    }
    onSelect(value);
    onClose();
  };

  const allSuggestions = [
    ...suggestions.products.map((p) => ({ type: 'product', value: p })),
    ...suggestions.brands.map((b) => ({ type: 'brand', value: b })),
    ...suggestions.categories.map((c) => ({ type: 'category', value: c })),
  ];

  const handleKeyDown = (e) => {
    if (!isVisible || allSuggestions.length === 0) return;

    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setSelectedIndex((prev) =>
        prev < allSuggestions.length - 1 ? prev + 1 : 0
      );
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setSelectedIndex((prev) =>
        prev > 0 ? prev - 1 : allSuggestions.length - 1
      );
    } else if (e.key === 'Enter' && selectedIndex >= 0) {
      e.preventDefault();
      const selected = allSuggestions[selectedIndex];
      handleSelect(selected.type, selected.value);
    } else if (e.key === 'Escape') {
      onClose();
    }
  };

  useEffect(() => {
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isVisible, allSuggestions, selectedIndex]);

  if (!isVisible || !query || query.trim().length < 2) {
    return null;
  }

  const hasSuggestions =
    suggestions.products.length > 0 ||
    suggestions.brands.length > 0 ||
    suggestions.categories.length > 0;

  return (
    <div className="search-suggestions" ref={containerRef}>
      {loading ? (
        <div className="suggestions-loading">
          <span className="loading-spinner" />
          Searching...
        </div>
      ) : hasSuggestions ? (
        <>
          {suggestions.products.length > 0 && (
            <div className="suggestion-group">
              <div className="suggestion-group-title">
                <FiSearch /> Products
              </div>
              {suggestions.products.map((product, index) => (
                <div
                  key={`product-${index}`}
                  className={`suggestion-item ${
                    selectedIndex === index ? 'selected' : ''
                  }`}
                  onClick={() => handleSelect('product', product)}
                >
                  <FiSearch className="suggestion-icon" />
                  <span
                    dangerouslySetInnerHTML={{
                      __html: highlightMatch(product, query),
                    }}
                  />
                </div>
              ))}
            </div>
          )}

          {suggestions.brands.length > 0 && (
            <div className="suggestion-group">
              <div className="suggestion-group-title">
                <FiTag /> Brands
              </div>
              {suggestions.brands.map((brand, index) => (
                <div
                  key={`brand-${index}`}
                  className={`suggestion-item ${
                    selectedIndex === suggestions.products.length + index
                      ? 'selected'
                      : ''
                  }`}
                  onClick={() => handleSelect('brand', brand)}
                >
                  <FiTag className="suggestion-icon" />
                  <span
                    dangerouslySetInnerHTML={{
                      __html: highlightMatch(brand, query),
                    }}
                  />
                </div>
              ))}
            </div>
          )}

          {suggestions.categories.length > 0 && (
            <div className="suggestion-group">
              <div className="suggestion-group-title">
                <FiFolder /> Categories
              </div>
              {suggestions.categories.map((category, index) => (
                <div
                  key={`category-${index}`}
                  className={`suggestion-item ${
                    selectedIndex ===
                    suggestions.products.length + suggestions.brands.length + index
                      ? 'selected'
                      : ''
                  }`}
                  onClick={() => handleSelect('category', category)}
                >
                  <FiFolder className="suggestion-icon" />
                  <span
                    dangerouslySetInnerHTML={{
                      __html: highlightMatch(category, query),
                    }}
                  />
                </div>
              ))}
            </div>
          )}
        </>
      ) : (
        <div className="no-suggestions">
          No suggestions found for "{query}"
        </div>
      )}
    </div>
  );
};

// Helper function to highlight matching text
const highlightMatch = (text, query) => {
  if (!query) return text;
  const regex = new RegExp(`(${escapeRegExp(query)})`, 'gi');
  return text.replace(regex, '<strong>$1</strong>');
};

const escapeRegExp = (string) => {
  return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
};

export default SearchSuggestions;
