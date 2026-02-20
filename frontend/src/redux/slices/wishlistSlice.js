import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { wishlistAPI } from '../../services/api';

const initialState = {
  items: [],
  loading: false,
  error: null,
};

export const fetchWishlist = createAsyncThunk(
  'wishlist/fetchWishlist',
  async (_, { rejectWithValue }) => {
    try {
      const response = await wishlistAPI.get();
      return response.data.data;
    } catch (error) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch wishlist');
    }
  }
);

// Accepts { productId, product } — product is the full product object for optimistic add
export const addToWishlist = createAsyncThunk(
  'wishlist/addToWishlist',
  async ({ productId, product }, { rejectWithValue }) => {
    try {
      await wishlistAPI.add(productId);
      // Return the product so reducer can use it if needed
      return { productId, product };
    } catch (error) {
      return rejectWithValue(error.response?.data?.message || 'Failed to add to wishlist');
    }
  }
);

export const removeFromWishlist = createAsyncThunk(
  'wishlist/removeFromWishlist',
  async (productId, { rejectWithValue }) => {
    try {
      await wishlistAPI.remove(productId);
      return productId;
    } catch (error) {
      return rejectWithValue(error.response?.data?.message || 'Failed to remove from wishlist');
    }
  }
);

// Accepts { productId, product } — product is the full product object for optimistic toggle
export const toggleWishlist = createAsyncThunk(
  'wishlist/toggleWishlist',
  async ({ productId, product }, { getState, rejectWithValue }) => {
    const { wishlist } = getState();
    const isCurrentlyInWishlist = wishlist.items.some(item => item.id === productId);

    try {
      if (isCurrentlyInWishlist) {
        await wishlistAPI.remove(productId);
      } else {
        await wishlistAPI.add(productId);
      }
      return { productId, product, wasInWishlist: isCurrentlyInWishlist };
    } catch (error) {
      return rejectWithValue({
        message: error.response?.data?.message || 'Failed to update wishlist',
        productId,
        product,
        wasInWishlist: isCurrentlyInWishlist,
      });
    }
  }
);

const wishlistSlice = createSlice({
  name: 'wishlist',
  initialState,
  reducers: {
    resetWishlist: () => initialState,
  },
  extraReducers: (builder) => {
    builder
      // Fetch
      .addCase(fetchWishlist.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchWishlist.fulfilled, (state, action) => {
        state.loading = false;
        state.items = action.payload || [];
      })
      .addCase(fetchWishlist.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })

      // Add — optimistic
      .addCase(addToWishlist.pending, (state, action) => {
        const { product } = action.meta.arg;
        if (product && !state.items.some(item => item.id === product.id)) {
          state.items.push(product);
        }
      })
      .addCase(addToWishlist.fulfilled, () => {
        // Optimistic state is already correct
      })
      .addCase(addToWishlist.rejected, (state, action) => {
        // Rollback: remove the optimistically added item
        const { productId } = action.meta.arg;
        state.items = state.items.filter(item => item.id !== productId);
        state.error = action.payload;
      })

      // Remove — optimistic
      .addCase(removeFromWishlist.pending, (state, action) => {
        const productId = action.meta.arg;
        state._rollbackItem = state.items.find(item => item.id === productId);
        state.items = state.items.filter(item => item.id !== productId);
      })
      .addCase(removeFromWishlist.fulfilled, (state) => {
        delete state._rollbackItem;
      })
      .addCase(removeFromWishlist.rejected, (state, action) => {
        // Rollback: re-add the removed item
        if (state._rollbackItem) {
          state.items.push(state._rollbackItem);
        }
        delete state._rollbackItem;
        state.error = action.payload;
      })

      // Toggle — optimistic
      .addCase(toggleWishlist.pending, (state, action) => {
        const { productId, product } = action.meta.arg;
        const index = state.items.findIndex(item => item.id === productId);
        if (index >= 0) {
          state._rollbackItem = state.items[index];
          state.items.splice(index, 1);
        } else if (product) {
          state.items.push(product);
        }
      })
      .addCase(toggleWishlist.fulfilled, (state) => {
        delete state._rollbackItem;
      })
      .addCase(toggleWishlist.rejected, (state, action) => {
        // Rollback: reverse the optimistic toggle
        const { productId, product } = action.meta.arg;
        const index = state.items.findIndex(item => item.id === productId);
        if (index >= 0) {
          // Was optimistically added, remove it
          state.items.splice(index, 1);
        } else if (state._rollbackItem) {
          // Was optimistically removed, re-add it
          state.items.push(state._rollbackItem);
        }
        delete state._rollbackItem;
        state.error = typeof action.payload === 'object' ? action.payload.message : action.payload;
      });
  },
});

export const { resetWishlist } = wishlistSlice.actions;
export default wishlistSlice.reducer;
