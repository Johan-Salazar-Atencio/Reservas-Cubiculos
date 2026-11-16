import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import AgendaGrid from './AgendaGrid.jsx'

const agenda = {
  horas: ['10:00', '11:00', '12:00'],
  cubiculos: [
    {
      id: 1,
      numero: 1,
      nombre: 'Cubículo 1',
      slots: [
        { hora: '10:00', estado: 'PASADO', mia: false },
        { hora: '11:00', estado: 'LIBRE', mia: false },
        { hora: '12:00', estado: 'RESERVADO', mia: true },
      ],
    },
  ],
}

describe('AgendaGrid', () => {
  it('solo deja elegir los turnos libres', async () => {
    const onElegir = vi.fn()
    render(<AgendaGrid agenda={agenda} onElegir={onElegir} />)

    await userEvent.click(screen.getByRole('button', { name: 'Cubículo 1, 11:00, libre' }))
    expect(onElegir).toHaveBeenCalledWith(agenda.cubiculos[0], '11:00')

    expect(screen.getByRole('button', { name: 'Cubículo 1, 10:00, ya pasó' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Cubículo 1, 12:00, reservado, es tuya' })).toBeDisabled()
  })

  it('marca con "Tuya" el turno del propio estudiante', () => {
    render(<AgendaGrid agenda={agenda} onElegir={() => {}} />)
    expect(screen.getByText('Tuya')).toBeInTheDocument()
  })

  it('no deja elegir nada cuando el estudiante no puede reservar (cuota agotada, pausa o cerrado)', async () => {
    const onElegir = vi.fn()
    render(<AgendaGrid agenda={agenda} onElegir={onElegir} puedeReservar={false} />)
    const libre = screen.getByRole('button', { name: 'Cubículo 1, 11:00, libre' })
    expect(libre).toBeDisabled()
    await userEvent.click(libre)
    expect(onElegir).not.toHaveBeenCalled()
  })
})
