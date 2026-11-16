import { useReportarServidor } from '../../context/ServidorContext.jsx'
import { usePolling } from '../../hooks/usePolling.js'
import { adminApi } from '../../services/endpoints.js'
import { Cargando, ErrorCarga } from '../../components/Estados.jsx'
import { ACCIONES, fechaCorta, horaDe } from '../../utils/formato.js'

/** Registro de auditoría: quién hizo qué, cuándo y por qué (acciones del administrador y de la propia página). */
export default function Registro() {
  const { datos, error, refrescar } = usePolling(() => adminApi.registro(60), 5000)
  useReportarServidor(undefined, error)

  return (
    <>
      <div className="encabezado-pagina">
        <div>
          <h1>Registro</h1>
          <p>Las acciones más recientes. SISTEMA son los cambios automáticos de la página.</p>
        </div>
      </div>

      {!datos ? (
        error ? <ErrorCarga error={error} onReintentar={refrescar} /> : <Cargando texto="Cargando el registro…" />
      ) : (
        <div className="tabla-contenedor">
          <table className="tabla">
            <thead>
              <tr>
                <th scope="col">Cuándo</th>
                <th scope="col">Quién</th>
                <th scope="col">Acción</th>
                <th scope="col">Cubículo</th>
                <th scope="col">Motivo</th>
                <th scope="col">Detalle</th>
              </tr>
            </thead>
            <tbody>
              {datos.length === 0 ? (
                <tr>
                  <td colSpan={6} className="tabla-vacia">
                    Todavía no hay acciones registradas.
                  </td>
                </tr>
              ) : (
                datos.map((r) => (
                  <tr key={r.id}>
                    <td>
                      {fechaCorta(r.momento)} {horaDe(r.momento)}
                    </td>
                    <td>
                      <span className="codigo">{r.actor}</span>
                    </td>
                    <td>{ACCIONES[r.accion] ?? r.accion}</td>
                    <td>{r.cubiculo ? `Cubículo ${r.cubiculo}` : '—'}</td>
                    <td>{r.motivo ?? '—'}</td>
                    <td>{r.detalle ?? '—'}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}
    </>
  )
}
