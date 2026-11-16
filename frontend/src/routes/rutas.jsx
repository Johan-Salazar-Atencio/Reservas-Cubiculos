import { Navigate, Outlet, Route, Routes } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import Layout from '../components/Layout.jsx'
import Login from '../pages/Login.jsx'
import Tablero from '../pages/estudiante/Tablero.jsx'
import MisReservas from '../pages/estudiante/MisReservas.jsx'
import Normas from '../pages/estudiante/Normas.jsx'
import Monitoreo from '../pages/admin/Monitoreo.jsx'
import ReservasAdmin from '../pages/admin/ReservasAdmin.jsx'
import Registro from '../pages/admin/Registro.jsx'
import Estadisticas from '../pages/admin/Estadisticas.jsx'
import Simulador from '../pages/admin/Simulador.jsx'

/** Solo deja pasar a quien tiene sesión y el rol que pide la zona; al resto lo manda a donde sí puede estar. */
function RutaProtegida({ rol }) {
  const { sesion, esAdmin } = useAuth()
  if (!sesion) return <Navigate to="/login" replace />
  if ((esAdmin ? 'ADMIN' : 'ESTUDIANTE') !== rol) return <Navigate to={esAdmin ? '/admin' : '/'} replace />
  return <Outlet />
}

export default function Rutas() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />

      <Route element={<RutaProtegida rol="ESTUDIANTE" />}>
        <Route element={<Layout />}>
          <Route index element={<Tablero />} />
          <Route path="mis-reservas" element={<MisReservas />} />
          <Route path="normas" element={<Normas />} />
        </Route>
      </Route>

      <Route element={<RutaProtegida rol="ADMIN" />}>
        <Route path="admin" element={<Layout />}>
          <Route index element={<Monitoreo />} />
          <Route path="reservas" element={<ReservasAdmin />} />
          <Route path="registro" element={<Registro />} />
          <Route path="estadisticas" element={<Estadisticas />} />
          <Route path="simulador" element={<Simulador />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
