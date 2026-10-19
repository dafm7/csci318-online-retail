import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, money, PAYMENT_LIMIT, type Order, categoryLabel } from '../api'
import { useStore, type CartLine } from '../store'
import ProductArt from '../components/ProductArt'
import OrderTracker from '../components/OrderTracker'

// The whole cart becomes one order (one item per cart line), then the order is paid
interface Placed { lines: CartLine[]; order?: Order; error?: string }

export default function CartPage() {
  const { cart, customer, setQuantity, removeFromCart, clearCart } = useStore()
  const [placed, setPlaced] = useState<Placed | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const total = cart.reduce((sum, l) => sum + l.product.price * l.quantity, 0)
  const items = cart.reduce((n, l) => n + l.quantity, 0)

  const checkout = async () => {
    if (!customer) return
    setBusy(true)
    setError(null)
    const lines = cart
    let order: Order
    try {
      order = await api.placeOrder(customer.id, lines.map(l => ({ productId: l.product.id, quantity: l.quantity })))
    } catch (e) {
      // Not placed (e.g. stock ran out): keep the cart so the customer can adjust it
      setError(e instanceof Error ? e.message : String(e))
      setBusy(false)
      return
    }
    clearCart()
    try {
      await api.pay(order.id)
      setPlaced({ lines, order })
    } catch (e) {
      setPlaced({ lines, order, error: `Payment not taken: ${e instanceof Error ? e.message : String(e)}. You can pay from your account page.` })
    }
    setBusy(false)
  }

  // Payment is confirmed asynchronously over Kafka: poll until the order leaves PENDING
  useEffect(() => {
    if (placed?.order?.status !== 'PENDING' || placed.error) return
    const t = setTimeout(async () => {
      const order = await api.order(placed.order!.id).catch(() => placed.order)
      setPlaced(p => (p ? { ...p, order } : p))
    }, 1000)
    return () => clearTimeout(t)
  }, [placed])

  if (placed) {
    const status = placed.order?.status
    return (
      <>
        <header className="page-head">
          <h1 className="display">
            {status === 'PENDING' && !placed.error ? <>Processing<em>…</em></> : status === 'PAID' ? <>Thank <em>you.</em></> : <>Not this <em>time.</em></>}
          </h1>
          <p className="kicker">
            {placed.order ? `Order #${placed.order.id} · ${money(placed.order.amount)}` : 'Not placed'} · over {money(PAYMENT_LIMIT)} is declined
          </p>
        </header>
        <div className="confirm">
          {placed.lines.map(l => (
            <div className="confirm-row" key={l.product.id}>
              <ProductArt product={l.product} mini />
              <div>
                <p className="kicker">qty {l.quantity}</p>
                <p className="line-name">{l.product.name}</p>
                <p className="mono small">{money(l.product.price * l.quantity)}</p>
              </div>
            </div>
          ))}
          {placed.order && <OrderTracker status={placed.order.status} />}
          {placed.error && <p className="error-text">{placed.error}</p>}
        </div>
        <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', marginTop: 32 }}>
          <Link to="/" className="btn">Keep shopping</Link>
          <Link to="/account" className="btn btn-ink">See all my orders <span className="arrow">→</span></Link>
        </div>
      </>
    )
  }

  if (cart.length === 0) {
    return (
      <div className="empty-state">
        <p className="kicker">Your cart</p>
        <h1 className="display">Empty, <em>for now.</em></h1>
        <Link to="/" className="btn btn-ink">Browse the catalogue <span className="arrow">→</span></Link>
      </div>
    )
  }

  return (
    <>
      <header className="page-head">
        <h1 className="display">Your <em>cart.</em></h1>
        <p className="kicker">{items} {items === 1 ? 'item' : 'items'}</p>
      </header>
      <div className="cart-layout">
        <ul className="lines">
          {cart.map(l => {
            const subtotal = l.product.price * l.quantity
            return (
              <li className="line" key={l.product.id}>
                <ProductArt product={l.product} mini />
                <div>
                  <p className="kicker">{categoryLabel(l.product.category)} · {money(l.product.price)} each</p>
                  <p className="line-name">{l.product.name}</p>
                  <div className="line-meta">
                    <div className="stepper" role="group" aria-label={`Quantity of ${l.product.name}`}>
                      <button onClick={() => setQuantity(l.product.id, l.quantity - 1)} disabled={l.quantity <= 1} aria-label="One fewer">−</button>
                      <output>{l.quantity}</output>
                      <button onClick={() => setQuantity(l.product.id, l.quantity + 1)} disabled={l.quantity >= l.product.stock} aria-label="One more">+</button>
                    </div>
                    <button className="linkish" onClick={() => removeFromCart(l.product.id)}>Remove</button>
                  </div>
                </div>
                <div className="line-total">
                  <span className="price">{money(subtotal)}</span>
                </div>
              </li>
            )
          })}
        </ul>

        <aside className="receipt" aria-label="Order summary">
          <h2>Summary</h2>
          <p className="kicker">{new Date().toLocaleDateString('en-AU')}</p>
          {cart.map(l => (
            <div className="receipt-row" key={l.product.id}>
              <span>{l.quantity} × {l.product.name}</span>
              <span>{money(l.product.price * l.quantity)}</span>
            </div>
          ))}
          <hr />
          <div className="receipt-row"><span>Shipping</span><span>Free</span></div>
          <hr />
          <div className="receipt-row receipt-total"><span>Total</span><span>{money(total)}</span></div>
          {total > PAYMENT_LIMIT && <span className="warn-tag">Over {money(PAYMENT_LIMIT)} · payment will be declined</span>}
          <p className="receipt-note">
            Everything in your cart becomes one order. Prices and stock are checked when you place it, and a total over {money(PAYMENT_LIMIT)} is declined.
          </p>
          {error && <p className="error-text" role="alert">{error}</p>}
          {customer
            ? <button className="btn btn-ink btn-wide" disabled={busy} onClick={checkout}>
                {busy ? 'Placing order…' : <>Place order <span className="arrow">→</span></>}
              </button>
            : <Link to="/login?next=/cart" className="btn btn-ink btn-wide">Sign in to check out <span className="arrow">→</span></Link>}
        </aside>
      </div>
    </>
  )
}
