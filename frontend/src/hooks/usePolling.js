import { useCallback, useEffect, useRef, useState } from 'react'

/**
 * Consulta al servidor cada {intervaloMs} milisegundos (el "en vivo" de la página).
 * Conserva los últimos datos si una consulta falla, y se pausa cuando la pestaña no está a la vista.
 * Si cambian las {dependencias} (por ejemplo, un filtro), vuelve a consultar de inmediato.
 */
export function usePolling(cargar, intervaloMs = 3000, dependencias = []) {
  const [datos, setDatos] = useState(null)
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [recibidoEn, setRecibidoEn] = useState(0)
  const cargarRef = useRef(cargar)

  useEffect(() => {
    cargarRef.current = cargar
  })

  const refrescar = useCallback(async () => {
    try {
      const nuevos = await cargarRef.current()
      setDatos(nuevos)
      setError(null)
      setRecibidoEn(Date.now())
    } catch (e) {
      setError(e)
    } finally {
      setCargando(false)
    }
  }, [])

  useEffect(() => {
    refrescar()
    const id = setInterval(() => {
      if (!document.hidden) refrescar()
    }, intervaloMs)
    return () => clearInterval(id)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [intervaloMs, refrescar, ...dependencias])

  return { datos, error, cargando, recibidoEn, refrescar }
}
