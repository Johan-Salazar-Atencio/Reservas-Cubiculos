import { Users } from 'lucide-react'
import { useCuentaRegresiva } from '../hooks/useCuentaRegresiva.js'
import { ESTADOS_RESERVA, horasTexto, minSeg } from '../utils/formato.js'

/** Una reserva de hoy: horario, estado, con quién y los botones que correspondan (Cancelar o Terminé). */
export default function TarjetaReserva({ r, recibidoEn = 0, onCancelar, onTerminar }) {
  const info = ESTADOS_RESERVA[r.estado]
  // La cuenta regresiva de la llegada solo corre cuando el turno ya empezó.
  const restante = useCuentaRegresiva(
    r.estado === 'RESERVADA' && r.yaEmpezo ? r.segundosParaLimite : null,
    recibidoEn,
  )
  const companeros = r.integrantes.filter((p) => p.nombre !== r.creador.nombre).map((p) => p.nombre)

  let detalle = ''
  if (r.estado === 'RESERVADA') {
    detalle = r.yaEmpezo
      ? `Llega antes de las ${r.checkInLimite} para confirmar tu llegada.`
      : `Empieza a las ${r.horaInicio}. Llega antes de las ${r.checkInLimite} para confirmar tu llegada.`
  } else if (r.estado === 'EN_CURSO') {
    detalle = `Llegada confirmada a las ${r.checkInEn}. Cuando termines, pulsa «Terminé».`
  } else if (r.estado === 'AUSENTE') {
    detalle = 'Nadie llegó a tiempo y la sala se liberó.'
  } else if (r.estado === 'CANCELADA') {
    detalle = 'Cancelada a tiempo: se te devolvió la hora.'
  } else if (r.estado === 'CANCELADA_TARDE') {
    detalle = 'Cancelada tarde: la hora de hoy se gastó.'
  } else if (r.estado === 'ANULADA_ADMIN') {
    detalle = `Anulada por administración${r.motivo ? `: ${r.motivo}` : ''}. Se te devolvió la hora.`
  }

  return (
    <div className={`reserva-fila estado-${info.clase}`}>
      <div>
        <h3>
          Cubículo {r.cubiculoNumero} · {r.horaInicio} – {r.horaFin}
          <span className={`chip chip-${info.clase}`}>{info.texto}</span>
          {r.tipo === 'GRUPO' && (
            <span className="chip chip-neutro">
              <Users size={14} aria-hidden="true" />
              Relevo · {horasTexto(r.horas)}
            </span>
          )}
        </h3>
        <p>{detalle}</p>
        {restante != null && <p className="tc-cuenta">Faltan {minSeg(restante)} para confirmar la llegada</p>}
        {companeros.length > 0 && <p>Con: {companeros.join(', ')}</p>}
      </div>
      {(r.puedeCancelar || r.puedeTerminar) && (
        <div className="grupo-botones">
          {r.puedeCancelar && (
            <button type="button" className="btn btn-peligro" onClick={() => onCancelar(r)}>
              Cancelar reserva
            </button>
          )}
          {r.puedeTerminar && (
            <button type="button" className="btn btn-primario" onClick={() => onTerminar(r)}>
              Terminé
            </button>
          )}
        </div>
      )}
    </div>
  )
}
