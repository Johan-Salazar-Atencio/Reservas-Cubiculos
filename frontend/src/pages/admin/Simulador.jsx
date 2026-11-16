import { useState } from 'react'
import { useAuth } from '../../context/AuthContext.jsx'
import { useReportarServidor } from '../../context/ServidorContext.jsx'
import { useToast } from '../../components/Toast.jsx'
import { usePolling } from '../../hooks/usePolling.js'
import { adminApi, simuladorApi } from '../../services/endpoints.js'
import ConfirmarModal from '../../components/ConfirmarModal.jsx'
import { Cargando, ErrorCarga } from '../../components/Estados.jsx'
import { ESTADOS, horaDe } from '../../utils/formato.js'

const HORAS = Array.from({ length: 12 }, (_, i) => `${String(i + 7).padStart(2, '0')}:00`)

const CONTRATO = `POST /api/sensores/{n}/presencia   el PIR detectó a alguien
POST /api/sensores/{n}/latido      señal de vida del sensor
GET  /api/sensores/{n}/estado      qué luz mostrar (cuenta como latido)

Cabecera obligatoria:  X-Sensor-Token: <token del servidor>
Respuesta: { cubiculo, estado, luz, tipoReserva,
             minutosRestantes, checkInConfirmado, ahora }
luz: APAGADA | AMBAR | AZUL | ROJO | VIOLETA`

async function cargar() {
  const [reloj, panel] = await Promise.all([simuladorApi.reloj(), adminApi.panel()])
  return { reloj, panel }
}

