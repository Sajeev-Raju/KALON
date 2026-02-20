import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { cartAPI } from '../../services/api';

const initialState = {
  items: [],
  subtotal: 0,
  shippingCost: 0,
  total: 0,
  itemCount: 0,
  hasStockIssues: false,
  loading: false,
  error: null,
  _previousItems: null,
};

const applyCartPayload = (state, payload) => {
  state.items = payload.items || [];
  state.subtotal = payload.subtotal || 0;
  state.shippingCost = payload.shippingCost || 0;
  state.total = payload.total || 0;
  state.itemCount = payload.itemCount || 0;
  state.hasStockIssues = payload.hasStockIssues || false;
};

export const fetchCart = createAsyncThunk(
  'cart/fetchCart',
  async (_, { rejectWithValue }) => {
    try {
      const response = await cartAPI.get();
      return response.data.data;
    } catch (error) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch cart');
    }
  }
);

export const addToCart = createAsyncThunk(
  'cart/addToCart',
  async (data, { rejectWithValue }) => {
    try {
      const response = await cartAPI.add(data);
      return response.data.data;
    } catch (error) {
      const errorData = error.response?.data;
      if (errorData?.data?.errorCode === 'INSUFFICIENT_STOCK') {
        return rejectWithValue({
          message: errorData.message,
          stockError: errorData.data,
        });
      }
      return rejectWithValue({
        message: errorData?.message || 'Failed to add to cart',
      });
    }
  }
);

export const updateCartItemQuantity = createAsyncThunk(
  'cart/updateQuantity',
  async ({ itemId, quantity }, { rejectWithValue }) => {
    try {
      const response = await cartAPI.updateQuantity(itemId, quantity);
      return response.data.data;
    } catch (error) {
      const errorData = error.response?.data;
      if (errorData?.data?.errorCode === 'INSUFFICIENT_STOCK') {
        return rejectWithValue({
          message: errorData.message,
          stockError: errorData.data,
        });
      }
      return rejectWithValue({
        message: errorData?.message || 'Failed to update quantity',
      });
    }
  }
);

export const removeFromCart = createAsyncThunk(
  'cart/removeFromCart',
  async (itemId, { rejectWithValue }) => {
    try {
      const response = await cartAPI.remove(itemId);
      return response.data.data;
    } catch (error) {
      return rejectWithValue(error.response?.data?.message || 'Failed to remove from cart');
    }
  }
);

export const clearCart = createAsyncThunk(
  'cart/clearCart',
  async (_, { rejectWithValue }) => {
    try {
      await cartAPI.clear();
      return initialState;
    } catch (error) {
      return rejectWithValue(error.response?.data?.message || 'Failed to clear cart');
    }
  }
);

export const validateCart = createAsyncThunk(
  'cart/validateCart',
  async (_, { rejectWithValue }) => {
    try {
      const response = await cartAPI.validate();
      return response.data.data;
    } catch (error) {
      return rejectWithValue(error.response?.data?.message || 'Failed to validate cart');
    }
  }
);

const cartSlice = createSlice({
  name: 'cart',
  initialState,
  reducers: {
    resetCart: () => initialState,
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchCart.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchCart.fulfilled, (state, action) => {
        state.loading = false;
        applyCartPayload(state, action.payload);
      })
      .addCase(fetchCart.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })
      .addCase(addToCart.pending, (state) => {
        state.loading = true;
      })
      .addCase(addToCart.fulfilled, (state, action) => {
        state.loading = false;
        applyCartPayload(state, action.payload);
      })
      .addCase(addToCart.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })
      // Optimistic update for quantity changes
      .addCase(updateCartItemQuantity.pending, (state, action) => {
        const { itemId, quantity } = action.meta.arg;
        const item = state.items.find((i) => i.id === itemId);
        if (item) {
          // Snapshot for rollback
          state._previousItems = JSON.parse(JSON.stringify(state.items));
          item.quantity = quantity;
          item.totalPrice = item.price * quantity;
          state.subtotal = state.items.reduce((sum, i) => sum + (i.price * i.quantity), 0);
          state.shippingCost = state.subtotal >= 999 ? 0 : 99;
          state.total = state.subtotal + state.shippingCost;
          state.itemCount = state.items.reduce((sum, i) => sum + i.quantity, 0);
        }
      })
      .addCase(updateCartItemQuantity.fulfilled, (state, action) => {
        state._previousItems = null;
        applyCartPayload(state, action.payload);
      })
      .addCase(updateCartItemQuantity.rejected, (state, action) => {
        // Rollback to previous state
        if (state._previousItems) {
          state.items = state._previousItems;
          state._previousItems = null;
          state.subtotal = state.items.reduce((sum, i) => sum + (i.price * i.quantity), 0);
          state.shippingCost = state.subtotal >= 999 ? 0 : 99;
          state.total = state.subtotal + state.shippingCost;
          state.itemCount = state.items.reduce((sum, i) => sum + i.quantity, 0);
        }
        state.error = action.payload;
      })
      .addCase(removeFromCart.fulfilled, (state, action) => {
        applyCartPayload(state, action.payload);
      })
      .addCase(validateCart.fulfilled, (state, action) => {
        applyCartPayload(state, action.payload);
      })
      .addCase(clearCart.fulfilled, () => initialState);
  },
});

export const { resetCart } = cartSlice.actions;
export default cartSlice.reducer;
