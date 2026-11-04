import { useEffect, useState } from 'react'
import { api, fechaHora, pesos } from '../api.js'
import { Campo, Confirmar, Dialogo, ErrorCarga, Esqueleto, Insignia, useAccion, useCarga, Vacio } from '../componentes.jsx'

const cargarTodo = async () => {
  const [entregas, pedidos, domiciliarios] = await Promise.all([
    api.get('/api/entregas'),
    api.get('/api/pedidos'),
    api.get('/api/domiciliarios'),
  ])
  return { entregas, pedidos, domiciliarios }
}

/** Valor para <input type="datetime-local">: dentro de una hora, en la zona del navegador. */
function proximaHora() {
  const fecha = new Date(Date.now() + 60 * 60 * 1000)
  return new Date(fecha - fecha.getTimezoneOffset() * 60000).toISOString().slice(0, 16)
}
const formularioNuevo = (pedidoId = '') => ({
  pedidoId: String(pedidoId),
  domiciliarioId: '',
  direccion: '',
  fechaEntrega: proximaHora(),
})

export default function Entregas({ pedidoInicial, alConsumir }) {
  const { datos, error, recargar } = useCarga(cargarTodo)
  const { ejecutar, pendiente } = useAccion(recargar)
  const [nueva, setNueva] = useState(false)
  const [form, setForm] = useState(formularioNuevo)
  const [entregando, setEntregando] = useState(null)
  const [instalada, setInstalada] = useState(false)
  const [fallando, setFallando] = useState(null)

  // Llegar desde "Asignar entrega" en Pedidos: abre el formulario con ese pedido ya elegido.
  useEffect(() => {
    if (pedidoInicial && datos) {
      setForm(formularioNuevo(pedidoInicial))
      setNueva(true)
      alConsumir()
    }
  }, [pedidoInicial, datos, alConsumir])

  if (error && !datos) return <ErrorCarga mensaje={error} onReintentar={recargar} />
  if (!datos) return <Esqueleto />

  const { entregas, pedidos, domiciliarios } = datos
  const pedidoPorId = new Map(pedidos.map((p) => [p.id, p]))
  const ocupados = new Set(entregas.filter((e) => e.estado !== 'fallida').map((e) => e.pedidoId))
  const asignables = pedidos.filter((p) => (p.estado === 'pagado' || p.estado === 'en_entrega') && !ocupados.has(p.id))
  const ordenadas = [...entregas].sort((a, b) => b.id - a.id)

  const crear = async (evento) => {
    evento.preventDefault()
    const ok = await ejecutar(
      () =>
        api.post('/api/entregas', {
          pedidoId: Number(form.pedidoId),
          domiciliarioId: Number(form.domiciliarioId),
          direccion: form.direccion.trim() || null,
          fechaEntrega: `${form.fechaEntrega}:00`,
        }),
      `Entrega asignada al pedido #${form.pedidoId}. El pedido pasó a «En entrega».`,
    )
    if (ok) setNueva(false)
  }

  const cambiar = (entrega, estado, texto, extra = {}) =>
    ejecutar(() => api.put(`/api/entregas/${entrega.id}/estado`, { estado, ...extra }), texto)

  const confirmarEntrega = async (evento) => {
    evento.preventDefault()
    const ok = await cambiar(entregando, 'entregada', `Entrega confirmada. El pedido #${entregando.pedidoId} quedó entregado y ya se puede facturar.`, { instalada })
    if (ok) setEntregando(null)
  }

  const marcarFallida = async () => {
    const ok = await cambiar(fallando, 'fallida', `Entrega #${fallando.id} marcada como fallida. Puedes asignar otro domiciliario.`)
    if (ok) setFallando(null)
  }

  return (
    <>
      <div className="barra">
        <p className="suave">{entregas.length} {entregas.length === 1 ? 'entrega registrada' : 'entregas registradas'}</p>
        <div className="barra-acciones">
          <button className="boton primario" onClick={() => { setForm(formularioNuevo()); setNueva(true) }}>Asignar entrega</button>
        </div>
      </div>

      {ordenadas.length === 0 ? (
        <Vacio titulo="Aún no hay entregas" texto="Cuando un pedido esté pagado, asígnale un domiciliario desde aquí.">
          <button className="boton primario" onClick={() => { setForm(formularioNuevo()); setNueva(true) }}>Asignar entrega</button>
        </Vacio>
      ) : (
        <ul className="lista">
          {ordenadas.map((e) => {
            const pedido = pedidoPorId.get(e.pedidoId)
            return (
              <li key={e.id} className="fila fila-entrega">
                <div className="fila-principal">
                  <p className="fila-titulo">
                    <span className="fila-id">Pedido #{e.pedidoId}</span> {pedido?.clienteNombre ?? ''}
                  </p>
                  <p className="fila-detalle">{e.direccion}</p>
                  <p className="fila-detalle suave">
                    Lo lleva {e.domiciliarioNombre}. {fechaHora(e.fechaEntrega)}.{e.estado === 'entregada' ? (e.instalada ? ' Batería instalada.' : ' Sin instalación.') : ''}
                  </p>
                </div>
                <div><Insignia valor={e.estado} /></div>
                <div className="fila-acciones">
                  {e.estado === 'asignada' && (
                    <button className="boton primario" disabled={pendiente} onClick={() => cambiar(e, 'en_camino', `El domiciliario salió con el pedido #${e.pedidoId}.`)}>
                      Salió a entregar
                    </button>
                  )}
                  {e.estado === 'en_camino' && (
                    <button className="boton primario" onClick={() => { setInstalada(false); setEntregando(e) }}>Confirmar entrega</button>
                  )}
                  {(e.estado === 'asignada' || e.estado === 'en_camino') && (
                    <button className="boton peligro" onClick={() => setFallando(e)}>No se pudo entregar</button>
                  )}
                </div>
              </li>
            )
          })}
        </ul>
      )}

      <Dialogo abierto={nueva} titulo="Asignar entrega" onCerrar={() => setNueva(false)}>
        <form className="formulario" onSubmit={crear}>
          <Campo etiqueta="Pedido" ayuda={asignables.length === 0 ? 'No hay pedidos pagados sin entrega. Registra el pago en Pedidos primero.' : undefined}>
            <select required value={form.pedidoId} onChange={(e) => setForm({ ...form, pedidoId: e.target.value })}>
              <option value="">Elige un pedido</option>
              {asignables.map((p) => <option key={p.id} value={p.id}>#{p.id} {p.clienteNombre} — {pesos(p.precioFinal)}</option>)}
            </select>
          </Campo>
          <Campo etiqueta="Domiciliario">
            <select required value={form.domiciliarioId} onChange={(e) => setForm({ ...form, domiciliarioId: e.target.value })}>
              <option value="">Elige un domiciliario</option>
              {domiciliarios.map((d) => <option key={d.id} value={d.id}>{d.nombre}</option>)}
            </select>
          </Campo>
          <Campo etiqueta="Dirección de entrega" ayuda="Si la dejas vacía se usa la dirección registrada del cliente.">
            <input value={form.direccion} maxLength={200} onChange={(e) => setForm({ ...form, direccion: e.target.value })} />
          </Campo>
          <Campo etiqueta="Fecha y hora">
            <input required type="datetime-local" value={form.fechaEntrega} onChange={(e) => setForm({ ...form, fechaEntrega: e.target.value })} />
          </Campo>
          <div className="dialogo-pie">
            <button type="button" className="boton secundario" onClick={() => setNueva(false)}>Cancelar</button>
            <button type="submit" className="boton primario" disabled={pendiente}>{pendiente ? 'Asignando…' : 'Asignar entrega'}</button>
          </div>
        </form>
      </Dialogo>

      <Dialogo abierto={entregando !== null} titulo="Confirmar entrega" onCerrar={() => setEntregando(null)}>
        <form className="formulario" onSubmit={confirmarEntrega}>
          <p className="dialogo-texto">
            El pedido #{entregando?.pedidoId} quedará entregado y se podrá facturar.
          </p>
          <label className="casilla">
            <input type="checkbox" checked={instalada} onChange={(e) => setInstalada(e.target.checked)} />
            <span>El domiciliario instaló la batería</span>
          </label>
          <div className="dialogo-pie">
            <button type="button" className="boton secundario" onClick={() => setEntregando(null)}>Volver</button>
            <button type="submit" className="boton primario" disabled={pendiente}>{pendiente ? 'Guardando…' : 'Confirmar entrega'}</button>
          </div>
        </form>
      </Dialogo>

      <Confirmar
        abierto={fallando !== null}
        titulo="Marcar entrega como fallida"
        texto={`La entrega del pedido #${fallando?.pedidoId ?? ''} quedará fallida. El pedido sigue en entrega y podrás asignar otro domiciliario.`}
        textoConfirmar="Marcar como fallida"
        peligro
        pendiente={pendiente}
        onConfirmar={marcarFallida}
        onCerrar={() => setFallando(null)}
      />
    </>
  )
}
