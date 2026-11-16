import { useState } from 'react'
import { Info } from 'lucide-react'
import { useAuth } from '../../context/AuthContext.jsx'
import { useReportarServidor } from '../../context/ServidorContext.jsx'
import { useToast } from '../../components/Toast.jsx'
import { usePolling } from '../../hooks/usePolling.js'
import { cubiculosApi, reservasApi } from '../../services/endpoints.js'
import TarjetaCubiculo from '../../components/TarjetaCubiculo.jsx'
import AgendaGrid from '../../components/AgendaGrid.jsx'
import Leyenda from '../../components/Leyenda.jsx'
import ModalReserva from '../../components/ModalReserva.jsx'
import ConfirmarModal from '../../components/ConfirmarModal.jsx'
import TarjetaReserva from '../../components/TarjetaReserva.jsx'
import { Cargando, ErrorCarga } from '../../components/Estados.jsx'
import { fechaLarga, mayuscula } from '../../utils/formato.js'

async function cargarTablero() {
  const [vivo, agenda, mias] = await Promise.all([cubiculosApi.enVivo(), cubiculosApi.agenda(), reservasApi.mias()])
  return { vivo, agenda, mias }
}

/** Pantalla principal del estudiante: cubículos en vivo, su reserva de hoy y la agenda para reservar. */
export default function Tablero() {
  const { usuario } = useAuth()
  const { mostrar } = useToast()
  const { datos, error, recibidoEn, refrescar } = usePolling(cargarTablero, 3000)
  const [reservando, setReservando] = useState(null) // { cubiculoId, hora }
  const [confirmando, setConfirmando] = useState(null) // { tipo: 'cancelar' | 'terminar', reserva }
  useReportarServidor(datos?.vivo, error)

  if (!datos) {
    return error ? (
      <ErrorCarga error={error} onReintentar={refrescar} />
    ) : (
      <Cargando texto="Cargando los cubículos…" />
    )
  }

  const { vivo, agenda, mias } = datos
  const activas = mias.hoy.filter((r) => r.estado === 'RESERVADA' || r.estado === 'EN_CURSO')
  const cubiculoModal = reservando ? agenda.cubiculos.find((c) => c.id === reservando.cubiculoId) : null
  const puedeReservar = vivo.abierto && !mias.pausa.activa && mias.cuota.disponibles > 0

  let motivoBloqueo = null
  if (!vivo.abierto) motivoBloqueo = vivo.mensajeCierre
  else if (mias.pausa.activa) motivoBloqueo = `Tus reservas están en pausa hasta el ${fechaLarga(mias.pausa.hasta)}.`
  else if (mias.cuota.disponibles === 0) motivoBloqueo = 'Ya usaste tu hora de hoy. Mañana podrás reservar otra vez.'

  function elegir(cubiculo, hora) {
    setReservando({ cubiculoId: cubiculo.id, hora })
  }

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
          <h1>Hola, {usuario.nombre.split(' ')[0]}</h1>
          <p>{mayuscula(fechaLarga(vivo.ahora))}. Así están los cubículos en este momento.</p>
        </div>
        <span className="chip chip-neutro">
          Tu hora de hoy: {mias.cuota.usadas} de {mias.cuota.horasPorDia} usada
        </span>
      </div>

      {motivoBloqueo && (
        <div className="aviso aviso-aviso" role="status">
          <Info size={20} aria-hidden="true" />
          <span>{motivoBloqueo}</span>
        </div>
      )}

      {activas.length > 0 && (
        <section aria-labelledby="titulo-tu-reserva">
          <div className="seccion-titulo">
            <h2 id="titulo-tu-reserva">Tu reserva de hoy</h2>
          </div>
          <div className="reserva-lista">
            {activas.map((r) => (
              <TarjetaReserva
                key={r.id}
                r={r}
                recibidoEn={recibidoEn}
                onCancelar={(x) => setConfirmando({ tipo: 'cancelar', reserva: x })}
                onTerminar={(x) => setConfirmando({ tipo: 'terminar', reserva: x })}
              />
            ))}
          </div>
        </section>
      )}

      <section className="seccion" style={activas.length > 0 ? undefined : { marginTop: 0 }} aria-labelledby="titulo-vivo">
        <div className="seccion-titulo">
          <h2 id="titulo-vivo">Cubículos en vivo</h2>
          <span>Se actualiza cada 3 segundos</span>
        </div>
        <div className="rejilla-cubiculos">
          {vivo.cubiculos.map((c) => (
            <TarjetaCubiculo key={c.id} c={c} cierre={vivo.cierre} recibidoEn={recibidoEn}>
              {c.estado === 'LIBRE' && puedeReservar && (
                <button type="button" className="btn btn-primario btn-pequeno" onClick={() => elegir(c, null)}>
                  Reservar
                </button>
              )}
            </TarjetaCubiculo>
          ))}
        </div>
        <div style={{ marginTop: 16 }}>
          <Leyenda />
        </div>
      </section>

      <section className="seccion" aria-labelledby="titulo-agenda">
        <div className="seccion-titulo">
          <h2 id="titulo-agenda">Agenda de hoy</h2>
          <span>Toca un turno libre para reservarlo</span>
        </div>
        <AgendaGrid agenda={agenda} onElegir={elegir} puedeReservar={puedeReservar} />
      </section>

      {cubiculoModal && (
        <ModalReserva
          key={cubiculoModal.id}
          cubiculo={cubiculoModal}
          horaInicial={reservando.hora}
          usuario={usuario}
          onCerrar={() => setReservando(null)}
          onReservado={(r) => {
            setReservando(null)
            mostrar(
              `Reserva confirmada: Cubículo ${r.cubiculoNumero}, de ${r.horaInicio} a ${r.horaFin}. ` +
                `Llega antes de las ${r.checkInLimite} para confirmar tu llegada.`,
            )
            refrescar()
          }}
        />
      )}

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
