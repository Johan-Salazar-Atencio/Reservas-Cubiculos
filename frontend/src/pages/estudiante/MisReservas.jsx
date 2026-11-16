import { useState } from 'react'
import { Link } from 'react-router-dom'
import { AlertTriangle } from 'lucide-react'
import { useReportarServidor } from '../../context/ServidorContext.jsx'
import { useToast } from '../../components/Toast.jsx'
import { usePolling } from '../../hooks/usePolling.js'
import { reservasApi } from '../../services/endpoints.js'
import TarjetaReserva from '../../components/TarjetaReserva.jsx'
import ConfirmarModal from '../../components/ConfirmarModal.jsx'
import { Cargando, ErrorCarga } from '../../components/Estados.jsx'
import { ESTADOS_RESERVA, fechaCorta, fechaLarga } from '../../utils/formato.js'

/** Mis reservas: cuánto queda de la hora de hoy, las reservas de hoy y el historial reciente. */
export default function MisReservas() {
  const { mostrar } = useToast()
  const { datos, error, recibidoEn, refrescar } = usePolling(reservasApi.mias, 4000)
  const [confirmando, setConfirmando] = useState(null)
  useReportarServidor(datos, error)

  if (!datos) {
    return error ? <ErrorCarga error={error} onReintentar={refrescar} /> : <Cargando texto="Cargando tus reservas…" />
  }

  const { cuota, pausa, hoy, historial } = datos
  const porcentaje = Math.min(100, Math.round((cuota.usadas / cuota.horasPorDia) * 100))

  async function confirmarAccion() {
    const { tipo, reserva } = confirmando
    if (tipo === 'cancelar') {
      await reservasApi.cancelar(reserva.id)
      mostrar('Reserva cancelada.')
    } else {
      await reservasApi.terminar(reserva.id)
      mostrar('Listo, el cubículo quedó libre. Gracias por avisar.')
    }
    setConfirmando(null)
    refrescar()
  }

  return (
    <>
      <div className="encabezado-pagina">
        <div>
          <h1>Mis reservas</h1>
          <p>Tu hora de hoy, tus reservas y el historial reciente.</p>
        </div>
      </div>

      {pausa.activa && (
        <div className="aviso aviso-peligro" role="alert">
          <AlertTriangle size={20} aria-hidden="true" />
          <span>Tus reservas están en pausa hasta el {fechaLarga(pausa.hasta)} por ausencias seguidas.</span>
        </div>
      )}

      <div className="rejilla-datos">
        <div className="dato">
          <p className="dato-numero">
            {cuota.usadas} de {cuota.horasPorDia}
          </p>
          <p className="dato-etiqueta">hora de hoy usada</p>
          <div className="barra" aria-hidden="true">
            <span style={{ width: `${porcentaje}%` }} />
          </div>
        </div>
        <div className="dato">
          <p className="dato-numero">
            {datos.ausenciasSeguidas} de {datos.ausenciasParaPausa}
          </p>
          <p className="dato-etiqueta">ausencias seguidas (con {datos.ausenciasParaPausa} se pausan tus reservas 1 día)</p>
        </div>
      </div>

      <section className="seccion" aria-labelledby="titulo-hoy">
        <div className="seccion-titulo">
          <h2 id="titulo-hoy">Hoy</h2>
        </div>
        {hoy.length === 0 ? (
          <p>
            Hoy no tienes reservas. Ve al <Link to="/">Tablero</Link> para reservar un cubículo.
          </p>
        ) : (
          <div className="reserva-lista">
            {hoy.map((r) => (
              <TarjetaReserva
                key={r.id}
                r={r}
                recibidoEn={recibidoEn}
                onCancelar={(x) => setConfirmando({ tipo: 'cancelar', reserva: x })}
                onTerminar={(x) => setConfirmando({ tipo: 'terminar', reserva: x })}
              />
            ))}
          </div>
        )}
      </section>

      <section className="seccion" aria-labelledby="titulo-historial">
        <div className="seccion-titulo">
          <h2 id="titulo-historial">Historial reciente</h2>
        </div>
        <div className="tabla-contenedor">
          <table className="tabla">
            <thead>
              <tr>
                <th scope="col">Fecha</th>
                <th scope="col">Cubículo</th>
                <th scope="col">Horario</th>
                <th scope="col">Estado</th>
              </tr>
            </thead>
            <tbody>
              {historial.length === 0 ? (
                <tr>
                  <td colSpan={4} className="tabla-vacia">
                    Todavía no tienes reservas anteriores.
                  </td>
                </tr>
              ) : (
                historial.map((r) => {
                  const info = ESTADOS_RESERVA[r.estado]
                  return (
                    <tr key={r.id}>
                      <td>{fechaCorta(r.fecha)}</td>
                      <td>Cubículo {r.cubiculoNumero}</td>
                      <td>
                        {r.horaInicio} – {r.horaFin}
                      </td>
                      <td>
                        <span className={`chip chip-${info.clase}`}>{info.texto}</span>
                      </td>
                    </tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>
      </section>

      {confirmando?.tipo === 'cancelar' && (
        <ConfirmarModal
          titulo="Cancelar reserva"
          etiqueta="Cancelar reserva"
          peligro
          onConfirmar={confirmarAccion}
          onCerrar={() => setConfirmando(null)}
        >
          <p>
            Vas a cancelar tu reserva del Cubículo {confirmando.reserva.cubiculoNumero}, de{' '}
            {confirmando.reserva.horaInicio} a {confirmando.reserva.horaFin}.
          </p>
          <p style={{ marginTop: 8 }}>
            {confirmando.reserva.cancelacionGratis
              ? 'Cancelas con tiempo: se te devuelve tu hora de hoy.'
              : 'Ya falta poco o el turno ya empezó: al cancelar ahora se gasta tu hora de hoy, pero no cuenta como ausencia.'}
          </p>
        </ConfirmarModal>
      )}

      {confirmando?.tipo === 'terminar' && (
        <ConfirmarModal
          titulo="¿Terminaste?"
          etiqueta="Terminé"
          onConfirmar={confirmarAccion}
          onCerrar={() => setConfirmando(null)}
        >
          <p>El cubículo quedará libre para otras personas y tu hora de hoy queda usada.</p>
        </ConfirmarModal>
      )}
    </>
  )
}
