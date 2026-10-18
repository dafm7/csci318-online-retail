import { api, LOW_STOCK, type StockLevel } from '../api'
import { useAsync } from '../useAsync'

// Scrolling strip of live stock levels from product-service's Kafka Streams inventory view
export default function Ticker() {
  const inventory = useAsync(api.inventory, [], 5000)
  const levels = inventory.data ?? []

  return (
    <div className="ticker" aria-label="Live stock levels">
      <div className="ticker-label"><span className="live-dot" />Live stock</div>
      {levels.length === 0
        ? <span className="ticker-static">{inventory.error ? 'Live inventory offline' : 'Connecting to inventory stream…'}</span>
        : (
          <div className="ticker-track">
            <div className="ticker-run" style={{ animationDuration: `${Math.max(20, levels.length * 7)}s` }}>
              {/* two identical groups so translateX(-50%) loops seamlessly */}
              <Group levels={levels} />
              <Group levels={levels} hidden />
            </div>
          </div>
        )}
    </div>
  )
}

function Group({ levels, hidden }: { levels: StockLevel[]; hidden?: boolean }) {
  return (
    <div className="ticker-group" aria-hidden={hidden}>
      {levels.map(s => (
        <span key={s.productId} className={s.stock < LOW_STOCK ? 'tick low' : 'tick'}>
          {s.name} <b>{s.stock < LOW_STOCK ? `· only ${s.stock} left` : `· ${s.stock}`}</b>
        </span>
      ))}
    </div>
  )
}
