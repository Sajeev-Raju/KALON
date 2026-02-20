import { createSlice } from '@reduxjs/toolkit';

const STORAGE_KEY = 'guestWishlist';

const loadFromStorage = () => {
  try {
    const data = localStorage.getItem(STORAGE_KEY);
    return data ? JSON.parse(data) : [];
  } catch {
    return [];
  }
};

const saveToStorage = (items) => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
};

const initialState = {
  items: loadFromStorage(),
};

const guestWishlistSlice = createSlice({
  name: 'guestWishlist',
  initialState,
  reducers: {
    addGuestWishlistItem: (state, action) => {
      const product = action.payload;
      const exists = state.items.some((item) => item.id === product.id);
      if (!exists) {
        state.items.push(product);
        saveToStorage(state.items);
      }
    },
    removeGuestWishlistItem: (state, action) => {
      const productId = action.payload;
      state.items = state.items.filter((item) => item.id !== productId);
      saveToStorage(state.items);
    },
    toggleGuestWishlistItem: (state, action) => {
      const product = action.payload;
      const index = state.items.findIndex((item) => item.id === product.id);
      if (index >= 0) {
        state.items.splice(index, 1);
      } else {
        state.items.push(product);
      }
      saveToStorage(state.items);
    },
    clearGuestWishlist: (state) => {
      state.items = [];
      localStorage.removeItem(STORAGE_KEY);
    },
  },
});

export const {
  addGuestWishlistItem,
  removeGuestWishlistItem,
  toggleGuestWishlistItem,
  clearGuestWishlist,
} = guestWishlistSlice.actions;
export default guestWishlistSlice.reducer;