/** Solo en desarrollo: adelanta el reloj y simula los sensores para probar la página sin la maqueta. */
export default function Simulador() {
  const { mostrar } = useToast()
  const { salir } = useAuth()
  const { datos, error, refrescar } = usePolling(cargar, 2500)
  const [caidos, setCaidos] = useState([])
  const [hora, setHora] = useState('11:00')
  const [restableciendo, setRestableciendo] = useState(false)
  useReportarServidor(datos?.reloj, error)

  if (!datos) {
    return error ? <ErrorCarga error={error} onReintentar={refrescar} /> : <Cargando texto="Cargando el simulador…" />
  }

  async function ejecutar(promesa, mensaje) {
    try {
      const respuesta = await promesa
      mostrar(typeof mensaje === 'function' ? mensaje(respuesta) : mensaje)
      refrescar()
    } catch (e) {
      mostrar(e.message, 'error')
    }
  }

  function alternarSenal(numero) {
    const caer = !caidos.includes(numero)
    setCaidos((lista) => (caer ? [...lista, numero] : lista.filter((n) => n !== numero)))
    ejecutar(
      simuladorApi.sinSenal(numero, caer),
      caer ? `El sensor del cubículo ${numero} dejó de dar señal.` : `El sensor del cubículo ${numero} volvió a dar señal.`,
    )
  }

  return (
    <>
      <div className="encabezado-pagina">
        <div>
          <h1>Simulador</h1>
          <p>Prueba la página sin la maqueta: adelanta el reloj y simula lo que haría el sensor de cada cubículo.</p>
        </div>
      </div>

      <div className="dos-columnas">
        <section className="panel-simulador" aria-labelledby="titulo-reloj">
          <h3 id="titulo-reloj">Reloj simulado</h3>
          <p>Así se prueban los 10 minutos de llegada y el aviso de 5 minutos sin esperar.</p>
          <p className="reloj-grande">{horaDe(datos.reloj.ahora)}</p>
          <div className="grupo-botones" style={{ margin: '14px 0' }}>
            {[5, 10, 30, 60].map((min) => (
              <button
                key={min}
                type="button"
                className="btn"
                onClick={() => ejecutar(simuladorApi.avanzar(min), `Reloj adelantado ${min} min.`)}
              >
                +{min === 60 ? '1 h' : `${min} min`}
              </button>
            ))}
          </div>
          <div className="filtros" style={{ marginBottom: 0 }}>
            <div className="campo">
              <label htmlFor="ir-hora">Ir a la hora</label>
              <select id="ir-hora" className="control" value={hora} onChange={(e) => setHora(e.target.value)}>
                {HORAS.map((h) => (
                  <option key={h} value={h}>
                    {h}
                  </option>
                ))}
              </select>
            </div>
            <button type="button" className="btn" onClick={() => ejecutar(simuladorApi.irA(hora), `Reloj en las ${hora}.`)}>
              Ir
            </button>
            <button type="button" className="btn btn-peligro" onClick={() => setRestableciendo(true)}>
              Restablecer demo
            </button>
          </div>
        </section>

        <section className="panel-simulador" aria-labelledby="titulo-sensores-sim">
          <h3 id="titulo-sensores-sim">Sensores virtuales</h3>
          <p>Lo mismo que mandaría el PIR de cada cubículo.</p>
          {datos.panel.cubiculos.map(({ vivo }) => {
            const info = ESTADOS[vivo.estado]
            const caido = caidos.includes(vivo.numero)
            return (
              <div className="sensor-fila" key={vivo.id}>
                <div>
                  <strong>{vivo.nombre}</strong>{' '}
                  <span className={`chip chip-${info.clase}`}>{info.corto ?? info.texto}</span>
                </div>
                <div className="grupo-botones">
                  <button
                    type="button"
                    className="btn btn-pequeno btn-primario"
                    onClick={() =>
                      ejecutar(simuladorApi.presencia(vivo.numero), (r) =>
                        r.checkInConfirmado
                          ? `Persona detectada en el cubículo ${vivo.numero}: llegada confirmada.`
                          : `Persona detectada en el cubículo ${vivo.numero}. No había una reserva esperando.`,
                      )
                    }
                  >
                    Detectar persona
                  </button>
                  <button
                    type="button"
                    className="btn btn-pequeno"
                    onClick={() => ejecutar(simuladorApi.latido(vivo.numero), `Latido enviado al cubículo ${vivo.numero}.`)}
                  >
                    Latido
                  </button>
                  <button type="button" className="btn btn-pequeno" onClick={() => alternarSenal(vivo.numero)}>
                    {caido ? 'Devolver señal' : 'Simular caída'}
                  </button>
                </div>
              </div>
            )
          })}
        </section>
      </div>

      <div className="dos-columnas seccion">
        <section className="panel-simulador" aria-labelledby="titulo-prueba">
          <h3 id="titulo-prueba">Prueba rápida, paso a paso</h3>
          <ol className="pasos">
            <li>Entra como estudiante (otra ventana o una cuenta demo) y reserva un turno libre de hoy.</li>
            <li>Aquí pulsa «Ir a la hora» de esa reserva: el cubículo queda en ámbar (reservado, aún no llega).</li>
            <li>Pulsa «Detectar persona» en ese cubículo: pasa a azul (ocupado).</li>
            <li>Adelanta hasta 5 minutos antes del final: la luz pasa a roja (quedan 5 minutos).</li>
            <li>Adelanta hasta la hora de fin: el cubículo se libera y la luz se apaga.</li>
            <li>
              Regla de los 10 minutos: no pulses «Detectar persona» y adelanta 10 minutos. La reserva queda como
              ausente y la sala se libera sola.
            </li>
          </ol>
        </section>

        <section className="panel-simulador" aria-labelledby="titulo-contrato">
          <h3 id="titulo-contrato">Lo que llamará la maqueta (ESP32)</h3>
          <p>Los botones de arriba usan los mismos servicios que usará la placa real.</p>
          <div className="contrato" tabIndex={0} role="region" aria-label="Contrato de la API de la maqueta">
            {CONTRATO}
          </div>
        </section>
      </div>

      {restableciendo && (
        <ConfirmarModal
          titulo="Restablecer la demostración"
          etiqueta="Restablecer demo"
          peligro
          onConfirmar={async () => {
            await simuladorApi.restablecer()
            salir('Los datos de demostración se restablecieron. Vuelve a entrar.')
          }}
          onCerrar={() => setRestableciendo(false)}
        >
          <p>
            Se borran las reservas y el registro, se crean de nuevo los datos de demostración y el reloj vuelve a las
            10:00. Tendrás que volver a entrar.
          </p>
        </ConfirmarModal>
      )}
    </>
  )
}
