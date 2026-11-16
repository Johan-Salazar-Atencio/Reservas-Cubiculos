import { useState } from 'react'
import { Wifi, WifiOff } from 'lucide-react'
import { useReportarServidor } from '../../context/ServidorContext.jsx'
import { useToast } from '../../components/Toast.jsx'
import { usePolling } from '../../hooks/usePolling.js'
import { adminApi } from '../../services/endpoints.js'
import TarjetaCubiculo from '../../components/TarjetaCubiculo.jsx'
import Leyenda from '../../components/Leyenda.jsx'
import ModalMotivo from '../../components/ModalMotivo.jsx'
import { Cargando, ErrorCarga } from '../../components/Estados.jsx'
import { horaDe } from '../../utils/formato.js'

const OCUPADOS = ['RESERVADO', 'OCUPADO', 'POR_TERMINAR', 'OCUPADO_ADMIN']

/** Panel del administrador: los 4 cubículos en vivo, quién los usa, el estado de cada sensor y las acciones manuales. */
export default function Monitoreo() {
  const { mostrar } = useToast()
  const { datos, error, recibidoEn, refrescar } = usePolling(adminApi.panel, 3000)
  const [accion, setAccion] = useState(null) // { tipo, cubiculo }
  useReportarServidor(datos, error)

  if (!datos) {
    return error ? <ErrorCarga error={error} onReintentar={refrescar} /> : <Cargando texto="Cargando el panel…" />
  }

  function terminar(mensaje) {
    mostrar(mensaje)
    setAccion(null)
    refrescar()
  }

  return (
    <>
      <div className="encabezado-pagina">
        <div>
          <h1>Monitoreo</h1>
          <p>Los cubículos en este momento. Toda acción manual pide un motivo y queda en el registro.</p>
        </div>
      </div>

      <div className="rejilla-cubiculos">
        {datos.cubiculos.map(({ vivo, reservaActual, proximas }) => {
          const detalle = (
            <>
              {reservaActual && (
                <ul className="tc-personas" aria-label="Personas de la reserva">
                  {reservaActual.tipo === 'ADMIN' ? (
                    <li>Administración: {reservaActual.motivo}</li>
                  ) : (
                    reservaActual.integrantes.map((p) => (
                      <li key={p.codigo}>
                        {p.nombre} <span className="codigo">{p.codigo}</span>
                      </li>
                    ))
                  )}
                </ul>
              )}
              {proximas.length > 0 && (
                <p className="tc-proximas">
                  Próximas:{' '}
                  {proximas
                    .map((r) => `${r.horaInicio}–${r.horaFin} (${r.creador.codigo}${r.horas > 1 ? ` +${r.horas - 1}` : ''})`)
                    .join(' · ')}
                </p>
              )}
              <p className={`tc-sensor ${vivo.sensor.enLinea ? 'en-linea' : 'sin-senal'}`}>
                {vivo.sensor.enLinea ? <Wifi size={16} aria-hidden="true" /> : <WifiOff size={16} aria-hidden="true" />}
                {vivo.sensor.enLinea ? 'Sensor en línea' : 'Sensor sin señal'}
                {vivo.sensor.ultimaPresencia && (
                  <span className="codigo"> · última detección {horaDe(vivo.sensor.ultimaPresencia)}</span>
                )}
              </p>
            </>
          )

          return (
            <TarjetaCubiculo key={vivo.id} c={vivo} recibidoEn={recibidoEn} detalle={detalle}>
              <button
                type="button"
                className="btn btn-peligro btn-pequeno"
                disabled={!OCUPADOS.includes(vivo.estado)}
                onClick={() => setAccion({ tipo: 'liberar', cubiculo: vivo })}
              >
                Liberar
              </button>
              <button
                type="button"
                className="btn btn-pequeno"
                disabled={vivo.estado !== 'LIBRE'}
                onClick={() => setAccion({ tipo: 'ocupar', cubiculo: vivo })}
              >
                Ocupar
              </button>
              {vivo.estado === 'MANTENIMIENTO' ? (
                <button
                  type="button"
                  className="btn btn-primario btn-pequeno"
                  onClick={() => setAccion({ tipo: 'fin-mantenimiento', cubiculo: vivo })}
                >
                  Terminar mantenimiento
                </button>
              ) : (
                <button
                  type="button"
                  className="btn btn-pequeno"
                  onClick={() => setAccion({ tipo: 'mantenimiento', cubiculo: vivo })}
                >
                  Mantenimiento
                </button>
              )}
            </TarjetaCubiculo>
          )
        })}
      </div>

      <div style={{ marginTop: 20 }}>
        <Leyenda />
      </div>

      {accion?.tipo === 'liberar' && (
        <ModalMotivo
          titulo={`Liberar ${accion.cubiculo.nombre}`}
          descripcion="Se anula la reserva u ocupación de este momento. La hora se le devuelve a la persona y no cuenta como ausencia."
          etiqueta="Liberar cubículo"
          peligro
          onCerrar={() => setAccion(null)}
          onConfirmar={async ({ motivo }) => {
            const r = await adminApi.liberar(accion.cubiculo.id, motivo)
            terminar(r.mensaje)
          }}
        />
      )}

      {accion?.tipo === 'ocupar' && (
        <ModalMotivo
          titulo={`Ocupar ${accion.cubiculo.nombre}`}
          descripcion="Marca el cubículo como ocupado por administración (visitas, eventos, uso interno) desde el turno actual."
          etiqueta="Ocupar cubículo"
          conHoras
          onCerrar={() => setAccion(null)}
          onConfirmar={async ({ motivo, horas }) => {
            const r = await adminApi.ocupar(accion.cubiculo.id, horas, motivo)
            terminar(r.mensaje)
          }}
        />
      )}

      {accion?.tipo === 'mantenimiento' && (
        <ModalMotivo
          titulo={`Mantenimiento de ${accion.cubiculo.nombre}`}
          descripcion="Se anulan las reservas de hoy de este cubículo (las horas se devuelven) y nadie podrá reservarlo hasta que termines el mantenimiento."
          etiqueta="Poner en mantenimiento"
          peligro
          onCerrar={() => setAccion(null)}
          onConfirmar={async ({ motivo }) => {
            const r = await adminApi.mantenimiento(accion.cubiculo.id, true, motivo)
            terminar(r.mensaje)
          }}
        />
      )}

      {accion?.tipo === 'fin-mantenimiento' && (
        <ModalMotivo
          titulo={`Reactivar ${accion.cubiculo.nombre}`}
          descripcion="El cubículo vuelve a estar disponible para reservar."
          etiqueta="Reactivar cubículo"
          onCerrar={() => setAccion(null)}
          onConfirmar={async ({ motivo }) => {
            const r = await adminApi.mantenimiento(accion.cubiculo.id, false, motivo)
            terminar(r.mensaje)
          }}
        />
      )}
    </>
  )
}
