import { useEffect } from 'react'
import { NavLink, Route, Routes, useLocation } from 'react-router-dom'
import { useStore } from './store'
import ShopPage from './pages/ShopPage'
import CartPage from './pages/CartPage'
import LoginPage from './pages/LoginPage'
import AccountPage from './pages/AccountPage'
import AdminPage from './pages/AdminPage'
import ChatPanel from './components/ChatPanel'
import Ticker from './components/Ticker'

export default function App() {
  const { customer, cart } = useStore()
  const items = cart.reduce((n, l) => n + l.quantity, 0)
  const { pathname } = useLocation()

  useEffect(() => { window.scrollTo(0, 0) }, [pathname])

  return (
    <>
      <Ticker />
      <header className="masthead">
        <div className="masthead-inner">
          <NavLink to="/" className="wordmark" aria-label="UOW Retail home">
            <span className="wordmark-serif">Retail</span>
            <span className="wordmark-mono">UOW · 318</span>
          </NavLink>
          <nav className="mainnav">
            <NavLink to="/" end>Shop</NavLink>
            <NavLink to="/admin">Admin</NavLink>
            <NavLink to={customer ? '/account' : '/login'}>{customer ? customer.name.split(' ')[0] : 'Sign in'}</NavLink>
            <NavLink to="/cart" className="cartlink" aria-label={`Cart, ${items} items`}>
              Cart
              {/* key remounts the badge so the bump animation replays on every change */}
              <span key={items} className={items ? 'cart-count' : 'cart-count empty'}>{items}</span>
            </NavLink>
          </nav>
        </div>
      </header>

      <main className="page" key={pathname}>
        <Routes>
          <Route path="/" element={<ShopPage />} />
          <Route path="/cart" element={<CartPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/account" element={<AccountPage />} />
          <Route path="/admin" element={<AdminPage />} />
          <Route path="*" element={
            <div className="empty-state">
              <h1 className="display">Nothing <em>here.</em></h1>
              <NavLink to="/" className="btn btn-ink">Back to the shop <span className="arrow">→</span></NavLink>
            </div>
          } />
        </Routes>
      </main>

      <footer className="colophon">
        <div className="colophon-inner">
          <p className="display">Five services, <em>one shop.</em></p>
          <ul className="services">
            <li><b>product</b> :8081</li>
            <li><b>customer</b> :8082</li>
            <li><b>order</b> :8083</li>
            <li><b>payment</b> :8084</li>
            <li><b>agent</b> :8085</li>
          </ul>
        </div>
      </footer>

      <ChatPanel />
    </>
  )
}
