import type { OrderStatus } from '../api'

// Placed → Payment → Paid/Declined. Payment runs asynchronously over Kafka,
// so the middle step pulses while the order is still PENDING.
export default function OrderTracker({ status }: { status: OrderStatus }) {
  const settled = status !== 'PENDING'
  const last = status === 'PAID' ? 'done' : status === 'FAILED' ? 'failed' : ''
  return (
    <ol className="tracker" aria-label={`Order status: ${status.toLowerCase()}`}>
      <li className="done">Placed</li>
      <li className={settled ? (status === 'PAID' ? 'done' : 'failed') : 'active'}>Payment</li>
      <li className={last}>{status === 'FAILED' ? 'Declined' : 'Paid'}</li>
    </ol>
  )
}
