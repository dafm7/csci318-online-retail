import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import type { Customer, Product } from './api'

// Signed-in customer and cart, both kept in localStorage so a refresh doesn't lose them.

export interface CartLine { product: Product; quantity: number }

interface Store {
  customer: Customer | null
  signIn: (c: Customer) => void
  signOut: () => void
  cart: CartLine[]
  addToCart: (p: Product, quantity?: number) => void
  setQuantity: (productId: number, quantity: number) => void
  removeFromCart: (productId: number) => void
  clearCart: () => void
}

const StoreContext = createContext<Store | null>(null)

function load<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key)
    return raw ? JSON.parse(raw) : fallback
  } catch {
    return fallback
  }
}

function save(key: string, value: unknown) {
  try {
    if (value == null) localStorage.removeItem(key)
    else localStorage.setItem(key, JSON.stringify(value))
  } catch {
    // storage unavailable (private window etc.): state just won't survive a refresh
  }
}

export function StoreProvider({ children }: { children: ReactNode }) {
  const [customer, setCustomer] = useState<Customer | null>(() => load('customer', null))
  const [cart, setCart] = useState<CartLine[]>(() => load('cart', []))

  useEffect(() => { save('customer', customer) }, [customer])
  useEffect(() => { save('cart', cart) }, [cart])

  const clamp = (quantity: number, p: Product) => Math.max(1, Math.min(quantity, p.stock))

  const store: Store = {
    customer,
    signIn: setCustomer,
    signOut: () => setCustomer(null),
    cart,
    addToCart: (product, quantity = 1) => setCart(lines => {
      const existing = lines.find(l => l.product.id === product.id)
      if (!existing) return [...lines, { product, quantity: clamp(quantity, product) }]
      return lines.map(l => l === existing ? { product, quantity: clamp(l.quantity + quantity, product) } : l)
    }),
    setQuantity: (productId, quantity) =>
      setCart(lines => lines.map(l => l.product.id === productId ? { ...l, quantity: clamp(quantity, l.product) } : l)),
    removeFromCart: id => setCart(lines => lines.filter(l => l.product.id !== id)),
    clearCart: () => setCart([]),
  }

  return <StoreContext.Provider value={store}>{children}</StoreContext.Provider>
}

export function useStore() {
  const store = useContext(StoreContext)
  if (!store) throw new Error('useStore must be used inside StoreProvider')
  return store
}
