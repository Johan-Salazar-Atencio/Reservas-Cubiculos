import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import ModalReserva from './ModalReserva.jsx'
import { reservasApi } from '../services/endpoints.js'

vi.mock('../services/endpoints.js', () => ({
  reservasApi: { crear: vi.fn() },
}))

const cubiculo = {
  id: 7,
  numero: 1,
  nombre: 'Cubículo 1',
  slots: [
    { hora: '10:00', estado: 'LIBRE' },
    { hora: '11:00', estado: 'LIBRE' },
    { hora: '12:00', estado: 'LIBRE' },
    { hora: '13:00', estado: 'RESERVADO' },
  ],
}
const usuario = { codigo: 'DEMO01', correo: 'demo01@tecsup.edu.pe', nombre: 'Ana Demo' }

function abrir(propiedades = {}) {
  const alReservar = vi.fn()
  render(
    <ModalReserva
      cubiculo={cubiculo}
      horaInicial="10:00"
      usuario={usuario}
      onCerrar={() => {}}
      onReservado={alReservar}
      {...propiedades}
    />,
  )
  return { alReservar, usuarioEvento: userEvent.setup() }
}

describe('ModalReserva', () => {
  beforeEach(() => {
    reservasApi.crear.mockReset()
  })

  it('reserva una hora para uno mismo', async () => {
    reservasApi.crear.mockResolvedValue({ id: 1 })
    const { alReservar, usuarioEvento } = abrir({ horaInicial: '11:00' })

    await usuarioEvento.click(screen.getByRole('button', { name: 'Confirmar reserva' }))

    expect(reservasApi.crear).toHaveBeenCalledWith({ cubiculoId: 7, horaInicio: '11:00', integrantes: [] })
    await waitFor(() => expect(alReservar).toHaveBeenCalledWith({ id: 1 }))
  })

  it('en un relevo pide una persona por cada hora extra y reserva el bloque', async () => {
    reservasApi.crear.mockResolvedValue({ id: 2 })
    const { usuarioEvento } = abrir()

    await usuarioEvento.click(screen.getByRole('radio', { name: /Relevo en grupo/ }))
    await usuarioEvento.selectOptions(screen.getByLabelText('Cantidad de personas (contándote)'), '3')

    const confirmar = screen.getByRole('button', { name: 'Confirmar reserva' })
    expect(confirmar).toBeDisabled() // faltan los códigos
    await usuarioEvento.type(screen.getByLabelText('Código o correo de la persona 2'), 'DEMO02')
    await usuarioEvento.type(screen.getByLabelText('Código o correo de la persona 3'), 'demo03@tecsup.edu.pe')
    expect(screen.getByText(/10:00/, { selector: 'strong' })).toBeInTheDocument()
    expect(screen.getByText(/13:00/, { selector: 'strong' })).toBeInTheDocument()

    await usuarioEvento.click(confirmar)
    expect(reservasApi.crear).toHaveBeenCalledWith({
      cubiculoId: 7,
      horaInicio: '10:00',
      integrantes: ['DEMO02', 'demo03@tecsup.edu.pe'],
    })
  })

  it('el relevo no puede ser más largo que los turnos libres seguidos', async () => {
    const { usuarioEvento } = abrir({ horaInicial: '11:00' }) // solo 11:00 y 12:00 están libres seguidos
    await usuarioEvento.click(screen.getByRole('radio', { name: /Relevo en grupo/ }))
    const opciones = screen.getAllByRole('option').map((o) => o.textContent)
    expect(opciones).toContain('2 personas · 2 horas')
    expect(opciones).not.toContain('3 personas · 3 horas')
  })

  it('no deja confirmar si alguien del grupo está repetido', async () => {
    const { usuarioEvento } = abrir()
    await usuarioEvento.click(screen.getByRole('radio', { name: /Relevo en grupo/ }))
    await usuarioEvento.type(screen.getByLabelText('Código o correo de la persona 2'), 'demo01')
    expect(screen.getByText('Hay una persona repetida en el grupo.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Confirmar reserva' })).toBeDisabled()
  })

  it('muestra el error que devuelve el servidor y deja volver a intentar', async () => {
    reservasApi.crear.mockRejectedValue(new Error('Ya usaste tu hora de hoy.'))
    const { alReservar, usuarioEvento } = abrir()

    await usuarioEvento.click(screen.getByRole('button', { name: 'Confirmar reserva' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Ya usaste tu hora de hoy.')
    expect(alReservar).not.toHaveBeenCalled()
    expect(screen.getByRole('button', { name: 'Confirmar reserva' })).toBeEnabled()
  })

  it('si ya no quedan turnos libres, lo avisa', () => {
    abrir({ cubiculo: { ...cubiculo, slots: cubiculo.slots.map((s) => ({ ...s, estado: 'RESERVADO' })) } })
    expect(screen.getByText('Ya no quedan turnos libres hoy en este cubículo.')).toBeInTheDocument()
  })
})
