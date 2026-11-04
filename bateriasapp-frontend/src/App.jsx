import { useCallback, useEffect, useState } from 'react'
import { IconoBateria, ProveedorAvisos } from './componentes.jsx'
import Pedidos from './pages/Pedidos.jsx'
import Entregas from './pages/Entregas.jsx'
import BateriasUsadas from './pages/BateriasUsadas.jsx'
import Sugerencias from './pages/Sugerencias.jsx'

const SECCIONES = [
  { id: 'pedidos', titulo: 'Pedidos', descripcion: 'Sigue cada pedido desde la cotización hasta la factura.' },
  { id: 'entregas', titulo: 'Entregas', descripcion: 'Asigna domiciliarios y confirma cuándo llega cada batería.' },
  { id: 'baterias', titulo: 'Baterías usadas', descripcion: 'Registra las baterías que recibe la tienda y evalúalas en bodega.' },
  { id: 'sugerencias', titulo: 'Sugerencias', descripcion: 'Consulta qué baterías encajan con un vehículo. Tú eliges la final.' },
]

function seccionGuardada() {
  try {
    const guardada = localStorage.getItem('seccion')
    return SECCIONES.some((s) => s.id === guardada) ? guardada : 'pedidos'
  } catch {
    return 'pedidos'
  }
}

export default function App() {
  const [seccion, setSeccion] = useState(seccionGuardada)
  const [pedidoParaEntrega, setPedidoParaEntrega] = useState(null)
  const actual = SECCIONES.find((s) => s.id === seccion)

  useEffect(() => {
    document.title = `${actual.titulo} · BateriasApp`
    try {
      localStorage.setItem('seccion', seccion)
    } catch {
      // sin almacenamiento disponible: no pasa nada
    }
  }, [actual, seccion])

  const irA = useCallback((destino, pedidoId = null) => {
    setPedidoParaEntrega(pedidoId)
    setSeccion(destino)
    window.scrollTo({ top: 0 })
  }, [])
  const consumirPedido = useCallback(() => setPedidoParaEntrega(null), [])

  return (
    <ProveedorAvisos>
      <a className="saltar" href="#contenido">Saltar al contenido</a>
      <header className="cabecera">
        <div className="cabecera-interior">
          <p className="marca">
            <IconoBateria />
            BateriasApp
          </p>
          <nav aria-label="Secciones">
            {SECCIONES.map((s) => (
              <button
                key={s.id}
                className="pestana"
                aria-current={s.id === seccion ? 'page' : undefined}
                onClick={() => irA(s.id)}
              >
                {s.titulo}
              </button>
            ))}
          </nav>
        </div>
      </header>
      <main id="contenido">
        <div className="titulo-seccion">
          <h1>{actual.titulo}</h1>
          <p>{actual.descripcion}</p>
        </div>
        {seccion === 'pedidos' && <Pedidos irA={irA} />}
        {seccion === 'entregas' && <Entregas pedidoInicial={pedidoParaEntrega} alConsumir={consumirPedido} />}
        {seccion === 'baterias' && <BateriasUsadas />}
        {seccion === 'sugerencias' && <Sugerencias />}
      </main>
    </ProveedorAvisos>
  )
}
