import { configureStore } from '@reduxjs/toolkit';
import authReducer from './slices/authSlice';
import cartReducer from './slices/cartSlice';
import wishlistReducer from './slices/wishlistSlice';
import guestCartReducer from './slices/guestCartSlice';
import guestWishlistReducer from './slices/guestWishlistSlice';

export const store = configureStore({
  reducer: {
    auth: authReducer,
    cart: cartReducer,
    wishlist: wishlistReducer,
    guestCart: guestCartReducer,
    guestWishlist: guestWishlistReducer,
  },
});

export default store;
