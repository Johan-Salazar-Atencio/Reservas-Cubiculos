// Textos y formatos que se repiten en varias pantallas.

export const ESTADOS = {
  LIBRE: { texto: 'Libre', clase: 'libre', luz: 'apagada', claseLuz: 'luz-apagada' },
  RESERVADO: { texto: 'Reservado · aún no llega', corto: 'Reservado', clase: 'reservado', luz: 'ámbar', claseLuz: 'luz-ambar' },
  OCUPADO: { texto: 'Ocupado', clase: 'ocupado', luz: 'azul', claseLuz: 'luz-azul' },
  POR_TERMINAR: { texto: 'Por terminar', clase: 'por-terminar', luz: 'roja', claseLuz: 'luz-roja' },
  OCUPADO_ADMIN: { texto: 'Ocupado por administración', clase: 'admin', luz: 'azul', claseLuz: 'luz-azul' },
  MANTENIMIENTO: { texto: 'En mantenimiento', clase: 'mantenimiento', luz: 'violeta', claseLuz: 'luz-violeta' },
  CERRADO: { texto: 'Cerrado', clase: 'cerrado', luz: 'apagada', claseLuz: 'luz-apagada' },
}

export const ESTADOS_RESERVA = {
  RESERVADA: { texto: 'Reservada', clase: 'reservado' },
  EN_CURSO: { texto: 'En curso', clase: 'ocupado' },
  COMPLETADA: { texto: 'Completada', clase: 'libre' },
  AUSENTE: { texto: 'Ausente', clase: 'por-terminar' },
  CANCELADA: { texto: 'Cancelada', clase: 'cerrado' },
  CANCELADA_TARDE: { texto: 'Cancelada tarde', clase: 'cerrado' },
  ANULADA_ADMIN: { texto: 'Anulada por administración', clase: 'admin' },
}

export const ACCIONES = {
  LIBERAR: 'Liberó un cubículo',
  OCUPAR: 'Ocupó un cubículo',
  MANTENIMIENTO_ON: 'Inició un mantenimiento',
  MANTENIMIENTO_OFF: 'Terminó un mantenimiento',
  LIBERACION_AUTOMATICA: 'Liberación automática',
  PAUSA_AUTOMATICA: 'Pausa automática',
}

const DIAS = ['domingo', 'lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado']
const MESES = [
  'enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio',
  'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre',
]

/** "2026-11-08T10:05:00" -> "10:05" */
export function horaDe(iso) {
  return iso ? iso.slice(11, 16) : ''
}

function aFecha(texto) {
  const [anio, mes, dia] = texto.slice(0, 10).split('-').map(Number)
  return new Date(anio, mes - 1, dia)
}

/** "2026-11-08" -> "domingo 8 de noviembre" */
export function fechaLarga(texto) {
  if (!texto) return ''
  const f = aFecha(texto)
  return `${DIAS[f.getDay()]} ${f.getDate()} de ${MESES[f.getMonth()]}`
}

/** "2026-11-08" -> "08/11" */
export function fechaCorta(texto) {
  if (!texto) return ''
  const f = aFecha(texto)
  return `${String(f.getDate()).padStart(2, '0')}/${String(f.getMonth() + 1).padStart(2, '0')}`
}

export function mayuscula(texto) {
  return texto ? texto[0].toUpperCase() + texto.slice(1) : texto
}

export function iniciales(nombre) {
  if (!nombre) return '?'
  return nombre
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0].toUpperCase())
    .join('')
}

export function minSeg(totalSegundos) {
  const m = Math.floor(totalSegundos / 60)
  const s = totalSegundos % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

export function horasTexto(n) {
  return n === 1 ? '1 hora' : `${n} horas`
}

/** "11:00" + 2 -> "13:00" */
export function sumarHoras(hora, n) {
  const h = Number(hora.slice(0, 2)) + n
  return `${String(h).padStart(2, '0')}:${hora.slice(3, 5)}`
}

/** Las dos líneas de texto de una tarjeta de cubículo según su estado. */
export function lineasDeEstado(c, cierre) {
  switch (c.estado) {
    case 'LIBRE':
      return {
        principal: c.proximaReserva ? `Libre hasta las ${c.proximaReserva}` : `Libre hasta el cierre (${cierre})`,
        secundaria: '',
      }
    case 'RESERVADO':
      return {
        principal: `Reservado hasta las ${c.ocupadoHasta}`,
        secundaria: `Se libera si nadie llega antes de las ${c.llegarAntesDe}`,
      }
    case 'OCUPADO':
    case 'POR_TERMINAR':
    case 'OCUPADO_ADMIN':
      return {
        principal: `Ocupado hasta las ${c.ocupadoHasta}`,
        secundaria: c.libreDesde ? `Libre desde las ${c.libreDesde}` : 'No quedan más turnos hoy',
      }
    case 'MANTENIMIENTO':
      return { principal: 'Fuera de servicio', secundaria: c.motivo ?? '' }
    default:
      return { principal: 'Cerrado por ahora', secundaria: '' }
  }
}

/** Lo que dice la franja de color de la tarjeta. */
export function textoDeBanner(c) {
  if (c.estado === 'POR_TERMINAR') return `Quedan ${c.minutosRestantes} min`
  return ESTADOS[c.estado]?.texto ?? c.estado
}
