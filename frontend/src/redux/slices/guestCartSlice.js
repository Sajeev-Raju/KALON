import { createSlice } from '@reduxjs/toolkit';

const STORAGE_KEY = 'guestCart';

const loadFromStorage = () => {
  try {
    const data = localStorage.getItem(STORAGE_KEY);
    return data ? JSON.parse(data) : { items: [] };
  } catch {
    return { items: [] };
  }
};

const saveToStorage = (items) => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify({ items }));
};

const calcTotals = (items) => {
  const subtotal = items.reduce((sum, item) => sum + item.price * item.quantity, 0);
  const shippingCost = subtotal >= 999 ? 0 : 99;
  return {
    subtotal,
    shippingCost,
    total: subtotal + shippingCost,
    itemCount: items.reduce((sum, item) => sum + item.quantity, 0),
  };
};

const stored = loadFromStorage();

const initialState = {
  items: stored.items,
  ...calcTotals(stored.items),
};

const guestCartSlice = createSlice({
  name: 'guestCart',
  initialState,
  reducers: {
    addGuestCartItem: (state, action) => {
      const { productId, variantId, quantity, productName, productImage, size, color, price, stockQuantity } = action.payload;
      const existing = state.items.find(
        (item) => item.productId === productId && item.variantId === variantId
      );
      if (existing) {
        const maxQty = stockQuantity || Infinity;
        existing.quantity = Math.min(existing.quantity + quantity, maxQty);
        existing.totalPrice = existing.price * existing.quantity;
        existing.stockQuantity = stockQuantity;
      } else {
        const clampedQty = stockQuantity ? Math.min(quantity, stockQuantity) : quantity;
        state.items.push({
          id: `guest_${productId}_${variantId}`,
          productId,
          variantId,
          quantity: clampedQty,
          productName,
          productImage,
          size,
          color,
          price,
          totalPrice: price * clampedQty,
          stockQuantity: stockQuantity || null,
          isAvailable: true,
        });
      }
      Object.assign(state, calcTotals(state.items));
      saveToStorage(state.items);
    },
    updateGuestCartQuantity: (state, action) => {
      const { itemId, quantity } = action.payload;
      const item = state.items.find((i) => i.id === itemId);
      if (item && quantity >= 1) {
        const maxQty = item.stockQuantity || Infinity;
        item.quantity = Math.min(quantity, maxQty);
        item.totalPrice = item.price * item.quantity;
        Object.assign(state, calcTotals(state.items));
        saveToStorage(state.items);
      }
    },
    removeGuestCartItem: (state, action) => {
      state.items = state.items.filter((i) => i.id !== action.payload);
      Object.assign(state, calcTotals(state.items));
      saveToStorage(state.items);
    },
    clearGuestCart: (state) => {
      state.items = [];
      state.subtotal = 0;
      state.shippingCost = 0;
      state.total = 0;
      state.itemCount = 0;
      localStorage.removeItem(STORAGE_KEY);
    },
  },
});

export const { addGuestCartItem, updateGuestCartQuantity, removeGuestCartItem, clearGuestCart } =
  guestCartSlice.actions;
export default guestCartSlice.reducer;
