import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { Provider } from 'react-redux';
import store from './redux/store';
import Layout from './components/layout/Layout';
import Home from './pages/Home';
import ProductList from './pages/ProductList';
import ProductDetail from './pages/ProductDetail';
import Cart from './pages/Cart';
import Checkout from './pages/Checkout';
import ReturnRequest from './pages/ReturnRequest';
import Wishlist from './pages/Wishlist';
import Login from './pages/Login';
import Register from './pages/Register';
import Contact from './pages/Contact';
import TrackOrder from './pages/TrackOrder';
import Returns from './pages/Returns';
import FAQ from './pages/FAQ';
import Account from './pages/Account';
import About from './pages/About';
import Careers from './pages/Careers';
import Blog from './pages/Blog';
import Terms from './pages/Terms';
import Privacy from './pages/Privacy';
import Sitemap from './pages/Sitemap';
import Orders from './pages/Orders';
import OrderDetail from './pages/OrderDetail';
import MyReturns from './pages/MyReturns';
import MyReviews from './pages/MyReviews';
import Addresses from './pages/Addresses';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';

function App() {
  return (
    <Provider store={store}>
      <Router>
        <Layout>
          <Routes>
            {/* Home */}
            <Route path="/" element={<Home />} />

            {/* Products */}
            <Route path="/products" element={<ProductList />} />
            <Route path="/product/:slug" element={<ProductDetail />} />

            {/* Categories */}
            <Route path="/men" element={<ProductList />} />
            <Route path="/women" element={<ProductList />} />
            <Route path="/sneakers" element={<ProductList />} />
            <Route path="/category/:categorySlug" element={<ProductList />} />
            <Route path="/:gender/:categorySlug" element={<ProductList />} />

            {/* Special Collections */}
            <Route path="/new-arrivals" element={<ProductList />} />
            <Route path="/best-sellers" element={<ProductList />} />
            <Route path="/featured" element={<ProductList />} />

            {/* Search */}
            <Route path="/search" element={<ProductList />} />

            {/* User */}
            <Route path="/cart" element={<Cart />} />
            <Route path="/checkout" element={<Checkout />} />
            <Route path="/return/:orderId/:itemId" element={<ReturnRequest />} />
            <Route path="/wishlist" element={<Wishlist />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/forgot-password" element={<ForgotPassword />} />
            <Route path="/reset-password" element={<ResetPassword />} />
            <Route path="/account" element={<Account />} />
            <Route path="/orders" element={<Orders />} />
            <Route path="/orders/:orderId" element={<OrderDetail />} />
            <Route path="/my-returns" element={<MyReturns />} />
            <Route path="/my-reviews" element={<MyReviews />} />
            <Route path="/addresses" element={<Addresses />} />

            {/* Footer Pages */}
            <Route path="/contact" element={<Contact />} />
            <Route path="/track-order" element={<TrackOrder />} />
            <Route path="/returns" element={<Returns />} />
            <Route path="/faq" element={<FAQ />} />
            <Route path="/about" element={<About />} />
            <Route path="/careers" element={<Careers />} />
            <Route path="/blog" element={<Blog />} />
            <Route path="/terms" element={<Terms />} />
            <Route path="/privacy" element={<Privacy />} />
            <Route path="/sitemap" element={<Sitemap />} />

            {/* Fallback */}
            <Route path="*" element={<Home />} />
          </Routes>
        </Layout>
      </Router>
    </Provider>
  );
}

export default App;
