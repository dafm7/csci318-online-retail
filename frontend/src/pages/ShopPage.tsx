import { useEffect, useState } from 'react'
import { api, money, categoryLabel, type Product } from '../api'
import { useStore } from '../store'
import { useAsync } from '../useAsync'
import ProductArt from '../components/ProductArt'

export default function ShopPage() {
  const [search, setSearch] = useState('')
  const [query, setQuery] = useState('')
  const [category, setCategory] = useState('')

  // Wait for typing to pause before hitting product-service
  useEffect(() => {
    const t = setTimeout(() => setQuery(search.trim()), 250)
    return () => clearTimeout(t)
  }, [search])

  const all = useAsync(() => api.products(), [])
  const products = useAsync(() => api.products(query, category), [query, category])

  const counts = new Map<string, number>()
  for (const p of all.data ?? []) counts.set(p.category, (counts.get(p.category) ?? 0) + 1)
  const categories = [...counts.keys()].sort()
  const today = new Date().toLocaleDateString('en-AU', { day: 'numeric', month: 'long', year: 'numeric' })

  return (
    <>
      <section className="shop-head">
        <p className="kicker">
          <span>The catalogue</span>
          <span>{today}</span>
          <span>{all.data ? `${all.data.length} items` : '—'}</span>
        </p>
        <h1 className="display">Good things,<br /><em>in stock now.</em></h1>
        <label className="field search">
          <span>Search</span>
          <input type="search" placeholder="keyboard, mug, shoes…" value={search} onChange={e => setSearch(e.target.value)} />
        </label>
      </section>

      <nav className="cats" aria-label="Category">
        <button className={!category ? 'cat active' : 'cat'} onClick={() => setCategory('')}>
          Everything<sup>{all.data?.length ?? ''}</sup>
        </button>
        {categories.map(c => (
          <button key={c} className={category === c ? 'cat active' : 'cat'} onClick={() => setCategory(c)}>
            {categoryLabel(c)}<sup>{counts.get(c)}</sup>
          </button>
        ))}
      </nav>

      {products.error && (
        <div className="notice" role="alert">
          <span>The product service isn’t answering: {products.error}</span>
          <button className="btn" onClick={products.reload}>Try again</button>
        </div>
      )}

      {products.data?.length === 0 && (
        <div className="empty-state">
          <p className="display" style={{ fontSize: 48 }}>Nothing matches <em>“{query || categoryLabel(category)}”.</em></p>
          <button className="linkish" onClick={() => { setSearch(''); setCategory('') }}>Clear filters</button>
        </div>
      )}

      <div className="catalogue">
        {products.loading && !products.data && !products.error && [1, 2, 3, 4].map(i => <Skeleton key={i} />)}
        {products.data?.map(p => <ProductCard key={p.id} product={p} />)}
      </div>
    </>
  )
}

function ProductCard({ product }: { product: Product }) {
  const { addToCart, cart } = useStore()
  const [added, setAdded] = useState(false)
  const inCart = cart.find(l => l.product.id === product.id)?.quantity ?? 0
  const soldOut = product.stock <= 0
  const full = !soldOut && inCart >= product.stock
  const low = product.availability === 'LOW_STOCK'

  useEffect(() => {
    if (!added) return
    const t = setTimeout(() => setAdded(false), 1400)
    return () => clearTimeout(t)
  }, [added])

  return (
    <article className="item">
      <ProductArt product={product} />
      <div className="item-body">
        <div className="item-row">
          <h3 className="item-name">{product.name}</h3>
          <span className="price">{money(product.price)}</span>
        </div>
        <div className={low ? 'meter low' : 'meter'}>
          <span className="meter-track"><span className="meter-fill" style={{ width: `${Math.min(100, product.stock)}%` }} /></span>
          <span>{soldOut ? 'Sold out' : low ? `Only ${product.stock} left` : `${product.stock} in stock`}</span>
        </div>
        <button
          className={added ? 'add added' : 'add'}
          disabled={soldOut || full}
          onClick={() => { addToCart(product); setAdded(true) }}
        >
          <span>{added ? 'Added to cart' : soldOut ? 'Sold out' : full ? 'All stock in cart' : inCart ? `Add another (${inCart} in cart)` : 'Add to cart'}</span>
          <span className="plus" aria-hidden>{added ? '✓' : '+'}</span>
        </button>
      </div>
    </article>
  )
}

function Skeleton() {
  return (
    <div className="item skeleton" aria-hidden>
      <div className="art" />
      <div className="item-body">
        <div className="skel-line" style={{ width: '70%', height: 22 }} />
        <div className="skel-line" style={{ width: '45%' }} />
      </div>
    </div>
  )
}
