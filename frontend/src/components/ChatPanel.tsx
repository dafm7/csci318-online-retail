import { useEffect, useRef, useState, type FormEvent } from 'react'
import { api } from '../api'
import { useStore } from '../store'

interface Message { from: 'me' | 'bot'; text: string }

const GREETING: Message = { from: 'bot', text: "Hello! I can find products, check what's in stock, and, once you're signed in, tell you where your orders are." }

// Floating chat with ai-agent-service. The sessionId from the first reply keeps the conversation going;
// customerId is sent on every message so signing in/out mid-chat takes effect straight away.
export default function ChatPanel() {
  const { customer } = useStore()
  const [open, setOpen] = useState(false)
  const [messages, setMessages] = useState<Message[]>([GREETING])
  const [input, setInput] = useState('')
  const [sessionId, setSessionId] = useState<string>()
  const [busy, setBusy] = useState(false)
  const endRef = useRef<HTMLDivElement>(null)

  useEffect(() => { endRef.current?.scrollIntoView({ behavior: 'smooth' }) }, [messages, busy, open])

  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') setOpen(false) }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open])

  const ask = async (text: string) => {
    text = text.trim()
    if (!text || busy) return
    setInput('')
    setMessages(m => [...m, { from: 'me', text }])
    setBusy(true)
    try {
      const res = await api.chat(text, sessionId, customer?.id)
      setSessionId(res.sessionId)
      setMessages(m => [...m, { from: 'bot', text: res.reply }])
    } catch (err) {
      setMessages(m => [...m, { from: 'bot', text: `Sorry, I couldn't reach the assistant (${err instanceof Error ? err.message : err}).` }])
    } finally {
      setBusy(false)
    }
  }

  const send = (e: FormEvent) => { e.preventDefault(); ask(input) }

  const newChat = () => {
    if (sessionId) api.endChat(sessionId).catch(() => {})
    setSessionId(undefined)
    setMessages([GREETING])
  }

  const suggestions = ['Do you sell keyboards?', 'What is running low on stock?',
    ...(customer ? ['Where is my latest order?'] : [])]

  if (!open) {
    return (
      <button className="concierge-fab" onClick={() => setOpen(true)}>
        <span className="live-dot" aria-hidden />Ask the assistant
      </button>
    )
  }

  return (
    <aside className="concierge" aria-label="Shop assistant" role="dialog">
      <header className="concierge-head">
        <div>
          <h2>Concierge</h2>
          <p className="kicker">{customer ? `Signed in as ${customer.name}` : 'Sign in to ask about orders'}</p>
        </div>
        <div style={{ display: 'flex', gap: 6 }}>
          <button className="icon-btn" onClick={newChat} title="New conversation" aria-label="New conversation">↺</button>
          <button className="icon-btn" onClick={() => setOpen(false)} title="Close" aria-label="Close">✕</button>
        </div>
      </header>
      <div className="concierge-log" aria-live="polite">
        {messages.map((m, i) => (
          <div key={i} className={`msg ${m.from}`}>
            {m.from === 'bot' && <span className="msg-who">Assistant</span>}
            {m.text}
          </div>
        ))}
        {messages.length === 1 && !busy && (
          <div className="suggestions">
            {suggestions.map(s => <button key={s} onClick={() => ask(s)}>{s}</button>)}
          </div>
        )}
        {busy && <div className="msg bot"><span className="msg-who">Assistant</span><span className="typing"><span /><span /><span /></span></div>}
        <div ref={endRef} />
      </div>
      <form onSubmit={send} className="concierge-input">
        <input value={input} onChange={e => setInput(e.target.value)} placeholder="Ask anything about the shop…" autoFocus aria-label="Message" />
        <button className="send" disabled={busy || !input.trim()} aria-label="Send">↑</button>
      </form>
    </aside>
  )
}
