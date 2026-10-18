import type { OrderStatus } from '../api'

const LABEL: Record<OrderStatus, string> = {
  PENDING: 'Pending', PAID: 'Paid', FAILED: 'Declined', CANCELLED: 'Cancelled',
  SHIPPED: 'Shipped', DELIVERED: 'Delivered', REFUNDED: 'Refunded',
}

export default function StatusBadge({ status }: { status: OrderStatus }) {
  return <span className={`pill pill-${status.toLowerCase()}`}>{(LABEL[status] ?? status).toUpperCase()}</span>
}
