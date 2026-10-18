import type { OrderStatus } from '../api'

const LABEL: Record<OrderStatus, string> = { PENDING: 'Pending', PAID: 'Paid', FAILED: 'Declined' }

export default function StatusBadge({ status }: { status: OrderStatus }) {
  return <span className={`pill pill-${status.toLowerCase()}`}>{LABEL[status].toUpperCase()}</span>
}
