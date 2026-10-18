import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, money, PAYMENT_LIMIT, type Order } from '../api'
import { useStore, type CartLine } from '../store'
import ProductArt from '../components/ProductArt'
import OrderTracker from '../components/OrderTracker'

interface Placed { line: CartLine; order?: Order; error?: string }

export default function CartPage() {
  const { cart, customer, setQuantity, removeFromCart, clearCart } = useStore()
  const [placed, setPlaced] = useState<Placed[] | null>(null)
  const [busy, setBusy] = useState(false)
  const total = cart.reduce((sum, l) => sum + l.product.price * l.quantity, 0)
  const items = cart.reduce((n, l) => n + l.quantity, 0)

  // Each order holds one product, so checkout places one order per cart line
  const checkout = async () => {
    if (!customer) return
    setBusy(true)
    const results = await Promise.all(cart.map(async (line): Promise<Placed> => {
      try {
        const order = await api.placeOrder({
          customerId: customer.id,
          productId: line.product.id,
          quantity: line.quantity,
          amount: Math.round(line.product.price * line.quantity * 100) / 100,
        })
        return { line, order }
      } catch (e) {
        return { line, error: e instanceof Error ? e.message : String(e) }
      }
    }))
    setPlaced(results)
    clearCart()
    setBusy(false)
  }

  // Payment happens asynchronously over Kafka: poll until every order leaves PENDING
  useEffect(() => {
    if (!placed?.some(p => p.order?.status === 'PENDING')) return
    const t = setTimeout(async () => {
      const next = await Promise.all(placed.map(async p =>
        p.order?.status === 'PENDING' ? { ...p, order: await api.order(p.order.id).catch(() => p.order) } : p))
      setPlaced(next)
    }, 1000)
    return () => clearTimeout(t)
  }, [placed])

  if (placed) {
    const pending = placed.some(p => p.order?.status === 'PENDING')
    const nonePaid = !placed.some(p => p.order?.status === 'PAID')
    return (
      <>
        <header className="page-head">
          <h1 className="display">
            {pending ? <>Processing<em>…</em></> : nonePaid ? <>Not this <em>time.</em></> : <>Thank <em>you.</em></>}
          </h1>
          <p className="kicker">Payments clear in the background · over {money(PAYMENT_LIMIT)} is declined</p>
        </header>
        <div className="confirm">
          {placed.map(p => (
            <div className="confirm-row" key={p.line.product.id}>
              <ProductArt product={p.line.product} mini />
              <div>
                <p className="kicker">{p.order ? `Order #${p.order.id}` : 'Not placed'} · qty {p.line.quantity}</p>
                <p className="line-name">{p.line.product.name}</p>
                <p className="mono small">{money(p.line.product.price * p.line.quantity)}</p>
              </div>
              {p.order ? <OrderTracker status={p.order.status} /> : <p className="error-text">{p.error}</p>}
            </div>
          ))}
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
                  <p className="kicker">{l.product.category} · {money(l.product.price)} each</p>
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
                  {subtotal > PAYMENT_LIMIT && <span className="warn-tag">Over limit · will be declined</span>}
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
          <p className="receipt-note">
            Each line becomes its own order. Payment is checked per order and anything over {money(PAYMENT_LIMIT)} is declined.
          </p>
          {customer
            ? <button className="btn btn-ink btn-wide" disabled={busy} onClick={checkout}>
                {busy ? 'Placing orders…' : <>Place order <span className="arrow">→</span></>}
              </button>
            : <Link to="/login?next=/cart" className="btn btn-ink btn-wide">Sign in to check out <span className="arrow">→</span></Link>}
        </aside>
      </div>
    </>
  )
}
