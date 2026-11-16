import { useState } from 'react'
import { Wifi, WifiOff } from 'lucide-react'
import { useReportarServidor } from '../../context/ServidorContext.jsx'
import { usePolling } from '../../hooks/usePolling.js'
import { adminApi } from '../../services/endpoints.js'
import { Cargando, ErrorCarga } from '../../components/Estados.jsx'
import { horaDe } from '../../utils/formato.js'

/** Estadísticas simples: horas pico, ausencias, ocupación por cubículo y estado de los sensores. */
export default function Estadisticas() {
  const [dias, setDias] = useState(7)
  const { datos, error, refrescar } = usePolling(() => adminApi.estadisticas(dias), 10000, [dias])
  useReportarServidor(datos, error)

  const maximo = datos ? Math.max(1, ...datos.horasPico.map((h) => h.reservas)) : 1

  return (
    <>
      <div className="encabezado-pagina">
        <div>
          <h1>Estadísticas</h1>
          <p>Resumen de los últimos días, incluido hoy.</p>
        </div>
        <div className="campo">
          <label htmlFor="dias">Periodo</label>
          <select id="dias" className="control" value={dias} onChange={(e) => setDias(Number(e.target.value))}>
            <option value={7}>Últimos 7 días</option>
            <option value={14}>Últimos 14 días</option>
            <option value={30}>Últimos 30 días</option>
          </select>
        </div>
      </div>

      {!datos ? (
        error ? <ErrorCarga error={error} onReintentar={refrescar} /> : <Cargando texto="Calculando las estadísticas…" />
      ) : (
        <>
          <div className="rejilla-datos">
            <div className="dato">
              <p className="dato-numero">{datos.totales.reservas}</p>
              <p className="dato-etiqueta">reservas en el periodo</p>
            </div>
            <div className="dato">
              <p className="dato-numero">{datos.totales.completadas}</p>
              <p className="dato-etiqueta">atendidas (llegó alguien)</p>
            </div>
            <div className="dato">
              <p className="dato-numero">
                {datos.totales.ausentes} <small style={{ fontSize: 18 }}>({datos.totales.tasaAusenciaPorc} %)</small>
              </p>
              <p className="dato-etiqueta">ausencias (sala liberada a los 10 min)</p>
            </div>
            <div className="dato">
              <p className="dato-numero">{datos.totales.canceladas}</p>
              <p className="dato-etiqueta">canceladas o anuladas</p>
            </div>
          </div>

          <section className="seccion" aria-labelledby="titulo-picos">
            <div className="seccion-titulo">
              <h2 id="titulo-picos">Horas pico</h2>
              <span>Reservas que ocupan cada hora del día</span>
            </div>
            <div className="barras" role="img" aria-label="Reservas por hora del día">
              {datos.horasPico.map((h) => (
                <div className="barra-columna" key={h.hora}>
                  <span className="barra-valor">{h.reservas}</span>
                  <div
                    className={`barra-relleno${h.reservas === maximo && h.reservas > 0 ? ' pico' : ''}`}
                    style={{ height: `calc((100% - 46px) * ${h.reservas / maximo})` }}
                  />
                  <span className="barra-etiqueta">{h.hora}h</span>
                </div>
              ))}
            </div>
          </section>

          <div className="dos-columnas seccion">
            <section aria-labelledby="titulo-ausencias">
              <div className="seccion-titulo">
                <h2 id="titulo-ausencias">Ausencias por persona</h2>
              </div>
              <div className="tabla-contenedor">
                <table className="tabla">
                  <thead>
                    <tr>
                      <th scope="col">Estudiante</th>
                      <th scope="col">Ausencias</th>
                    </tr>
                  </thead>
                  <tbody>
                    {datos.ausencias.length === 0 ? (
                      <tr>
                        <td colSpan={2} className="tabla-vacia">
                          Sin ausencias en el periodo.
                        </td>
                      </tr>
                    ) : (
                      datos.ausencias.map((a) => (
                        <tr key={a.codigo}>
                          <td>
                            {a.nombre} <span className="codigo">{a.codigo}</span>
                          </td>
                          <td>{a.ausencias}</td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </section>

            <section aria-labelledby="titulo-ocupacion">
              <div className="seccion-titulo">
                <h2 id="titulo-ocupacion">Ocupación por cubículo</h2>
              </div>
              <div className="tabla-contenedor">
                <table className="tabla">
                  <thead>
                    <tr>
                      <th scope="col">Cubículo</th>
                      <th scope="col">Horas reservadas</th>
                      <th scope="col">Horas con gente</th>
                      <th scope="col">Ausencias</th>
                    </tr>
                  </thead>
                  <tbody>
                    {datos.ocupacion.map((o) => (
                      <tr key={o.cubiculo}>
                        <td>Cubículo {o.cubiculo}</td>
                        <td>{o.horasReservadas}</td>
                        <td>{o.horasOcupadas}</td>
                        <td>{o.ausencias}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>
          </div>

          <section className="seccion" aria-labelledby="titulo-sensores">
            <div className="seccion-titulo">
              <h2 id="titulo-sensores">Estado de los sensores</h2>
            </div>
            <div className="tabla-contenedor">
              <table className="tabla">
                <thead>
                  <tr>
                    <th scope="col">Cubículo</th>
                    <th scope="col">Estado</th>
                    <th scope="col">Último latido</th>
                    <th scope="col">Última detección</th>
                  </tr>
                </thead>
                <tbody>
                  {datos.sensores.map((s) => (
                    <tr key={s.cubiculo}>
                      <td>Cubículo {s.cubiculo}</td>
                      <td>
                        <span className={`tc-sensor ${s.enLinea ? 'en-linea' : 'sin-senal'}`}>
                          {s.enLinea ? <Wifi size={16} aria-hidden="true" /> : <WifiOff size={16} aria-hidden="true" />}
                          {s.enLinea ? 'En línea' : 'Sin señal'}
                        </span>
                      </td>
                      <td>{s.ultimoLatido ? horaDe(s.ultimoLatido) : '—'}</td>
                      <td>{s.ultimaPresencia ? horaDe(s.ultimaPresencia) : '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </>
      )}
    </>
  )
}
