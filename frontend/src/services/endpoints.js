// Todas las llamadas a la API, agrupadas por zona. Las pantallas nunca usan fetch directamente.
import { api } from './api.js'

export const authApi = {
  login: (cuerpo) => api('/auth/login', { metodo: 'POST', cuerpo }),
  perfil: () => api('/auth/me'),
  // Solo existen en el perfil dev del servidor
  cuentasDemo: () => api('/auth/demo'),
  entrarDemo: (codigo) => api(`/auth/demo/${encodeURIComponent(codigo)}`, { metodo: 'POST' }),
}

export const relojApi = {
  actual: () => api('/reloj'),
}

export const cubiculosApi = {
  enVivo: () => api('/cubiculos'),
  agenda: () => api('/cubiculos/agenda'),
}

export const reservasApi = {
  crear: (cuerpo) => api('/reservas', { metodo: 'POST', cuerpo }),
  mias: () => api('/reservas/mias'),
  cancelar: (id) => api(`/reservas/${id}/cancelar`, { metodo: 'POST' }),
  terminar: (id) => api(`/reservas/${id}/terminar`, { metodo: 'POST' }),
}

export const adminApi = {
  panel: () => api('/admin/panel'),
  liberar: (id, motivo) => api(`/admin/cubiculos/${id}/liberar`, { metodo: 'POST', cuerpo: { motivo } }),
  ocupar: (id, horas, motivo) =>
    api(`/admin/cubiculos/${id}/ocupar`, { metodo: 'POST', cuerpo: { horas, motivo } }),
  mantenimiento: (id, activo, motivo) =>
    api(`/admin/cubiculos/${id}/mantenimiento`, { metodo: 'POST', cuerpo: { activo, motivo } }),
  reservas: (filtros = {}) => {
    const consulta = new URLSearchParams()
    for (const [clave, valor] of Object.entries(filtros)) {
      if (valor !== '' && valor != null) consulta.set(clave, valor)
    }
    const texto = consulta.toString()
    return api(`/admin/reservas${texto ? `?${texto}` : ''}`)
  },
  registro: (limite = 50) => api(`/admin/registro?limite=${limite}`),
  estadisticas: (dias = 7) => api(`/admin/estadisticas?dias=${dias}`),
}

// Solo en el perfil dev: reloj y sensores virtuales
export const simuladorApi = {
  reloj: () => api('/dev/reloj'),
  avanzar: (minutos) => api('/dev/reloj/avanzar', { metodo: 'POST', cuerpo: { minutos } }),
  irA: (hora) => api('/dev/reloj/ir', { metodo: 'POST', cuerpo: { hora } }),
  restablecer: () => api('/dev/reloj/restablecer', { metodo: 'POST' }),
  presencia: (numero) => api(`/dev/sensores/${numero}/presencia`, { metodo: 'POST' }),
  latido: (numero) => api(`/dev/sensores/${numero}/latido`, { metodo: 'POST' }),
  sinSenal: (numero, activo) =>
    api(`/dev/sensores/${numero}/sin-senal`, { metodo: 'POST', cuerpo: { activo } }),
}
