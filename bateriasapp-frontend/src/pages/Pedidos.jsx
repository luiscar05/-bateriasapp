import { useMemo, useState } from 'react'
import { api, fechaHora, pesos } from '../api.js'
import { estimarPrecio } from '../precio.js'
import {
  Campo, CargaPedido, Confirmar, Dialogo, ErrorCarga, Esqueleto, ETAPAS, nombreEstado, useAccion, useCarga, Vacio,
} from '../componentes.jsx'

const METODOS = ['efectivo', 'transferencia', 'tarjeta', 'addi']
const CANCELABLES = ['cotizado', 'confirmado', 'pagado', 'en_entrega']
const FORMULARIO_VACIO = { clienteId: '', productoId: '', metodoPago: 'efectivo', entregaBateriaUsada: false }

// La acción principal de cada etapa. "en_entrega" y "entregado" se alcanzan desde Entregas,
// y "facturado" solo al generar la factura (así lo exige el backend).
const SIGUIENTE = {
  cotizado: { texto: 'Confirmar pedido', estado: 'confirmado', exito: 'confirmado' },
  confirmado: { texto: 'Registrar pago', estado: 'pagado', exito: 'pago registrado' },
  pagado: { texto: 'Asignar entrega', ir: 'entregas', conPedido: true },
  en_entrega: { texto: 'Ver entrega', ir: 'entregas' },
  entregado: { texto: 'Generar factura', factura: true },
}

const cargarTodo = async () => {
  const [pedidos, clientes, productos] = await Promise.all([
    api.get('/api/pedidos'),
    api.get('/api/clientes'),
    api.get('/api/productos'),
  ])
  return { pedidos, clientes, productos }
}

const coincide = (pedido, busqueda) => {
  const texto = busqueda.trim().toLowerCase().replace('#', '')
  return !texto || pedido.clienteNombre.toLowerCase().includes(texto) || String(pedido.id) === texto
}

