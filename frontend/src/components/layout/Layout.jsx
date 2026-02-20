import { useEffect, useRef } from 'react';
import { useLocation } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { Toaster } from 'react-hot-toast';
import Header from './Header';
import Footer from './Footer';
import { fetchCart, addToCart } from '../../redux/slices/cartSlice';
import { fetchWishlist, addToWishlist } from '../../redux/slices/wishlistSlice';
import { clearGuestCart } from '../../redux/slices/guestCartSlice';
import { clearGuestWishlist } from '../../redux/slices/guestWishlistSlice';
import { syncUserProfile } from '../../redux/slices/authSlice';
import './Layout.css';

const Layout = ({ children }) => {
  const location = useLocation();
  const dispatch = useDispatch();
  const { isAuthenticated } = useSelector((state) => state.auth);
  const { items: guestCartItems } = useSelector((state) => state.guestCart);
  const { items: guestWishlistItems } = useSelector((state) => state.guestWishlist);
  const prevAuthRef = useRef(isAuthenticated);
  const guestCartRef = useRef(guestCartItems);
  const guestWishlistRef = useRef(guestWishlistItems);

  // Keep refs in sync with latest guest data
  useEffect(() => {
    guestCartRef.current = guestCartItems;
  }, [guestCartItems]);

  useEffect(() => {
    guestWishlistRef.current = guestWishlistItems;
  }, [guestWishlistItems]);

  // Scroll to top on route change
  useEffect(() => {
    window.scrollTo(0, 0);
  }, [location.pathname]);

  // Fetch cart, wishlist, and sync profile when authenticated
  useEffect(() => {
    if (isAuthenticated) {
      dispatch(fetchCart());
      dispatch(fetchWishlist());
      dispatch(syncUserProfile());
    }
  }, [isAuthenticated, dispatch]);

  // Merge guest cart/wishlist into server when user logs in
  useEffect(() => {
    const wasAuthenticated = prevAuthRef.current;
    prevAuthRef.current = isAuthenticated;

    if (!wasAuthenticated && isAuthenticated) {
      // Snapshot guest data at the moment of login before it gets cleared
      const cartItemsToMerge = [...guestCartRef.current];
      const wishlistItemsToMerge = [...guestWishlistRef.current];

      // Clear guest data immediately to prevent stale references
      dispatch(clearGuestCart());
      dispatch(clearGuestWishlist());

      const mergeGuestData = async () => {
        // Merge guest cart items (backend handles duplicates by incrementing quantity)
        for (const item of cartItemsToMerge) {
          try {
            await dispatch(addToCart({
              productId: item.productId,
              variantId: item.variantId,
              quantity: item.quantity,
            })).unwrap();
          } catch (err) {
            console.warn('Failed to merge cart item:', err);
          }
        }

        // Merge guest wishlist items
        for (const item of wishlistItemsToMerge) {
          try {
            await dispatch(addToWishlist({ productId: item.id, product: item })).unwrap();
          } catch (err) {
            console.warn('Failed to merge wishlist item:', err);
          }
        }

        // Re-fetch to get accurate server state
        dispatch(fetchCart());
        dispatch(fetchWishlist());
      };

      mergeGuestData();
    }
  }, [isAuthenticated, dispatch]);

  return (
    <div className="layout">
      <Header />
      <main className="main-content">
        {children}
      </main>
      <Footer />
      <Toaster
        position="bottom-right"
        toastOptions={{
          duration: 3000,
          style: {
            background: '#333',
            color: '#fff',
            borderRadius: '8px',
          },
          success: {
            style: {
              background: '#28a745',
            },
          },
          error: {
            style: {
              background: '#dc3545',
            },
          },
        }}
      />
    </div>
  );
};

export default Layout;
