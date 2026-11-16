// Cliente HTTP único. Guarda la sesión (token + usuario) y manda el token en cada petición.
// En desarrollo, Vite reenvía /api a Spring (puerto 8095); ver vite.config.js.

const CLAVE_SESION = 'reservas.sesion'
let sesionEnMemoria = null

export function leerSesion() {
  if (sesionEnMemoria) return sesionEnMemoria
  try {
    return JSON.parse(localStorage.getItem(CLAVE_SESION))
  } catch {
    return null
  }
}

export function guardarSesion(sesion) {
  sesionEnMemoria = sesion
  try {
    localStorage.setItem(CLAVE_SESION, JSON.stringify(sesion))
  } catch {
    /* sin almacenamiento: la sesión dura hasta recargar la página */
  }
}

export function borrarSesion() {
  sesionEnMemoria = null
  try {
    localStorage.removeItem(CLAVE_SESION)
  } catch {
    /* nada que borrar */
  }
}

export class ApiError extends Error {
  constructor(status, codigo, mensaje) {
    super(mensaje)
    this.name = 'ApiError'
    this.status = status
    this.codigo = codigo
  }
}

let alCaducar = null

/** Registra qué hacer cuando el servidor dice que la sesión ya no sirve (401). */
export function alCaducarSesion(funcion) {
  alCaducar = funcion
}

export async function api(ruta, { metodo = 'GET', cuerpo } = {}) {
  const sesion = leerSesion()
  const cabeceras = { Accept: 'application/json' }
  if (cuerpo !== undefined) cabeceras['Content-Type'] = 'application/json'
  if (sesion?.token) cabeceras.Authorization = `Bearer ${sesion.token}`

  let respuesta
  try {
    respuesta = await fetch(`/api${ruta}`, {
      method: metodo,
      headers: cabeceras,
      body: cuerpo !== undefined ? JSON.stringify(cuerpo) : undefined,
    })
  } catch {
    throw new ApiError(0, 'SIN_CONEXION', 'No se pudo conectar con el servidor.')
  }

  const texto = await respuesta.text()
  let datos = null
  if (texto) {
    try {
      datos = JSON.parse(texto)
    } catch {
      datos = null
    }
  }

  if (!respuesta.ok) {
    if (respuesta.status === 401 && sesion) alCaducar?.()
    throw new ApiError(
      respuesta.status,
      datos?.codigo ?? 'ERROR',
      datos?.mensaje ?? 'Algo salió mal. Inténtalo de nuevo.',
    )
  }
  return datos
}
