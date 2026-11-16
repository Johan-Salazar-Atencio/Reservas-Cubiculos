import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { ApiError, alCaducarSesion, borrarSesion, guardarSesion, leerSesion } from '../services/api.js'
import { authApi } from '../services/endpoints.js'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [sesion, setSesion] = useState(() => leerSesion())
  const [aviso, setAviso] = useState('')

  const salir = useCallback((mensaje = '') => {
    borrarSesion()
    setSesion(null)
    setAviso(typeof mensaje === 'string' ? mensaje : '')
  }, [])

  const entrar = useCallback((respuesta) => {
    guardarSesion(respuesta)
    setSesion(respuesta)
    setAviso('')
  }, [])

  useEffect(() => {
    alCaducarSesion(() => salir('Tu sesión terminó. Vuelve a entrar.'))
  }, [salir])

  // Al abrir la página se comprueba que la sesión guardada todavía sirva (el servidor pudo reiniciarse).
  useEffect(() => {
    if (!leerSesion()) return
    authApi.perfil().catch((e) => {
      if (e instanceof ApiError && (e.status === 401 || e.status === 404)) {
        salir('Tu sesión terminó. Vuelve a entrar.')
      }
    })
  }, [salir])

  const valor = useMemo(
    () => ({
      sesion,
      usuario: sesion?.usuario ?? null,
      esAdmin: sesion?.usuario?.rol === 'ADMIN',
      aviso,
      entrar,
      salir,
    }),
    [sesion, aviso, entrar, salir],
  )

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const contexto = useContext(AuthContext)
  if (!contexto) throw new Error('useAuth debe usarse dentro de AuthProvider')
  return contexto
}
