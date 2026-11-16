const ITEMS = [
  { clase: 'libre', texto: 'Libre', luz: 'luz apagada' },
  { clase: 'reservado', texto: 'Reservado, aún no llega', luz: 'luz ámbar' },
  { clase: 'ocupado', texto: 'Ocupado', luz: 'luz azul' },
  { clase: 'por-terminar', texto: 'Quedan 5 min', luz: 'luz roja' },
  { clase: 'mantenimiento', texto: 'Mantenimiento', luz: 'luz violeta' },
]

/** Qué significa cada color de la página, con la luz que mostrará la maqueta en cada caso. */
export default function Leyenda() {
  return (
    <ul className="leyenda" aria-label="Significado de los colores">
      {ITEMS.map((item) => (
        <li key={item.clase} className={`estado-${item.clase}`}>
          <span className="cuadro" aria-hidden="true" />
          {item.texto} <small>({item.luz})</small>
        </li>
      ))}
    </ul>
  )
}
