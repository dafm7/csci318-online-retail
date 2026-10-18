import { useState, type FormEvent } from 'react'
import { api, money, LOW_STOCK } from '../api'
import { useAsync } from '../useAsync'

const REFRESH_MS = 5000

export default function AdminPage() {
  const inventory = useAsync(api.inventory, [], REFRESH_MS)
  const sales = useAsync(api.sales, [], REFRESH_MS)
  const perMinute = useAsync(() => api.salesPerMinute(5), [], REFRESH_MS)
  const payments = useAsync(api.payments, [], REFRESH_MS)

  const stock = [...(inventory.data ?? [])].sort((a, b) => a.stock - b.stock)
  const maxStock = Math.max(1, ...stock.map(s => s.stock))
  const low = stock.filter(s => s.stock < LOW_STOCK)
  const revenue = (sales.data ?? []).reduce((s, x) => s + x.revenue, 0)
  const units = (sales.data ?? []).reduce((s, x) => s + x.unitsSold, 0)
  const declined = (payments.data ?? []).filter(p => !p.success).length

  // Per-minute rows grouped by window, newest first
  const windows = new Map<string, { label: string; rows: { name: string; units: number }[] }>()
  for (const m of perMinute.data ?? []) {
    const w = windows.get(m.windowStart) ?? { label: timeRange(m.windowStart, m.windowEnd), rows: [] }
    w.rows.push({ name: m.name, units: m.unitsSold })
    windows.set(m.windowStart, w)
  }
  const minuteGroups = [...windows.entries()].sort(([a], [b]) => b.localeCompare(a)).map(([, w]) => w)
  const maxMinute = Math.max(1, ...(perMinute.data ?? []).map(m => m.unitsSold))

  return (
    <>
      <header className="page-head">
        <div>
          <p className="kicker">Back office</p>
          <h1 className="display">Control <em>room.</em></h1>
        </div>
        <span className="live-badge"><span className="live-dot" />LIVE · {REFRESH_MS / 1000}s</span>
      </header>

      <div className="figures">
        <div className="figure signal"><span className="kicker">Revenue · paid</span><strong>{money(revenue)}</strong></div>
        <div className="figure"><span className="kicker">Units sold</span><strong>{units}</strong></div>
        <div className={low.length ? 'figure signal' : 'figure'}><span className="kicker">Low stock</span><strong>{low.length}</strong></div>
        <div className={declined ? 'figure bad' : 'figure'}><span className="kicker">Declined</span><strong>{declined}</strong></div>
      </div>

      <div className="admin-grid">
        <section className="panel span-7">
          <h2>Inventory</h2>
          <p className="kicker">Kafka Streams · flagged below {LOW_STOCK} units</p>
          <Err msg={inventory.error} />
          {stock.length === 0 && !inventory.loading && !inventory.error && <p className="muted">No stock data yet.</p>}
          <ul className="bars">
            {stock.map(s => (
              <li key={s.productId} className={s.stock < LOW_STOCK ? 'low' : ''}>
                <span className="bar-label">{s.name}</span>
                <span className="bar-track"><span className="bar-fill" style={{ width: `${(s.stock / maxStock) * 100}%` }} /></span>
                <span className="bar-value">{s.stock}</span>
              </li>
            ))}
          </ul>
        </section>

        <section className="panel span-5">
          <h2>Best sellers</h2>
          <p className="kicker">Paid orders only</p>
          <Err msg={sales.error} />
          {sales.data?.length === 0 && <p className="muted">No paid orders yet.</p>}
          <ol className="rank">
            {sales.data?.map(s => (
              <li key={s.productId}>
                <span className="name">{s.name}</span>
                <span className="rev">{money(s.revenue)}</span>
                <span className="sub">{s.unitsSold} {s.unitsSold === 1 ? 'unit' : 'units'} ·{s.orders} {s.orders === 1 ? 'order' : 'orders'}</span>
              </li>
            ))}
          </ol>
        </section>

        <section className="panel">
          <h2>Per minute</h2>
          <p className="kicker">Units sold · 1-minute windows · last 5 min</p>
          <Err msg={perMinute.error} />
          {minuteGroups.length === 0 && !perMinute.loading && !perMinute.error && <p className="muted">Nothing sold in the last five minutes.</p>}
          {minuteGroups.map(w => (
            <div key={w.label} className="minute">
              <div className="minute-label">{w.label}</div>
              <ul className="bars thin">
                {w.rows.map(r => (
                  <li key={r.name}>
                    <span className="bar-label">{r.name}</span>
                    <span className="bar-track"><span className="bar-fill" style={{ width: `${(r.units / maxMinute) * 100}%` }} /></span>
                    <span className="bar-value">{r.units}</span>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </section>

        <section className="panel">
          <h2>Payments</h2>
          <p className="kicker">Newest first · limit {money(1000)}</p>
          <Err msg={payments.error} />
          {payments.data?.length === 0 && <p className="muted">No payments yet.</p>}
          {!!payments.data?.length && (
            <div className="scroll table-wrap">
              <table className="ledger">
                <thead><tr><th>Payment</th><th>Order</th><th className="num">Amount</th><th>Result</th></tr></thead>
                <tbody>
                  {[...payments.data].reverse().map(p => (
                    <tr key={p.id}>
                      <td className="id">#{p.id}</td>
                      <td className="id">#{p.orderId}</td>
                      <td className="num">{money(p.amount)}</td>
                      <td><span className={`pill ${p.success ? 'pill-paid' : 'pill-failed'}`}>{p.success ? 'APPROVED' : 'DECLINED'}</span></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        <AddProduct onAdded={inventory.reload} />
      </div>
    </>
  )
}

function AddProduct({ onAdded }: { onAdded: () => void }) {
  const empty = { name: '', category: '', price: '', stock: '' }
  const [form, setForm] = useState(empty)
  const [msg, setMsg] = useState<{ ok: boolean; text: string } | null>(null)
  const set = (k: keyof typeof form) => (e: { target: { value: string } }) => setForm(f => ({ ...f, [k]: e.target.value }))

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    try {
      const p = await api.addProduct({ name: form.name, category: form.category, price: Number(form.price), stock: Number(form.stock) })
      setMsg({ ok: true, text: `Added ${p.name} as No.${String(p.id).padStart(3, '0')}` })
      setForm(empty)
      onAdded()
    } catch (err) {
      setMsg({ ok: false, text: err instanceof Error ? err.message : String(err) })
    }
  }

  return (
    <section className="panel wide">
      <h2>New product</h2>
      <p className="kicker">Adds to the catalogue straight away</p>
      <form className="add-form" onSubmit={submit}>
        <label className="field"><span>Name</span><input required value={form.name} onChange={set('name')} placeholder="Gaming Headset" /></label>
        <label className="field"><span>Category</span><input required value={form.category} onChange={set('category')} placeholder="Electronics" /></label>
        <label className="field"><span>Price</span><input required type="number" min="0.01" step="0.01" value={form.price} onChange={set('price')} placeholder="79.95" /></label>
        <label className="field"><span>Stock</span><input required type="number" min="0" step="1" value={form.stock} onChange={set('stock')} placeholder="15" /></label>
        <button className="btn btn-ink">Add <span className="arrow">→</span></button>
      </form>
      {msg && <p className={msg.ok ? 'ok-text' : 'error-text'} style={{ marginTop: 16 }}>{msg.text}</p>}
    </section>
  )
}

function Err({ msg }: { msg: string | null }) {
  return msg ? <p className="error-text">{msg}</p> : null
}

function timeRange(start: string, end: string) {
  const f = (s: string) => new Date(s).toLocaleTimeString('en-AU', { hour: '2-digit', minute: '2-digit' })
  return `${f(start)} – ${f(end)}`
}
