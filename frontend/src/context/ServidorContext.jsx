import { createContext, useContext, useEffect } from 'react'

/** Lo que cada pantalla sabe del servidor (hora, si es simulada, si hay conexión) y la barra superior muestra. */
export const ServidorContext = createContext({
  servidor: { ahora: null, simulado: false, conectado: true },
  publicar: () => {},
})

export function useServidor() {
  return useContext(ServidorContext)
}

/** Cada pantalla llama a esto con sus últimos datos para que la barra superior muestre la hora del servidor. */
export function useReportarServidor(fuente, error) {
  const { publicar } = useServidor()
  const ahora = fuente?.ahora
  const simulado = fuente?.simulado
  const conectado = !error
  useEffect(() => {
    publicar({ ahora, simulado, conectado })
  }, [ahora, simulado, conectado, publicar])
}
