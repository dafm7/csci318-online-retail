// Typed client for the retail microservices. Paths are proxied to each service by vite.config.ts.

export type Category = 'CLOTHES' | 'ELECTRONICS' | 'SPORTS' | 'HOME'
export const CATEGORIES: Category[] = ['CLOTHES', 'ELECTRONICS', 'SPORTS', 'HOME']
export type Availability = 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK'
export interface Product {
  id: number; name: string; category: Category; price: number; stock: number
  lowStockThreshold: number; availability: Availability
}
export type Role = 'CUSTOMER' | 'ADMIN'
export interface Customer { id: number; name: string; email: string; role: Role }
export type OrderStatus = 'PENDING' | 'PAID' | 'FAILED'
export interface Order { id: number; customerId: number; productId: number; quantity: number; amount: number; status: OrderStatus }
export interface RecentOrder { orderId: number; productId: number; quantity: number; amount: number; status: OrderStatus }
export interface CustomerProfile {
  id: number; name: string; email: string; role: Role
  // null when order-service is unavailable
  orders: { totalOrders: number; paidOrders: number; totalSpent: number; recentOrders: RecentOrder[] } | null
}
// lowStockThreshold is null for stock events published before per-product thresholds existed
export interface StockLevel { productId: number; name: string; stock: number; lowStockThreshold: number | null }
export interface ProductSales { productId: number; name: string; unitsSold: number; revenue: number; orders: number }
export interface MinuteSales { productId: number; name: string; windowStart: string; windowEnd: string; unitsSold: number }
export interface Payment { id: number; orderId: number; amount: number; success: boolean }
export interface ChatResponse { sessionId: string; reply: string }

export class ApiError extends Error {
  constructor(public status: number, message: string) { super(message) }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let res: Response
  try {
    res = await fetch(path, {
      ...init,
      headers: { ...(init?.body ? { 'Content-Type': 'application/json' } : {}), ...(init?.headers as Record<string, string>) },
    })
  } catch {
    throw new ApiError(0, 'Could not reach the server')
  }
  if (!res.ok) {
    // Spring's error body is {"status", "error", "message", ...}; message is empty unless
    // server.error.include-message is on, so fall back to a readable default per status
    const body = await res.json().catch(() => null)
    throw new ApiError(res.status, body?.message || defaultMessage(res.status, body?.error))
  }
  if (res.status === 204) return undefined as T
  return res.json()
}

function defaultMessage(status: number, error?: string): string {
  if (status === 401) return 'Invalid email or password'
  if (status === 403) return 'Only admins can do this'
  if (status === 409) return 'That email is already registered'
  if (status === 404) return 'Not found'
  if (status >= 500) return 'The service is unavailable. Is it running?'
  return error || `Request failed (${status})`
}

const post = <T,>(path: string, body: unknown, headers?: Record<string, string>) =>
  request<T>(path, { method: 'POST', body: JSON.stringify(body), headers })

// Admin-only endpoints identify the caller by the X-User-Id header (product-service checks the role with user-service)
const asUser = (userId: number) => ({ 'X-User-Id': String(userId) })

export const api = {
  products: (search?: string, category?: string) => {
    const q = new URLSearchParams()
    if (search) q.set('search', search)
    if (category) q.set('category', category)
    const qs = q.toString()
    return request<Product[]>(`/api/products${qs ? `?${qs}` : ''}`)
  },
  addProduct: (adminId: number, p: { name: string; category: Category; price: number; stock: number; lowStockThreshold?: number }) =>
    post<Product>('/api/products', p, asUser(adminId)),
  restock: (adminId: number, productId: number, quantity: number) =>
    post<Product>(`/api/products/${productId}/restock`, { quantity }, asUser(adminId)),

  register: (c: { name: string; email: string; password: string }) => post<Customer>('/api/users/register', c),
  login: (email: string, password: string) => post<Customer>('/api/users/login', { email, password }),
  profile: (id: number) => request<CustomerProfile>(`/api/users/${id}`),

  placeOrder: (o: { customerId: number; productId: number; quantity: number; amount: number }) => post<Order>('/api/orders', o),
  order: (id: number) => request<Order>(`/api/orders/${id}`),
  orders: (customerId?: number) => request<Order[]>(`/api/orders${customerId ? `?customerId=${customerId}` : ''}`),

  inventory: () => request<StockLevel[]>('/api/inventory'),
  lowStock: () => request<StockLevel[]>('/api/inventory/low-stock'),
  sales: () => request<ProductSales[]>('/api/sales'),
  salesPerMinute: (minutes = 5) => request<MinuteSales[]>(`/api/sales/per-minute?minutes=${minutes}`),
  payments: () => request<Payment[]>('/api/payments'),

  chat: (message: string, sessionId?: string, customerId?: number) => post<ChatResponse>('/api/chat', { message, sessionId, customerId }),
  endChat: (sessionId: string) => request<void>(`/api/chat/${sessionId}`, { method: 'DELETE' }),
}

export const money = (n: number) => n.toLocaleString('en-AU', { style: 'currency', currency: 'AUD' })

// Same rule as payment-service's Payment.LIMIT: orders above this are declined
export const PAYMENT_LIMIT = 1000

// Same rule as product-service's LowStockPolicy: low when below the product's own threshold (default 10)
export const DEFAULT_LOW_STOCK = 10
export const isLowStock = (s: { stock: number; lowStockThreshold?: number | null }) =>
  s.stock < (s.lowStockThreshold ?? DEFAULT_LOW_STOCK)

// ELECTRONICS -> Electronics
export const categoryLabel = (c: string) => c.charAt(0) + c.slice(1).toLowerCase()
