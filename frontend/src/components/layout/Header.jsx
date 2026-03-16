import { useState, useEffect, useRef } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useSelector, useDispatch } from 'react-redux';
import { FiSearch, FiUser, FiHeart, FiShoppingBag, FiMenu, FiX } from 'react-icons/fi';
import { logoutAsync } from '../../redux/slices/authSlice';
import SearchSuggestions from '../common/SearchSuggestions';
import logo from '../../assets/images/logo.png';
import './Header.css';

const Header = () => {
  const [isScrolled, setIsScrolled] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [isSearchOpen, setIsSearchOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const searchContainerRef = useRef(null);

  const { isAuthenticated, user } = useSelector((state) => state.auth);
  const { itemCount } = useSelector((state) => state.cart);
  const { items: wishlistItems } = useSelector((state) => state.wishlist);
  const { itemCount: guestCartCount } = useSelector((state) => state.guestCart);
  const { items: guestWishlistItems } = useSelector((state) => state.guestWishlist);

  const activeCartCount = isAuthenticated ? itemCount : guestCartCount;
  const activeWishlistCount = isAuthenticated ? wishlistItems.length : guestWishlistItems.length;

  const dispatch = useDispatch();
  const navigate = useNavigate();

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 50);
    };

    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  // Close suggestions when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (searchContainerRef.current && !searchContainerRef.current.contains(event.target)) {
        setShowSuggestions(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSearch = (e) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      navigate(`/search?q=${encodeURIComponent(searchQuery)}`);
      setSearchQuery('');
      setIsSearchOpen(false);
      setShowSuggestions(false);
    }
  };

  const handleSearchInputChange = (e) => {
    setSearchQuery(e.target.value);
    setShowSuggestions(e.target.value.length >= 2);
  };

  const handleSuggestionSelect = (value) => {
    setSearchQuery(value);
    setShowSuggestions(false);
  };

  const handleLogout = () => {
    dispatch(logoutAsync());
    setIsUserMenuOpen(false);
    navigate('/');
  };

  return (
    <header className={`header ${isScrolled ? 'scrolled' : ''}`}>
      <div className="header-container">
        {/* Mobile Menu Toggle */}
        <button
          className="mobile-menu-toggle"
          onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
          aria-label={isMobileMenuOpen ? 'Close menu' : 'Open menu'}
          aria-expanded={isMobileMenuOpen}
        >
          {isMobileMenuOpen ? <FiX /> : <FiMenu />}
        </button>

        {/* Logo */}
        <Link to="/" className="logo">
          <img src={logo} alt="Kalon" />
        </Link>

        {/* Desktop Navigation */}
        <nav className={`main-nav ${isMobileMenuOpen ? 'open' : ''}`} role="navigation">
          <ul className="nav-list">
            <li className="nav-item">
              <Link to="/men" className="nav-link" onClick={() => setIsMobileMenuOpen(false)}>
                MEN
              </Link>
              <div className="mega-menu">
                <div className="mega-menu-content">
                  <div className="mega-menu-column">
                    <h4>Topwear</h4>
                    <ul>
                      <li><Link to="/men/t-shirts">T-Shirts</Link></li>
                      <li><Link to="/men/shirts">Shirts</Link></li>
                      <li><Link to="/men/hoodies">Hoodies & Sweatshirts</Link></li>
                      <li><Link to="/men/jackets">Jackets</Link></li>
                    </ul>
                  </div>
                  <div className="mega-menu-column">
                    <h4>Bottomwear</h4>
                    <ul>
                      <li><Link to="/men/joggers">Joggers</Link></li>
                      <li><Link to="/men/jeans">Jeans</Link></li>
                      <li><Link to="/men/shorts">Shorts</Link></li>
                      <li><Link to="/men/pants">Pants</Link></li>
                    </ul>
                  </div>
                </div>
              </div>
            </li>
            <li className="nav-item">
              <Link to="/women" className="nav-link" onClick={() => setIsMobileMenuOpen(false)}>
                WOMEN
              </Link>
              <div className="mega-menu">
                <div className="mega-menu-content">
                  <div className="mega-menu-column">
                    <h4>Topwear</h4>
                    <ul>
                      <li><Link to="/women/t-shirts">T-Shirts</Link></li>
                      <li><Link to="/women/shirts">Shirts</Link></li>
                      <li><Link to="/women/hoodies">Hoodies & Sweatshirts</Link></li>
                      <li><Link to="/women/dresses">Dresses</Link></li>
                    </ul>
                  </div>
                  <div className="mega-menu-column">
                    <h4>Bottomwear</h4>
                    <ul>
                      <li><Link to="/women/joggers">Joggers</Link></li>
                      <li><Link to="/women/jeans">Jeans</Link></li>
                      <li><Link to="/women/shorts">Shorts</Link></li>
                      <li><Link to="/women/skirts">Skirts</Link></li>
                    </ul>
                  </div>
                </div>
              </div>
            </li>
            <li className="nav-item">
              <Link to="/sneakers" className="nav-link" onClick={() => setIsMobileMenuOpen(false)}>
                SNEAKERS
              </Link>
            </li>
          </ul>
        </nav>

        {/* Header Actions */}
        <div className="header-actions">
          {/* Search */}
          <div
            className={`search-container ${isSearchOpen ? 'open' : ''}`}
            ref={searchContainerRef}
          >
            <button className="icon-btn" onClick={() => setIsSearchOpen(!isSearchOpen)} aria-label="Search">
              <FiSearch />
            </button>
            {isSearchOpen && (
              <div className="search-form-wrapper">
                <form className="search-form" onSubmit={handleSearch}>
                  <input
                    type="text"
                    placeholder="What are you looking for?"
                    value={searchQuery}
                    onChange={handleSearchInputChange}
                    onFocus={() => searchQuery.length >= 2 && setShowSuggestions(true)}
                    autoFocus
                  />
                  <button type="submit">
                    <FiSearch />
                  </button>
                </form>
                <SearchSuggestions
                  query={searchQuery}
                  isVisible={showSuggestions}
                  onSelect={handleSuggestionSelect}
                  onClose={() => setShowSuggestions(false)}
                />
              </div>
            )}
          </div>

          {/* User Account */}
          <div className="user-menu-container">
            <button
              className="icon-btn"
              onClick={() => isAuthenticated ? setIsUserMenuOpen(!isUserMenuOpen) : navigate('/login')}
            >
              <FiUser />
            </button>
            {isAuthenticated && isUserMenuOpen && (
              <div className="user-dropdown">
                <div className="user-info">
                  <p className="user-name">Hi, {user?.firstName}</p>
                  <p className="user-email">{user?.email}</p>
                </div>
                <ul className="user-menu-list">
                  <li><Link to="/account" onClick={() => setIsUserMenuOpen(false)}>My Account</Link></li>
                  <li><Link to="/orders" onClick={() => setIsUserMenuOpen(false)}>My Orders</Link></li>
                  <li><Link to="/addresses" onClick={() => setIsUserMenuOpen(false)}>Addresses</Link></li>
                  <li><button onClick={handleLogout}>Logout</button></li>
                </ul>
              </div>
            )}
          </div>

          {/* Wishlist */}
          <Link to="/wishlist" className="icon-btn wishlist-btn" aria-label="Wishlist">
            <FiHeart />
            {activeWishlistCount > 0 && (
              <span className="badge">{activeWishlistCount}</span>
            )}
          </Link>

          {/* Cart */}
          <Link to="/cart" className="icon-btn cart-btn" aria-label="Cart">
            <FiShoppingBag />
            {activeCartCount > 0 && (
              <span className="badge">{activeCartCount}</span>
            )}
          </Link>
        </div>
      </div>
    </header>
  );
};

export default Header;
