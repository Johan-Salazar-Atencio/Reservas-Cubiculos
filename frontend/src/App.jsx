import { AuthProvider } from './context/AuthContext.jsx'
import { ToastProvider } from './components/Toast.jsx'
import Rutas from './routes/rutas.jsx'

export default function App() {
  return (
    <AuthProvider>
      <ToastProvider>
        <Rutas />
      </ToastProvider>
    </AuthProvider>
  )
}
