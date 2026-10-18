import { Link, Navigate } from 'react-router-dom'
import { api, money } from '../api'
import { useStore } from '../store'
import { useAsync } from '../useAsync'
import StatusBadge from '../components/StatusBadge'

export default function AccountPage() {
  const { customer, signOut } = useStore()
  const id = customer?.id
  const profile = useAsync(() => (id ? api.profile(id) : Promise.resolve(null)), [id])
  // Full history (the profile only carries recent orders); refresh so PENDING orders update
  const orders = useAsync(() => (id ? api.orders(id) : Promise.resolve([])), [id], 3000)
  const products = useAsync(() => api.products(), [])

  if (!customer) return <Navigate to="/login" replace />

  const productName = (pid: number) => products.data?.find(p => p.id === pid)?.name ?? `Product #${pid}`
  const p = profile.data
  const history = [...(orders.data ?? [])].sort((a, b) => b.id - a.id)
  const paid = history.filter(o => o.status === 'PAID')
  const declined = history.filter(o => o.status === 'FAILED').length
  const since = new Date(p?.memberSince ?? customer.registeredAt)

  return (
    <>
      <section className="account-top">
        <div>
          <p className="kicker">Your account</p>
          <h1 className="display">Hi, <em>{customer.name.split(' ')[0]}.</em></h1>
        </div>
        <div className="member-card" aria-label="Membership card">
          <div className="wordmark"><span className="wordmark-serif">Retail</span></div>
          <div>
            <p className="member-name">{customer.name}</p>
            <div className="member-meta" style={{ marginTop: 10 }}>
              <span>No. {String(customer.id).padStart(4, '0')}</span>
              <span>Since {since.toLocaleDateString('en-AU', { month: 'short', year: 'numeric' })}</span>
            </div>
          </div>
        </div>
      </section>

      <div className="details">
        <span>{p?.email ?? customer.email}</span>
        <span>{(p?.address ?? customer.address) || 'No address on file'}</span>
        <button className="linkish" onClick={signOut}>Sign out</button>
      </div>

      {profile.error && <div className="notice" role="alert"><span>Couldn’t load your profile: {profile.error}</span></div>}

      <div className="figures">
        <div className="figure"><span className="kicker">Orders</span><strong>{history.length}</strong></div>
        <div className="figure"><span className="kicker">Paid</span><strong>{paid.length}</strong></div>
        <div className={declined ? 'figure bad' : 'figure'}><span className="kicker">Declined</span><strong>{declined}</strong></div>
        <div className="figure signal"><span className="kicker">Total spent</span><strong>{money(paid.reduce((s, o) => s + o.amount, 0))}</strong></div>
      </div>

      <section>
        <div className="section-title">
          <h2>Order history</h2>
          <span className="kicker">Updates live</span>
        </div>
        {orders.error && <p className="error-text">{orders.error}</p>}
        {history.length === 0 && !orders.loading && (
          <p className="muted">No orders yet. <Link to="/">Find something you like →</Link></p>
        )}
        {history.length > 0 && (
          <div className="table-wrap">
            <table className="ledger">
              <thead><tr><th>Order</th><th>Product</th><th className="num hide-sm">Qty</th><th className="num">Amount</th><th>Status</th></tr></thead>
              <tbody>
                {history.map(o => (
                  <tr key={o.id}>
                    <td className="id">#{String(o.id).padStart(3, '0')}</td>
                    <td className="name">{productName(o.productId)}</td>
                    <td className="num hide-sm">{o.quantity}</td>
                    <td className="num">{money(o.amount)}</td>
                    <td><StatusBadge status={o.status} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </>
  )
}
