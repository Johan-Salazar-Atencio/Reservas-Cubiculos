import { Clock, DoorOpen, Lightbulb, Timer, Users } from 'lucide-react'
import Leyenda from '../../components/Leyenda.jsx'

/** Las reglas de uso de los cubículos, explicadas en corto. */
export default function Normas() {
  return (
    <>
      <div className="encabezado-pagina">
        <div>
          <h1>Normas de uso</h1>
          <p>Lo que debes saber antes de reservar un cubículo.</p>
        </div>
      </div>

      <div className="normas">
        <article className="norma">
          <h3>
            <Clock size={20} aria-hidden="true" />
            Horario y reservas
          </h3>
          <ul>
            <li>Hay 4 cubículos, con atención de 7:00 a 19:00 (hora de Lima).</li>
            <li>Solo se reserva para el mismo día: no se aparta para mañana.</li>
            <li>Cada estudiante tiene como máximo 1 hora al día.</li>
            <li>Los turnos son de 1 hora, en punto: 7:00, 8:00, 9:00…</li>
          </ul>
        </article>

        <article className="norma">
          <h3>
            <Users size={20} aria-hidden="true" />
            Relevos (grupos)
          </h3>
          <ul>
            <li>Varias personas suman sus horas para apartar un bloque seguido en el mismo cubículo.</li>
            <li>Para 2 horas hacen falta el código o correo de 2 personas, para 3 horas el de 3, y así.</li>
            <li>Cada persona gasta su hora del día.</li>
            <li>Si un turno del bloque no está libre, no se reserva nada.</li>
          </ul>
        </article>

        <article className="norma">
          <h3>
            <DoorOpen size={20} aria-hidden="true" />
            Llegada
          </h3>
          <ul>
            <li>Sensor de movimiento del cubículo confirma que llegó alguien.</li>
            <li>Si en 10 minutos desde el inicio no llega nadie, la sala se libera sola y la luz se apaga.</li>
            <li>Con la llegada confirmada, el cubículo sigue tuyo hasta que termine tu hora.</li>
            <li>Si sales antes, pulsa «Terminé» para liberarlo.</li>
          </ul>
        </article>

        <article className="norma">
          <h3>
            <Timer size={20} aria-hidden="true" />
            Cancelar y ausencias
          </h3>
          <ul>
            <li>Cancelar hasta 15 minutos antes del inicio te devuelve la hora.</li>
            <li>Cancelar más tarde gasta la hora, pero no cuenta como ausencia.</li>
            <li>No llegar (la sala se libera a los 10 minutos) cuenta como ausencia.</li>
            <li>Con 2 ausencias seguidas, tus reservas quedan en pausa 1 día de atención.</li>
          </ul>
        </article>
      </div>

      <section className="seccion" aria-labelledby="titulo-luces">
        <div className="seccion-titulo">
          <h2 id="titulo-luces">
            <Lightbulb size={20} aria-hidden="true" style={{ verticalAlign: '-3px', marginRight: 8 }} />
            Colores y luz de la maqueta
          </h2>
        </div>
        <p style={{ marginBottom: 12 }}>
          Cada color de la página coincide con la luz que muestra la tira LED del cubículo:
        </p>
        <Leyenda />
      </section>
    </>
  )
}
