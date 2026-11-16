import { useEffect, useState } from 'react'
import { useReportarServidor } from '../../context/ServidorContext.jsx'
import { usePolling } from '../../hooks/usePolling.js'
import { adminApi, cubiculosApi } from '../../services/endpoints.js'
import { Cargando, ErrorCarga } from '../../components/Estados.jsx'
import { ESTADOS_RESERVA } from '../../utils/formato.js'

/** Quién reservó qué: reservas de un día, con los códigos completos y filtros. */
export default function ReservasAdmin() {
  const [fecha, setFecha] = useState('')
  const [cubiculoId, setCubiculoId] = useState('')
  const [estado, setEstado] = useState('')
  const [texto, setTexto] = useState('')
  const [cubiculos, setCubiculos] = useState([])

  useEffect(() => {
    cubiculosApi
      .agenda()
      .then((agenda) => setCubiculos(agenda.cubiculos))
      .catch(() => setCubiculos([]))
  }, [])

  const { datos, error, refrescar } = usePolling(
    () => adminApi.reservas({ fecha, cubiculoId, estado, q: texto }),
    4000,
    [fecha, cubiculoId, estado, texto],
  )
  useReportarServidor(undefined, error)

  return (
    <>
      <div className="encabezado-pagina">
        <div>
          <h1>Reservas</h1>
          <p>Quién reservó, con el código completo. Sin fecha, se muestran las de hoy.</p>
        </div>
      </div>

      <div className="filtros">
        <div className="campo">
          <label htmlFor="f-fecha">Fecha</label>
          <input id="f-fecha" type="date" className="control" value={fecha} onChange={(e) => setFecha(e.target.value)} />
        </div>
        <div className="campo">
          <label htmlFor="f-cubiculo">Cubículo</label>
          <select id="f-cubiculo" className="control" value={cubiculoId} onChange={(e) => setCubiculoId(e.target.value)}>
            <option value="">Todos</option>
            {cubiculos.map((c) => (
              <option key={c.id} value={c.id}>
                {c.nombre}
              </option>
            ))}
          </select>
        </div>
        <div className="campo">
          <label htmlFor="f-estado">Estado</label>
          <select id="f-estado" className="control" value={estado} onChange={(e) => setEstado(e.target.value)}>
            <option value="">Todos</option>
            {Object.entries(ESTADOS_RESERVA).map(([clave, info]) => (
              <option key={clave} value={clave}>
                {info.texto}
              </option>
            ))}
          </select>
        </div>
        <div className="campo" style={{ flex: 1, minWidth: 200 }}>
          <label htmlFor="f-buscar">Buscar persona</label>
          <input
            id="f-buscar"
            className="control"
            value={texto}
            onChange={(e) => setTexto(e.target.value)}
            placeholder="Nombre, código o correo"
          />
        </div>
        <button type="button" className="btn" onClick={() => { setFecha(''); setCubiculoId(''); setEstado(''); setTexto('') }}>
          Limpiar
        </button>
      </div>

      {!datos ? (
        error ? <ErrorCarga error={error} onReintentar={refrescar} /> : <Cargando texto="Cargando las reservas…" />
      ) : (
        <div className="tabla-contenedor">
          <table className="tabla">
            <thead>
              <tr>
                <th scope="col">Horario</th>
                <th scope="col">Cubículo</th>
                <th scope="col">Reservó</th>
                <th scope="col">Integrantes</th>
                <th scope="col">Estado</th>
                <th scope="col">Llegada</th>
                <th scope="col">Motivo</th>
              </tr>
            </thead>
            <tbody>
              {datos.length === 0 ? (
                <tr>
                  <td colSpan={7} className="tabla-vacia">
                    No hay reservas con esos filtros.
                  </td>
                </tr>
              ) : (
                datos.map((r) => {
                  const info = ESTADOS_RESERVA[r.estado]
                  return (
                    <tr key={r.id}>
                      <td>
                        {r.horaInicio} – {r.horaFin}
                      </td>
                      <td>Cubículo {r.cubiculoNumero}</td>
                      <td>
                        {r.creador.nombre} <span className="codigo">{r.creador.codigo}</span>
                      </td>
                      <td>
                        {r.tipo === 'ADMIN'
                          ? 'Administración'
                          : r.integrantes.length > 1
                            ? r.integrantes.map((p) => `${p.nombre} (${p.codigo})`).join(', ')
                            : '—'}
                      </td>
                      <td>
                        <span className={`chip chip-${info.clase}`}>{info.texto}</span>
                      </td>
                      <td>{r.checkInEn ?? '—'}</td>
                      <td>{r.motivo ?? '—'}</td>
                    </tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>
      )}
    </>
  )
}