export default function Pedidos({ irA }) {
  const { datos, error, recargar } = useCarga(cargarTodo)
  const { ejecutar, pendiente } = useAccion(recargar)
  const [filtro, setFiltro] = useState('todos')
  const [busqueda, setBusqueda] = useState('')
  const [nuevo, setNuevo] = useState(false)
  const [form, setForm] = useState(FORMULARIO_VACIO)
  const [cancelando, setCancelando] = useState(null)

  const conteos = useMemo(() => {
    const total = { todos: 0 }
    for (const p of datos?.pedidos ?? []) {
      total.todos += 1
      total[p.estado] = (total[p.estado] ?? 0) + 1
    }
    return total
  }, [datos])

  if (error && !datos) return <ErrorCarga mensaje={error} onReintentar={recargar} />
  if (!datos) return <Esqueleto />

  const { pedidos, clientes, productos } = datos
  const visibles = [...pedidos]
    .filter((p) => (filtro === 'todos' || p.estado === filtro) && coincide(p, busqueda))
    .sort((a, b) => b.id - a.id)
  const disponibles = productos.filter((p) => p.disponible)
  const elegido = productos.find((p) => String(p.id) === form.productoId)
  const estimado = elegido ? estimarPrecio(elegido.precioBase, form.metodoPago, form.entregaBateriaUsada) : null

  const abrirNuevo = () => {
    setForm(FORMULARIO_VACIO)
    setNuevo(true)
  }

  const crear = async (evento) => {
    evento.preventDefault()
    const ok = await ejecutar(
      () =>
        api.post('/api/pedidos', {
          clienteId: Number(form.clienteId),
          productoId: Number(form.productoId),
          metodoPago: form.metodoPago,
          entregaBateriaUsada: form.entregaBateriaUsada,
        }),
      (p) => `Pedido #${p.id} creado por ${pesos(p.precioFinal)}. Queda cotizado hasta que lo confirmes.`,
    )
    if (ok) setNuevo(false)
  }

  const avanzar = (pedido, siguiente) => {
    if (siguiente.ir) return irA(siguiente.ir, siguiente.conPedido ? pedido.id : null)
    if (siguiente.factura) {
      return ejecutar(
        () => api.post('/api/facturas', { pedidoId: pedido.id }),
        (f) => `Factura ${f.numeroFactura} generada por ${pesos(f.total)}.`,
      )
    }
    return ejecutar(
      () => api.put(`/api/pedidos/${pedido.id}/estado`, { estado: siguiente.estado }),
      `Pedido #${pedido.id} ${siguiente.exito}.`,
    )
  }

  const cancelar = async () => {
    const ok = await ejecutar(
      () => api.put(`/api/pedidos/${cancelando.id}/estado`, { estado: 'cancelado' }),
      `Pedido #${cancelando.id} cancelado.`,
    )
    if (ok) setCancelando(null)
  }

  const filtros = [
    { id: 'todos', texto: 'Todos' },
    ...ETAPAS.map((e) => ({ id: e, texto: nombreEstado(e) })),
    { id: 'cancelado', texto: 'Cancelados' },
  ]

  return (
    <>
      <div className="barra">
        <div className="filtros" role="group" aria-label="Filtrar por etapa">
          {filtros.map((f) => (
            <button key={f.id} className="chip" aria-pressed={filtro === f.id} onClick={() => setFiltro(f.id)}>
              {f.texto}
              <span className="chip-cuenta">{conteos[f.id] ?? 0}</span>
            </button>
          ))}
        </div>
        <div className="barra-acciones">
          <input
            type="search"
            className="buscar"
            placeholder="Buscar por cliente o número"
            aria-label="Buscar pedidos por cliente o número"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
          />
          <button className="boton primario" onClick={abrirNuevo}>Nuevo pedido</button>
        </div>
      </div>

      {pedidos.length === 0 ? (
        <Vacio titulo="Aún no hay pedidos" texto="Crea el primero y síguelo hasta que quede facturado.">
          <button className="boton primario" onClick={abrirNuevo}>Nuevo pedido</button>
        </Vacio>
      ) : visibles.length === 0 ? (
        <Vacio titulo="Ningún pedido coincide" texto="Prueba con otra etapa o con otro nombre.">
          <button className="boton secundario" onClick={() => { setFiltro('todos'); setBusqueda('') }}>Quitar filtros</button>
        </Vacio>
      ) : (
        <ul className="lista">
          {visibles.map((p) => {
            const siguiente = SIGUIENTE[p.estado]
            return (
              <li key={p.id} className="fila fila-pedido">
                <div className="fila-principal">
                  <p className="fila-titulo"><span className="fila-id">#{p.id}</span> {p.clienteNombre}</p>
                  <p className="fila-detalle">{p.productoNombre}</p>
                  <p className="fila-detalle suave">
                    Pago con {p.metodoPago}{p.entregaBateriaUsada ? ' y entrega batería usada' : ''}. {fechaHora(p.fechaCreacion)}
                  </p>
                </div>
                <CargaPedido estado={p.estado} />
                <p className="precio">{pesos(p.precioFinal)}</p>
                <div className="fila-acciones">
                  {siguiente && (
                    <button className="boton primario" disabled={pendiente} onClick={() => avanzar(p, siguiente)}>
                      {siguiente.texto}
                    </button>
                  )}
                  {CANCELABLES.includes(p.estado) && (
                    <button className="boton peligro" onClick={() => setCancelando(p)}>Cancelar</button>
                  )}
                </div>
              </li>
            )
          })}
        </ul>
      )}

      <Dialogo abierto={nuevo} titulo="Nuevo pedido" onCerrar={() => setNuevo(false)}>
        <form className="formulario" onSubmit={crear}>
          <Campo etiqueta="Cliente">
            <select required value={form.clienteId} onChange={(e) => setForm({ ...form, clienteId: e.target.value })}>
              <option value="">Elige un cliente</option>
              {clientes.map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
            </select>
          </Campo>
          <Campo etiqueta="Batería o producto">
            <select required value={form.productoId} onChange={(e) => setForm({ ...form, productoId: e.target.value })}>
              <option value="">Elige un producto</option>
              {disponibles.map((p) => <option key={p.id} value={p.id}>{p.nombre} — {pesos(p.precioBase)}</option>)}
            </select>
          </Campo>
          <Campo etiqueta="Método de pago">
            <select value={form.metodoPago} onChange={(e) => setForm({ ...form, metodoPago: e.target.value })}>
              {METODOS.map((m) => <option key={m} value={m}>{nombreEstado(m)}</option>)}
            </select>
          </Campo>
          <label className="casilla">
            <input
              type="checkbox"
              checked={form.entregaBateriaUsada}
              onChange={(e) => setForm({ ...form, entregaBateriaUsada: e.target.checked })}
            />
            <span>El cliente entrega una batería usada (descuento de {pesos(30000)})</span>
          </label>

          <div className="resumen" aria-live="polite">
            {estimado ? (
              <dl>
                <div><dt>Precio base</dt><dd>{pesos(elegido.precioBase)}</dd></div>
                {estimado.descuento > 0 && <div><dt>Batería usada</dt><dd>−{pesos(estimado.descuento)}</dd></div>}
                {estimado.recargo > 0 && (
                  <div><dt>Recargo {Math.round(estimado.porcentaje * 100)}%</dt><dd>{pesos(estimado.recargo)}</dd></div>
                )}
                <div className="resumen-total"><dt>Total estimado</dt><dd>{pesos(estimado.total)}</dd></div>
              </dl>
            ) : (
              <p className="suave">Elige un producto para ver el precio estimado.</p>
            )}
            <p className="nota">El servidor calcula y guarda el precio final al crear el pedido.</p>
          </div>

          <div className="dialogo-pie">
            <button type="button" className="boton secundario" onClick={() => setNuevo(false)}>Cancelar</button>
            <button type="submit" className="boton primario" disabled={pendiente}>
              {pendiente ? 'Creando…' : 'Crear pedido'}
            </button>
          </div>
        </form>
      </Dialogo>

      <Confirmar
        abierto={cancelando !== null}
        titulo={`Cancelar el pedido #${cancelando?.id ?? ''}`}
        texto={`El pedido de ${cancelando?.clienteNombre ?? ''} quedará cancelado y no se puede reactivar.`}
        textoConfirmar="Cancelar pedido"
        peligro
        pendiente={pendiente}
        onConfirmar={cancelar}
        onCerrar={() => setCancelando(null)}
      />
    </>
  )
}
