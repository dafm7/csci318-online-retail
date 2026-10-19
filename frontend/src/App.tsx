import { useEffect } from 'react'
import { NavLink, Route, Routes, useLocation } from 'react-router-dom'
import { useStore } from './store'
import { api, ApiError } from './api'
import ShopPage from './pages/ShopPage'
import CartPage from './pages/CartPage'
import LoginPage from './pages/LoginPage'
import AccountPage from './pages/AccountPage'
import AdminPage from './pages/AdminPage'
import ChatPanel from './components/ChatPanel'
import Ticker from './components/Ticker'

export default function App() {
  const { customer, cart, signOut } = useStore()
  const items = cart.reduce((n, l) => n + l.quantity, 0)
  const { pathname } = useLocation()

  useEffect(() => { window.scrollTo(0, 0) }, [pathname])

  // The sign-in is remembered in the browser, but user-service starts empty after a restart:
  // sign out if the remembered user no longer exists, so orders aren't placed for an unknown customer
  useEffect(() => {
    if (!customer) return
    api.profile(customer.id).catch(e => { if (e instanceof ApiError && e.status === 404) signOut() })
  }, [customer?.id])

  return (
    <>
      <Ticker />
      <header className="masthead">
        <div className="masthead-inner">
          <NavLink to="/" className="wordmark" aria-label="UOW Retail home">
            <span className="wordmark-serif">Retail</span>
            <span className="wordmark-mono">UOW</span>
          </NavLink>
          <nav className="mainnav">
            <NavLink to="/" end>Shop</NavLink>
            {customer?.role === 'ADMIN' && <NavLink to="/admin">Admin</NavLink>}
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

      <ChatPanel />
    </>
  )
}
