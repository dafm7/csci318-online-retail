import { useState, type FormEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { api } from '../api'
import { useStore } from '../store'

export default function LoginPage() {
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [form, setForm] = useState({ name: '', email: '', password: '', address: '' })
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const { signIn } = useStore()
  const navigate = useNavigate()
  const [params] = useSearchParams()

  const set = (k: keyof typeof form) => (e: { target: { value: string } }) => setForm(f => ({ ...f, [k]: e.target.value }))

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      if (mode === 'register') await api.register(form)
      // Log in after registering too, so both paths end the same way
      signIn(await api.login(form.email, form.password))
      navigate(params.get('next') ?? '/account')
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth">
      <div className="auth-pitch">
        <p className="kicker">{mode === 'login' ? 'Members' : 'New here'}</p>
        <h1 className="display">
          {mode === 'login' ? <>Welcome <em>back.</em></> : <>Join the <em>shop.</em></>}
        </h1>
        <p>
          Signed in, you can check out, follow each order from placed to paid, and ask the assistant
          about your own orders.
        </p>
      </div>

      <form onSubmit={submit} className="auth-form">
        <div className="seg" role="tablist">
          <button type="button" role="tab" aria-selected={mode === 'login'} className={mode === 'login' ? 'active' : ''} onClick={() => setMode('login')}>Sign in</button>
          <button type="button" role="tab" aria-selected={mode === 'register'} className={mode === 'register' ? 'active' : ''} onClick={() => setMode('register')}>Create account</button>
        </div>
        {mode === 'register' && (
          <label className="field"><span>Name</span><input required value={form.name} onChange={set('name')} autoComplete="name" /></label>
        )}
        <label className="field"><span>Email</span><input required type="email" value={form.email} onChange={set('email')} autoComplete="email" /></label>
        <label className="field"><span>Password</span><input required type="password" value={form.password} onChange={set('password')}
          autoComplete={mode === 'login' ? 'current-password' : 'new-password'} /></label>
        {mode === 'register' && (
          <label className="field"><span>Address</span><input value={form.address} onChange={set('address')} autoComplete="street-address" placeholder="Wollongong NSW" /></label>
        )}
        {error && <p className="error-text" role="alert">{error}</p>}
        <button className="btn btn-ink btn-wide" disabled={busy}>
          {busy ? 'One moment…' : <>{mode === 'login' ? 'Sign in' : 'Create account'} <span className="arrow">→</span></>}
        </button>
      </form>
    </div>
  )
}
