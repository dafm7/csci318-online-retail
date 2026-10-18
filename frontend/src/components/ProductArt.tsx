import type { Product } from '../api'

// There are no product photos, so each product gets a typographic "catalogue plate":
// a colour per category with the product's initial set large.

const KNOWN: Record<string, string> = { Electronics: 'cobalt', Sport: 'tomato', Home: 'mustard' }
const TONES = ['moss', 'plum', 'sky', 'tomato', 'cobalt', 'mustard']

export function toneFor(category: string) {
  if (KNOWN[category]) return KNOWN[category]
  let h = 0
  for (const ch of category) h = (h * 31 + ch.charCodeAt(0)) | 0
  return TONES[Math.abs(h) % TONES.length]
}

export default function ProductArt({ product, mini }: { product: Pick<Product, 'id' | 'name' | 'category'>; mini?: boolean }) {
  return (
    <div className={`art tone-${toneFor(product.category)}${mini ? ' mini' : ''}`} aria-hidden>
      <span className="art-no">No.{String(product.id).padStart(3, '0')}</span>
      <span className="art-cat">{product.category}</span>
      <span className="art-letter">{product.name.charAt(0)}</span>
    </div>
  )
}
