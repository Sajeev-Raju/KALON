import { useState, useEffect, useCallback } from 'react';
import { useParams, useSearchParams, useLocation } from 'react-router-dom';
import { FiFilter, FiX, FiChevronDown, FiChevronUp } from 'react-icons/fi';
import ProductCard from '../components/common/ProductCard';
import { productAPI, categoryAPI } from '../services/api';
import SEO, { BASE_URL } from '../components/common/SEO';
import './ProductList.css';

const ProductList = () => {
  const { gender, categorySlug } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const location = useLocation();

  const [products, setProducts] = useState([]);
  const [category, setCategory] = useState(null);
  const [loading, setLoading] = useState(true);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [isFilterOpen, setIsFilterOpen] = useState(false);
  const [filterOptions, setFilterOptions] = useState({
    sizes: [],
    colors: [],
    brands: [],
    materials: [],
    categories: [],
    priceRange: { min: 0, max: 10000, avg: 0 },
    onSaleCount: 0,
    inStockCount: 0,
    totalProducts: 0,
  });

  // Expanded filter sections
  const [expandedSections, setExpandedSections] = useState({
    size: true,
    color: true,
    price: true,
    brand: false,
    material: false,
  });

  // Get all filter values from URL params
  const page = parseInt(searchParams.get('page') || '0');
  const sortBy = searchParams.get('sortBy') || 'createdAt';
  const sortDir = searchParams.get('sortDir') || 'desc';
  const searchKeyword = searchParams.get('q') || '';
  const selectedSizes = searchParams.getAll('sizes') || [];
  const selectedColors = searchParams.getAll('colors') || [];
  const selectedBrands = searchParams.getAll('brands') || [];
  const minPrice = searchParams.get('minPrice') || '';
  const maxPrice = searchParams.get('maxPrice') || '';
  const inStock = searchParams.get('inStock') === 'true';
  const onSale = searchParams.get('onSale') === 'true';

  // Fetch filter options on mount (with counts)
  useEffect(() => {
    const fetchFilterOptions = async () => {
      try {
        const response = await productAPI.getFacetedFilterOptions();
        if (response.data.data) {
          setFilterOptions(response.data.data);
        }
      } catch (error) {
        console.error('Error fetching filter options:', error);
        // Fallback to basic filter options if faceted fails
        try {
          const fallbackResponse = await productAPI.getFilterOptions();
          if (fallbackResponse.data.data) {
            const data = fallbackResponse.data.data;
            setFilterOptions({
              sizes: data.sizes?.map(s => ({ value: s, count: null })) || [],
              colors: data.colors?.map(c => ({ value: c, count: null })) || [],
              brands: data.brands?.map(b => ({ value: b, count: null })) || [],
              materials: data.materials?.map(m => ({ value: m, count: null })) || [],
              categories: [],
              priceRange: { min: data.minPrice || 0, max: data.maxPrice || 10000, avg: 0 },
              onSaleCount: 0,
              inStockCount: 0,
              totalProducts: 0,
            });
          }
        } catch (fallbackError) {
          console.error('Error fetching fallback filter options:', fallbackError);
        }
      }
    };
    fetchFilterOptions();
  }, []);

  // Fetch category info if categorySlug exists
  useEffect(() => {
    const fetchCategory = async () => {
      if (categorySlug) {
        try {
          const response = await categoryAPI.getBySlug(categorySlug);
          setCategory(response.data.data);
        } catch (error) {
          console.error('Error fetching category:', error);
        }
      } else if (gender) {
        setCategory({ name: gender.charAt(0).toUpperCase() + gender.slice(1) });
      } else if (searchKeyword) {
        setCategory({ name: `Search: "${searchKeyword}"` });
      } else {
        setCategory(null);
      }
    };
    fetchCategory();
  }, [categorySlug, gender, searchKeyword]);

  // Fetch products with all filters
  const fetchProducts = useCallback(async () => {
    setLoading(true);
    try {
      // Build filter params
      const params = {
        page,
        size: 12,
        sortBy,
        sortDir,
      };

      // Add search keyword
      if (searchKeyword) {
        params.keyword = searchKeyword;
      }

      // Add category or gender filter
      if (categorySlug) {
        params.categorySlug = categorySlug;
      } else if (gender) {
        params.gender = gender.toUpperCase();
      }

      // Add size filters
      if (selectedSizes.length > 0) {
        params.sizes = selectedSizes;
      }

      // Add color filters
      if (selectedColors.length > 0) {
        params.colors = selectedColors;
      }

      // Add brand filters
      if (selectedBrands.length > 0) {
        params.brands = selectedBrands;
      }

      // Add price range
      if (minPrice) {
        params.minPrice = parseFloat(minPrice);
      }
      if (maxPrice) {
        params.maxPrice = parseFloat(maxPrice);
      }

      // Add stock and sale filters
      if (inStock) {
        params.inStock = true;
      }
      if (onSale) {
        params.onSale = true;
      }

      const response = await productAPI.searchAndFilter(params);
      const data = response.data.data;

      setProducts(data.content || []);
      setTotalPages(data.totalPages || 1);
      setTotalElements(data.totalElements || 0);
    } catch (error) {
      console.error('Error fetching products:', error);
      setProducts([]);
    } finally {
      setLoading(false);
    }
  }, [
    page,
    sortBy,
    sortDir,
    searchKeyword,
    categorySlug,
    gender,
    selectedSizes,
    selectedColors,
    selectedBrands,
    minPrice,
    maxPrice,
    inStock,
    onSale,
  ]);

  useEffect(() => {
    fetchProducts();
  }, [fetchProducts]);

  // Helper to update URL params while preserving others
  const updateSearchParams = (updates) => {
    const newParams = new URLSearchParams(searchParams);

    Object.entries(updates).forEach(([key, value]) => {
      if (value === null || value === undefined || value === '' || (Array.isArray(value) && value.length === 0)) {
        newParams.delete(key);
      } else if (Array.isArray(value)) {
        newParams.delete(key);
        value.forEach((v) => newParams.append(key, v));
      } else {
        newParams.set(key, value.toString());
      }
    });

    // Reset to page 0 when filters change (except for page changes)
    if (!updates.hasOwnProperty('page')) {
      newParams.set('page', '0');
    }

    setSearchParams(newParams);
  };

  const handleSortChange = (e) => {
    const [newSortBy, newSortDir] = e.target.value.split('-');
    updateSearchParams({
      sortBy: newSortBy,
      sortDir: newSortDir,
      page: '0',
    });
  };

  const handlePageChange = (newPage) => {
    updateSearchParams({ page: newPage.toString() });
    window.scrollTo(0, 0);
  };

  const handleSizeToggle = (size) => {
    const newSizes = selectedSizes.includes(size)
      ? selectedSizes.filter((s) => s !== size)
      : [...selectedSizes, size];
    updateSearchParams({ sizes: newSizes });
  };

  const handleColorToggle = (color) => {
    const newColors = selectedColors.includes(color)
      ? selectedColors.filter((c) => c !== color)
      : [...selectedColors, color];
    updateSearchParams({ colors: newColors });
  };

  const handleBrandToggle = (brand) => {
    const newBrands = selectedBrands.includes(brand)
      ? selectedBrands.filter((b) => b !== brand)
      : [...selectedBrands, brand];
    updateSearchParams({ brands: newBrands });
  };

  const handlePriceChange = (type, value) => {
    if (type === 'min') {
      updateSearchParams({ minPrice: value || null });
    } else {
      updateSearchParams({ maxPrice: value || null });
    }
  };

  const handleInStockToggle = () => {
    updateSearchParams({ inStock: !inStock ? 'true' : null });
  };

  const handleOnSaleToggle = () => {
    updateSearchParams({ onSale: !onSale ? 'true' : null });
  };

  const clearAllFilters = () => {
    const newParams = new URLSearchParams();
    // Keep search keyword if present
    if (searchKeyword) {
      newParams.set('q', searchKeyword);
    }
    setSearchParams(newParams);
  };

  const toggleSection = (section) => {
    setExpandedSections((prev) => ({
      ...prev,
      [section]: !prev[section],
    }));
  };

  const hasActiveFilters =
    selectedSizes.length > 0 ||
    selectedColors.length > 0 ||
    selectedBrands.length > 0 ||
    minPrice ||
    maxPrice ||
    inStock ||
    onSale;

  // Get page title
  const getPageTitle = () => {
    if (searchKeyword) {
      return `Search Results for "${searchKeyword}"`;
    }
    return category?.name || 'All Products';
  };

  // Color code mapping for display
  const getColorCode = (colorName) => {
    const colorMap = {
      Black: '#000000',
      White: '#FFFFFF',
      Navy: '#000080',
      Grey: '#808080',
      Red: '#FF0000',
      Blue: '#0000FF',
      Green: '#008000',
      Maroon: '#800000',
      Brown: '#8B4513',
      Beige: '#F5F5DC',
      Pink: '#FFC0CB',
      Orange: '#FFA500',
      Yellow: '#FFFF00',
      Purple: '#800080',
    };
    return colorMap[colorName] || '#CCCCCC';
  };

  // SEO data
  const getSeoTitle = () => {
    if (searchKeyword) return `Search: ${searchKeyword}`;
    if (category?.metaTitle) return category.metaTitle;
    if (category?.name) return category.name;
    const path = location.pathname;
    if (path === '/new-arrivals') return 'New Arrivals';
    if (path === '/best-sellers') return 'Best Sellers';
    if (path === '/featured') return 'Featured Collection';
    return 'All Products';
  };

  const getSeoDescription = () => {
    if (category?.metaDescription) return category.metaDescription;
    if (category?.description) return category.description;
    return `Shop ${getSeoTitle()} at KALON. Discover the latest fashion trends with free shipping on orders above ₹999.`;
  };

  const getCanonicalUrl = () => {
    const base = `${BASE_URL}${location.pathname}`;
    return page > 0 ? `${base}?page=${page}` : base;
  };

  const breadcrumbSchema = category?.slug ? {
    '@context': 'https://schema.org',
    '@type': 'BreadcrumbList',
    itemListElement: [
      {
        '@type': 'ListItem',
        position: 1,
        name: 'Home',
        item: BASE_URL,
      },
      {
        '@type': 'ListItem',
        position: 2,
        name: category.name,
        item: `${BASE_URL}/category/${category.slug}`,
      },
    ],
  } : null;

  return (
    <div className="product-list-page">
      <SEO
        title={getSeoTitle()}
        description={getSeoDescription()}
        keywords={category?.metaKeywords}
        canonicalUrl={getCanonicalUrl()}
        ogImage={category?.ogImage}
        ogType="website"
        jsonLd={breadcrumbSchema}
      />
      {/* Page Header */}
      <div className="page-header">
        <div className="container">
          <h1 className="page-title">{getPageTitle()}</h1>
          <p className="page-description">
            {category?.description || `${totalElements} products found`}
          </p>
        </div>
      </div>

      <div className="container">
        <div className="product-list-content">
          {/* Sidebar Filters */}
          <aside className={`filters-sidebar ${isFilterOpen ? 'open' : ''}`}>
            <div className="filters-header">
              <h3>Filters</h3>
              <button
                className="close-filters"
                onClick={() => setIsFilterOpen(false)}
              >
                <FiX />
              </button>
            </div>

            {hasActiveFilters && (
              <button className="clear-filters-btn" onClick={clearAllFilters}>
                Clear All Filters
              </button>
            )}

            {/* In Stock Filter */}
            <div className="filter-group">
              <label className="checkbox-option">
                <input
                  type="checkbox"
                  checked={inStock}
                  onChange={handleInStockToggle}
                />
                <span>In Stock Only</span>
                {filterOptions.inStockCount > 0 && (
                  <span className="filter-count-badge">({filterOptions.inStockCount})</span>
                )}
              </label>
              <label className="checkbox-option">
                <input
                  type="checkbox"
                  checked={onSale}
                  onChange={handleOnSaleToggle}
                />
                <span>On Sale</span>
                {filterOptions.onSaleCount > 0 && (
                  <span className="filter-count-badge">({filterOptions.onSaleCount})</span>
                )}
              </label>
            </div>

            {/* Size Filter */}
            <div className="filter-group">
              <h4
                className="filter-title"
                onClick={() => toggleSection('size')}
              >
                Size{' '}
                {expandedSections.size ? <FiChevronUp /> : <FiChevronDown />}
              </h4>
              {expandedSections.size && (
                <div className="filter-options sizes">
                  {filterOptions.sizes.map((sizeItem) => {
                    const sizeValue = sizeItem.value || sizeItem;
                    const sizeCount = sizeItem.count;
                    return (
                      <label
                        key={sizeValue}
                        className={`size-option ${
                          selectedSizes.includes(sizeValue) ? 'selected' : ''
                        }`}
                      >
                        <input
                          type="checkbox"
                          checked={selectedSizes.includes(sizeValue)}
                          onChange={() => handleSizeToggle(sizeValue)}
                        />
                        <span>{sizeValue}</span>
                        {sizeCount !== null && sizeCount !== undefined && (
                          <span className="filter-count-badge">({sizeCount})</span>
                        )}
                      </label>
                    );
                  })}
                </div>
              )}
            </div>

            {/* Color Filter */}
            <div className="filter-group">
              <h4
                className="filter-title"
                onClick={() => toggleSection('color')}
              >
                Color{' '}
                {expandedSections.color ? <FiChevronUp /> : <FiChevronDown />}
              </h4>
              {expandedSections.color && (
                <div className="filter-options colors">
                  {filterOptions.colors.map((colorItem) => {
                    const colorValue = colorItem.value || colorItem;
                    const colorCount = colorItem.count;
                    return (
                      <label
                        key={colorValue}
                        className={`color-option ${
                          selectedColors.includes(colorValue) ? 'selected' : ''
                        }`}
                      >
                        <input
                          type="checkbox"
                          checked={selectedColors.includes(colorValue)}
                          onChange={() => handleColorToggle(colorValue)}
                        />
                        <span
                          className="color-swatch"
                          style={{ backgroundColor: getColorCode(colorValue) }}
                        />
                        <span>{colorValue}</span>
                        {colorCount !== null && colorCount !== undefined && (
                          <span className="filter-count-badge">({colorCount})</span>
                        )}
                      </label>
                    );
                  })}
                </div>
              )}
            </div>

            {/* Price Filter */}
            <div className="filter-group">
              <h4
                className="filter-title"
                onClick={() => toggleSection('price')}
              >
                Price{' '}
                {expandedSections.price ? <FiChevronUp /> : <FiChevronDown />}
              </h4>
              {expandedSections.price && (
                <div className="filter-options price-range">
                  <div className="price-inputs">
                    <input
                      type="number"
                      placeholder="Min"
                      value={minPrice}
                      onChange={(e) => handlePriceChange('min', e.target.value)}
                      min="0"
                    />
                    <span>to</span>
                    <input
                      type="number"
                      placeholder="Max"
                      value={maxPrice}
                      onChange={(e) => handlePriceChange('max', e.target.value)}
                      min="0"
                    />
                  </div>
                  <div className="price-presets">
                    <button
                      className={
                        minPrice === '' && maxPrice === '500' ? 'active' : ''
                      }
                      onClick={() => {
                        updateSearchParams({ minPrice: null, maxPrice: '500' });
                      }}
                    >
                      Under ₹500
                    </button>
                    <button
                      className={
                        minPrice === '500' && maxPrice === '1000' ? 'active' : ''
                      }
                      onClick={() => {
                        updateSearchParams({ minPrice: '500', maxPrice: '1000' });
                      }}
                    >
                      ₹500 - ₹1000
                    </button>
                    <button
                      className={
                        minPrice === '1000' && maxPrice === '2000' ? 'active' : ''
                      }
                      onClick={() => {
                        updateSearchParams({ minPrice: '1000', maxPrice: '2000' });
                      }}
                    >
                      ₹1000 - ₹2000
                    </button>
                    <button
                      className={minPrice === '2000' && maxPrice === '' ? 'active' : ''}
                      onClick={() => {
                        updateSearchParams({ minPrice: '2000', maxPrice: null });
                      }}
                    >
                      Above ₹2000
                    </button>
                  </div>
                </div>
              )}
            </div>

            {/* Brand Filter */}
            {filterOptions.brands.length > 0 && (
              <div className="filter-group">
                <h4
                  className="filter-title"
                  onClick={() => toggleSection('brand')}
                >
                  Brand{' '}
                  {expandedSections.brand ? <FiChevronUp /> : <FiChevronDown />}
                </h4>
                {expandedSections.brand && (
                  <div className="filter-options">
                    {filterOptions.brands.map((brandItem) => {
                      const brandValue = brandItem.value || brandItem;
                      const brandCount = brandItem.count;
                      return (
                        <label key={brandValue} className="checkbox-option">
                          <input
                            type="checkbox"
                            checked={selectedBrands.includes(brandValue)}
                            onChange={() => handleBrandToggle(brandValue)}
                          />
                          <span>{brandValue}</span>
                          {brandCount !== null && brandCount !== undefined && (
                            <span className="filter-count-badge">({brandCount})</span>
                          )}
                        </label>
                      );
                    })}
                  </div>
                )}
              </div>
            )}
          </aside>

          {/* Products Grid */}
          <div className="products-container">
            {/* Toolbar */}
            <div className="products-toolbar">
              <button
                className="filter-toggle"
                onClick={() => setIsFilterOpen(true)}
              >
                <FiFilter /> Filters
                {hasActiveFilters && (
                  <span className="filter-count">
                    {selectedSizes.length +
                      selectedColors.length +
                      selectedBrands.length +
                      (minPrice || maxPrice ? 1 : 0) +
                      (inStock ? 1 : 0) +
                      (onSale ? 1 : 0)}
                  </span>
                )}
              </button>

              <div className="products-count">{totalElements} Products</div>

              <div className="sort-select">
                <select
                  value={`${sortBy}-${sortDir}`}
                  onChange={handleSortChange}
                >
                  <option value="createdAt-desc">Newest First</option>
                  <option value="price-asc">Price: Low to High</option>
                  <option value="price-desc">Price: High to Low</option>
                  <option value="name-asc">Name: A to Z</option>
                  <option value="averageRating-desc">Top Rated</option>
                </select>
              </div>
            </div>

            {/* Active Filters Tags */}
            {hasActiveFilters && (
              <div className="active-filters">
                {selectedSizes.map((size) => (
                  <span key={size} className="filter-tag">
                    Size: {size}
                    <button onClick={() => handleSizeToggle(size)}>
                      <FiX />
                    </button>
                  </span>
                ))}
                {selectedColors.map((color) => (
                  <span key={color} className="filter-tag">
                    Color: {color}
                    <button onClick={() => handleColorToggle(color)}>
                      <FiX />
                    </button>
                  </span>
                ))}
                {selectedBrands.map((brand) => (
                  <span key={brand} className="filter-tag">
                    Brand: {brand}
                    <button onClick={() => handleBrandToggle(brand)}>
                      <FiX />
                    </button>
                  </span>
                ))}
                {(minPrice || maxPrice) && (
                  <span className="filter-tag">
                    Price: ₹{minPrice || '0'} - ₹{maxPrice || '∞'}
                    <button
                      onClick={() =>
                        updateSearchParams({ minPrice: null, maxPrice: null })
                      }
                    >
                      <FiX />
                    </button>
                  </span>
                )}
                {inStock && (
                  <span className="filter-tag">
                    In Stock
                    <button onClick={handleInStockToggle}>
                      <FiX />
                    </button>
                  </span>
                )}
                {onSale && (
                  <span className="filter-tag">
                    On Sale
                    <button onClick={handleOnSaleToggle}>
                      <FiX />
                    </button>
                  </span>
                )}
              </div>
            )}

            {/* Products Grid */}
            {loading ? (
              <div className="products-grid loading">
                {[...Array(8)].map((_, index) => (
                  <div key={index} className="product-skeleton">
                    <div className="skeleton-image" />
                    <div className="skeleton-text" />
                    <div className="skeleton-text short" />
                  </div>
                ))}
              </div>
            ) : products.length > 0 ? (
              <>
                <div className="products-grid">
                  {products.map((product) => (
                    <ProductCard key={product.id} product={product} />
                  ))}
                </div>

                {/* Pagination */}
                {totalPages > 1 && (
                  <div className="pagination">
                    <button
                      disabled={page === 0}
                      onClick={() => handlePageChange(page - 1)}
                    >
                      Previous
                    </button>
                    {[...Array(Math.min(totalPages, 10))].map((_, index) => {
                      // Show pages around current page
                      let pageNum = index;
                      if (totalPages > 10) {
                        if (page < 5) {
                          pageNum = index;
                        } else if (page > totalPages - 6) {
                          pageNum = totalPages - 10 + index;
                        } else {
                          pageNum = page - 5 + index;
                        }
                      }
                      return (
                        <button
                          key={pageNum}
                          className={page === pageNum ? 'active' : ''}
                          onClick={() => handlePageChange(pageNum)}
                        >
                          {pageNum + 1}
                        </button>
                      );
                    })}
                    <button
                      disabled={page === totalPages - 1}
                      onClick={() => handlePageChange(page + 1)}
                    >
                      Next
                    </button>
                  </div>
                )}
              </>
            ) : (
              <div className="no-products">
                <h3>No products found</h3>
                <p>Try adjusting your filters or browse other categories</p>
                {hasActiveFilters && (
                  <button className="clear-filters-btn" onClick={clearAllFilters}>
                    Clear All Filters
                  </button>
                )}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Mobile Filter Overlay */}
      {isFilterOpen && (
        <div
          className="filter-overlay"
          onClick={() => setIsFilterOpen(false)}
        />
      )}
    </div>
  );
};

export default ProductList;
