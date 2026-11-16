const ETIQUETA = {
  PASADO: '',
  LIBRE: '',
  RESERVADO: 'Reservado',
  EN_CURSO: 'Ocupado',
  BLOQUEADO: 'Admin',
  MANTENIMIENTO: 'Manten.',
  CERRADO: '',
}

const NOMBRE = {
  PASADO: 'ya pasó',
  LIBRE: 'libre',
  RESERVADO: 'reservado',
  EN_CURSO: 'ocupado',
  BLOQUEADO: 'ocupado por administración',
  MANTENIMIENTO: 'en mantenimiento',
  CERRADO: 'cerrado',
}

/** Agenda de hoy: una fila por cubículo y una columna por hora. Solo los turnos libres se pueden pulsar. */
export default function AgendaGrid({ agenda, onElegir, puedeReservar = true }) {
  return (
    <div className="agenda-contenedor">
      <table className="agenda">
        <caption className="solo-lectores">Turnos de hoy de cada cubículo</caption>
        <thead>
          <tr>
            <th scope="col">Cubículo</th>
            {agenda.horas.map((hora) => (
              <th key={hora} scope="col">
                {hora}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {agenda.cubiculos.map((c) => (
            <tr key={c.id}>
              <th scope="row">{c.nombre}</th>
              {c.slots.map((slot) => (
                <td key={slot.hora}>
                  <button
                    type="button"
                    className={`slot slot-${slot.estado.toLowerCase()}${slot.mia ? ' slot-mia' : ''}`}
                    disabled={slot.estado !== 'LIBRE' || !puedeReservar}
                    aria-label={`${c.nombre}, ${slot.hora}, ${NOMBRE[slot.estado]}${slot.mia ? ', es tuya' : ''}`}
                    onClick={() => onElegir(c, slot.hora)}
                  >
                    {slot.mia ? 'Tuya' : ETIQUETA[slot.estado]}
                  </button>
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
