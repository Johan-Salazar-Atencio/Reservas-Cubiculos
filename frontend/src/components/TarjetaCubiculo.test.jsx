import { render, screen } from '@testing-library/react'
import TarjetaCubiculo from './TarjetaCubiculo.jsx'

const base = {
  id: 1,
  numero: 2,
  nombre: 'Cubículo 2',
  luz: 'APAGADA',
  ocupadoHasta: null,
  libreDesde: null,
  proximaReserva: null,
  llegarAntesDe: null,
  segundosParaLimite: null,
  minutosRestantes: null,
  tipoReserva: null,
  personas: null,
  motivo: null,
  miReservaId: null,
  sensor: null,
}

describe('TarjetaCubiculo', () => {
  it('muestra un cubículo libre, hasta qué hora y con la luz apagada', () => {
    render(<TarjetaCubiculo c={{ ...base, estado: 'LIBRE', proximaReserva: '14:00' }} />)
    expect(screen.getByRole('article', { name: 'Cubículo 2: Libre' })).toBeInTheDocument()
    expect(screen.getByText('Libre hasta las 14:00')).toBeInTheDocument()
    expect(screen.getByText(/Luz de la maqueta: apagada/)).toBeInTheDocument()
  })

  it('si no hay más reservas, dice que está libre hasta el cierre', () => {
    render(<TarjetaCubiculo c={{ ...base, estado: 'LIBRE' }} cierre="19:00" />)
    expect(screen.getByText('Libre hasta el cierre (19:00)')).toBeInTheDocument()
  })

  it('muestra hasta qué hora está ocupado, cuándo queda libre y la luz azul', () => {
    render(<TarjetaCubiculo c={{ ...base, estado: 'OCUPADO', ocupadoHasta: '11:00', libreDesde: '12:00' }} />)
    expect(screen.getByText('Ocupado hasta las 11:00')).toBeInTheDocument()
    expect(screen.getByText('Libre desde las 12:00')).toBeInTheDocument()
    expect(screen.getByText(/Luz de la maqueta: azul/)).toBeInTheDocument()
  })

  it('cuando quedan pocos minutos lo dice y la luz es roja', () => {
    render(
      <TarjetaCubiculo c={{ ...base, estado: 'POR_TERMINAR', ocupadoHasta: '11:00', minutosRestantes: 4 }} />,
    )
    expect(screen.getByText('Quedan 4 min')).toBeInTheDocument()
    expect(screen.getByText(/Luz de la maqueta: roja/)).toBeInTheDocument()
  })

  it('si está reservado y aún no llega nadie, cuenta el tiempo que falta para confirmar la llegada', () => {
    render(
      <TarjetaCubiculo
        c={{ ...base, estado: 'RESERVADO', ocupadoHasta: '12:00', llegarAntesDe: '11:10', segundosParaLimite: 600 }}
        recibidoEn={Date.now()}
      />,
    )
    expect(screen.getByText('Se libera si nadie llega antes de las 11:10')).toBeInTheDocument()
    expect(screen.getByText(/Faltan 10:00 para confirmar la llegada/)).toBeInTheDocument()
    expect(screen.getByText(/Luz de la maqueta: ámbar/)).toBeInTheDocument()
  })

  it('un cubículo en mantenimiento muestra el motivo y la luz violeta', () => {
    render(<TarjetaCubiculo c={{ ...base, estado: 'MANTENIMIENTO', motivo: 'Cambio de la tira LED' }} />)
    expect(screen.getByText('Fuera de servicio')).toBeInTheDocument()
    expect(screen.getByText('Cambio de la tira LED')).toBeInTheDocument()
    expect(screen.getByText(/Luz de la maqueta: violeta/)).toBeInTheDocument()
  })

  it('marca la reserva del propio estudiante y el relevo', () => {
    render(
      <TarjetaCubiculo
        c={{ ...base, estado: 'OCUPADO', ocupadoHasta: '13:00', tipoReserva: 'GRUPO', personas: 2, miReservaId: 9 }}
      />,
    )
    expect(screen.getByText('Tu reserva')).toBeInTheDocument()
    expect(screen.getByText(/Relevo de 2 personas/)).toBeInTheDocument()
  })
})
