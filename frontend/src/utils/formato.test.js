import { fechaCorta, fechaLarga, horaDe, iniciales, lineasDeEstado, minSeg, sumarHoras, textoDeBanner } from './formato.js'

describe('formato', () => {
  it('saca la hora de una fecha ISO sin zona', () => {
    expect(horaDe('2026-11-08T10:05:12')).toBe('10:05')
    expect(horaDe(null)).toBe('')
  })

  it('escribe las fechas en español sin depender de la zona del navegador', () => {
    expect(fechaLarga('2026-11-08')).toBe('domingo 8 de noviembre')
    expect(fechaLarga('2026-11-09T07:00:00')).toBe('lunes 9 de noviembre')
    expect(fechaCorta('2026-11-08')).toBe('08/11')
  })

  it('arma las iniciales y los textos de tiempo', () => {
    expect(iniciales('Ana Demo')).toBe('AD')
    expect(iniciales('Administración')).toBe('A')
    expect(minSeg(612)).toBe('10:12')
    expect(sumarHoras('11:00', 2)).toBe('13:00')
  })

  it('explica cada estado del cubículo', () => {
    expect(lineasDeEstado({ estado: 'LIBRE', proximaReserva: '11:00' }, '19:00').principal).toBe('Libre hasta las 11:00')
    expect(lineasDeEstado({ estado: 'OCUPADO', ocupadoHasta: '11:00', libreDesde: null }, '19:00').secundaria).toBe(
      'No quedan más turnos hoy',
    )
    expect(textoDeBanner({ estado: 'POR_TERMINAR', minutosRestantes: 5 })).toBe('Quedan 5 min')
    expect(textoDeBanner({ estado: 'MANTENIMIENTO' })).toBe('En mantenimiento')
  })
})
