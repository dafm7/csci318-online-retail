import type { Order, Payment, Product } from './api'

// "2 × Wireless Mouse, 1 × Mechanical Keyboard"
export function describeItems(order: Order, products: Product[] | null): string {
  const name = (id: number) => products?.find(p => p.id === id)?.name ?? `Product #${id}`
  const items = order.items?.length ? order.items : [{ productId: order.productId, quantity: order.quantity, unitPrice: 0 }]
  return items.map(i => `${i.quantity} × ${name(i.productId)}`).join(', ')
}

// Same rules as the CustomerOrder and Payment aggregates, so only valid buttons are shown
export const canPay = (o: Order) => o.status === 'PENDING'
export const canCancel = (o: Order) => o.status === 'PENDING' || o.status === 'PAID' || o.status === 'FAILED'
export const canRefund = (o: Order, payments: Payment[] | null) =>
  (o.status === 'PAID' || o.status === 'CANCELLED') && !o.refundedAt
  && !!payments?.some(p => p.orderId === o.id && p.status === 'SUCCESS')
