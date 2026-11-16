import { useCallback, useEffect, useMemo, useState } from 'react'
import { Outlet } from 'react-router-dom'
import GlobalNav from './GlobalNav.jsx'
import TopBar from './TopBar.jsx'
import TrayCuenta from './TrayCuenta.jsx'
import { ServidorContext } from '../context/ServidorContext.jsx'
import { usePolling } from '../hooks/usePolling.js'
import { relojApi } from '../services/endpoints.js'

/** Estructura de todas las pantallas: barra lateral, barra superior, contenido y pie. */
export default function Layout() {
  const [cuentaAbierta, setCuentaAbierta] = useState(false)
  const [servidor, setServidor] = useState({ ahora: null, simulado: false, conectado: true })

  const publicar = useCallback((parcial) => {
    setServidor((previo) => {
      const siguiente = { ...previo }
      for (const [clave, valor] of Object.entries(parcial)) {
        if (valor !== undefined) siguiente[clave] = valor
      }
      const igual = Object.keys(siguiente).every((clave) => siguiente[clave] === previo[clave])
      return igual ? previo : siguiente
    })
  }, [])

  // La hora del servidor se pregunta aquí, así se ve en todas las pantallas (también si la hora es simulada).
  const { datos: reloj, error } = usePolling(relojApi.actual, 5000)
  useEffect(() => {
    publicar({ ahora: reloj?.ahora, simulado: reloj?.simulado, conectado: !error })
  }, [reloj, error, publicar])

  const valor = useMemo(() => ({ servidor, publicar }), [servidor, publicar])
  const cerrarCuenta = useCallback(() => setCuentaAbierta(false), [])

  return (
    <ServidorContext.Provider value={valor}>
      <GlobalNav cuentaAbierta={cuentaAbierta} onCuenta={() => setCuentaAbierta((abierta) => !abierta)} />
      {cuentaAbierta && <TrayCuenta onCerrar={cerrarCuenta} />}
      <div className="contenido-app">
        <TopBar />
        <main className="contenido">
          <Outlet />
        </main>
        <footer className="pie">
          <span>Reservas de cubículos · Tecsup</span>
          <span>Atención de 07:00 a 19:00 (hora de Lima)</span>
          <span>Prototipo del proyecto de Investigación e Innovación Tecnológica</span>
        </footer>
      </div>
    </ServidorContext.Provider>
  )
}
