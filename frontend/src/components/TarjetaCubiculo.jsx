import { DoorClosed, DoorOpen, Hourglass, Lock, Timer, UserCheck, Users, Wrench } from 'lucide-react'
import { useCuentaRegresiva } from '../hooks/useCuentaRegresiva.js'
import { ESTADOS, lineasDeEstado, minSeg, textoDeBanner } from '../utils/formato.js'

const ICONOS = {
  LIBRE: DoorOpen,
  RESERVADO: Hourglass,
  OCUPADO: UserCheck,
  POR_TERMINAR: Timer,
  OCUPADO_ADMIN: Lock,
  MANTENIMIENTO: Wrench,
  CERRADO: DoorClosed,
}

/**
 * Tarjeta de un cubículo, parecida a las tarjetas de curso de Canvas: franja de color según el estado,
 * qué pasa ahora, hasta qué hora y qué luz mostrará la maqueta. {detalle} y {children} son extras
 * (datos del administrador y botones de acción).
 */
export default function TarjetaCubiculo({ c, cierre = '19:00', recibidoEn = 0, detalle = null, children = null }) {
  const info = ESTADOS[c.estado]
  const Icono = ICONOS[c.estado]
  const { principal, secundaria } = lineasDeEstado(c, cierre)
  const restante = useCuentaRegresiva(c.estado === 'RESERVADO' ? c.segundosParaLimite : null, recibidoEn)

  return (
    <article className={`tarjeta-cubiculo estado-${info.clase}`} aria-label={`${c.nombre}: ${info.texto}`}>
      <div className="tc-banner">
        <span className="tc-numero" aria-hidden="true">
          {c.numero}
        </span>
        <span className="tc-estado">
          <Icono size={20} aria-hidden="true" />
          {textoDeBanner(c)}
        </span>
      </div>
      <div className="tc-cuerpo">
        <h3>{c.nombre}</h3>
        <p className="tc-principal">{principal}</p>
        {secundaria && <p className="tc-secundaria">{secundaria}</p>}
        {restante != null && <p className="tc-cuenta">Faltan {minSeg(restante)} para confirmar la llegada</p>}
        {c.tipoReserva === 'GRUPO' && (
          <p className="tc-secundaria">
            <Users size={14} aria-hidden="true" /> Relevo de {c.personas} personas
          </p>
        )}
        {c.miReservaId && (
          <p>
            <span className="chip chip-ocupado">Tu reserva</span>
          </p>
        )}
        {detalle}
        <p className="tc-luz">
          <span className={`punto-luz ${info.claseLuz}`} aria-hidden="true" />
          Luz de la maqueta: {info.luz}
        </p>
      </div>
      {children && <div className="tc-pie">{children}</div>}
    </article>
  )
}
