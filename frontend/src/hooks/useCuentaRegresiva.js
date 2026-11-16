import { useEffect, useState } from 'react'

/**
 * Cuenta regresiva en segundos entre una consulta y otra. El servidor manda cuántos segundos faltan
 * ({segundos}) y en qué momento llegó la respuesta ({recibidoEn}); aquí solo se sigue contando.
 * Devuelve null si no hay nada que contar.
 */
export function useCuentaRegresiva(segundos, recibidoEn) {
  const [ahora, setAhora] = useState(() => Date.now())

  useEffect(() => {
    if (segundos == null) return undefined
    const id = setInterval(() => setAhora(Date.now()), 1000)
    return () => clearInterval(id)
  }, [segundos])

  if (segundos == null) return null
  return Math.max(0, Math.round(segundos - (ahora - recibidoEn) / 1000))
}
