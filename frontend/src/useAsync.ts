import { useCallback, useEffect, useRef, useState } from 'react'

// Loads data from a promise-returning function, re-running when deps change.
// With refreshMs it also polls on that interval, quietly (no loading flash).
export function useAsync<T>(fn: () => Promise<T>, deps: unknown[], refreshMs?: number) {
  const [data, setData] = useState<T | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const fnRef = useRef(fn)
  fnRef.current = fn

  const run = useCallback(async (quiet: boolean) => {
    if (!quiet) setLoading(true)
    try {
      setData(await fnRef.current())
      setError(null)
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    run(false)
    if (!refreshMs) return
    const t = setInterval(() => run(true), refreshMs)
    return () => clearInterval(t)
  }, [run, refreshMs, ...deps])

  return { data, error, loading, reload: () => run(true) }
}
