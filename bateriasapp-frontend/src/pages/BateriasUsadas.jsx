import { useState } from 'react'
import { api, fechaHora } from '../api.js'
import { Campo, Confirmar, Dialogo, ErrorCarga, Esqueleto, Insignia, nombreEstado, useAccion, useCarga, Vacio } from '../componentes.jsx'

const cargarTodo = async () => {
  const [baterias, pedidos] = await Promise.all([api.get('/api/baterias-usadas'), api.get('/api/pedidos')])
  return { baterias, pedidos }
}
const REGISTRO_VACIO = { pedidoId: '', descripcion: '' }

export default function BateriasUsadas() {
  const { datos, error, recargar } = useCarga(cargarTodo)
  const { ejecutar, pendiente } = useAccion(recargar)
  const [registrando, setRegistrando] = useState(false)
  const [registro, setRegistro] = useState(REGISTRO_VACIO)
  const [evaluando, setEvaluando] = useState(null) // { bateria, resultado, destino, observaciones }
  const [eliminando, setEliminando] = useState(null)

  if (error && !datos) return <ErrorCarga mensaje={error} onReintentar={recargar} />
  if (!datos) return <Esqueleto />

  const { baterias, pedidos } = datos
  const pedidoPorId = new Map(pedidos.map((p) => [p.id, p]))
  const yaRegistrados = new Set(baterias.map((b) => b.pedidoId))
  const elegibles = pedidos.filter(
    (p) => p.entregaBateriaUsada && ['en_entrega', 'entregado', 'facturado'].includes(p.estado) && !yaRegistrados.has(p.id),
  )
  const pendientes = baterias.filter((b) => b.estadoEvaluacion === 'pendiente_evaluacion').length
  const ordenadas = [...baterias].sort((a, b) => b.id - a.id)

  const abrirRegistro = () => {
    setRegistro(REGISTRO_VACIO)
    setRegistrando(true)
  }

  const registrar = async (evento) => {
    evento.preventDefault()
    const ok = await ejecutar(
      () => api.post('/api/baterias-usadas', { pedidoId: Number(registro.pedidoId), descripcion: registro.descripcion.trim() || null }),
      `Batería del pedido #${registro.pedidoId} registrada. Queda pendiente de evaluación en bodega.`,
    )
    if (ok) setRegistrando(false)
  }

  const evaluar = async (evento) => {
    evento.preventDefault()
    const aceptada = evaluando.resultado === 'aceptada'
    const ok = await ejecutar(
      () =>
        api.put(`/api/baterias-usadas/${evaluando.bateria.id}/evaluacion`, {
          estadoEvaluacion: evaluando.resultado,
          destino: aceptada ? evaluando.destino : null,
          observaciones: evaluando.observaciones.trim() || null,
        }),
      aceptada
        ? `Batería aceptada con destino ${nombreEstado(evaluando.destino).toLowerCase()}.`
        : 'Batería rechazada y registrada.',
    )
    if (ok) setEvaluando(null)
  }

  const eliminar = async () => {
    const ok = await ejecutar(() => api.del(`/api/baterias-usadas/${eliminando.id}`), `Registro #${eliminando.id} eliminado.`)
    if (ok) setEliminando(null)
  }

  return (
    <>
      <div className="barra">
        <p className="suave">
          {pendientes === 0 ? 'No hay baterías pendientes de evaluación.' : `${pendientes} ${pendientes === 1 ? 'batería pendiente' : 'baterías pendientes'} de evaluación.`}
        </p>
        <div className="barra-acciones">
          <button className="boton primario" onClick={abrirRegistro}>Registrar batería recibida</button>
        </div>
      </div>

      {ordenadas.length === 0 ? (
        <Vacio titulo="Aún no hay baterías usadas" texto="Se registran cuando un pedido con batería usada ya salió a entrega.">
          <button className="boton primario" onClick={abrirRegistro}>Registrar batería recibida</button>
        </Vacio>
      ) : (
        <ul className="lista">
          {ordenadas.map((b) => {
            const pedido = pedidoPorId.get(b.pedidoId)
            const pendienteEval = b.estadoEvaluacion === 'pendiente_evaluacion'
            return (
              <li key={b.id} className="fila fila-bateria">
                <div className="fila-principal">
                  <p className="fila-titulo"><span className="fila-id">Pedido #{b.pedidoId}</span> {pedido?.clienteNombre ?? ''}</p>
                  <p className="fila-detalle">{b.descripcion ?? 'Sin descripción'}</p>
                  <p className="fila-detalle suave">
                    Recibida el {fechaHora(b.fechaRecepcion)}.
                    {b.destino ? ` Destino: ${nombreEstado(b.destino).toLowerCase()}.` : ''}
                    {b.observaciones ? ` ${b.observaciones}` : ''}
                  </p>
                </div>
                <div><Insignia valor={b.estadoEvaluacion} /></div>
                <div className="fila-acciones">
                  {pendienteEval && (
                    <>
                      <button className="boton primario" onClick={() => setEvaluando({ bateria: b, resultado: 'aceptada', destino: 'lote_mensual', observaciones: '' })}>
                        Evaluar
                      </button>
                      <button className="boton peligro" onClick={() => setEliminando(b)}>Eliminar</button>
                    </>
                  )}
                </div>
              </li>
            )
          })}
        </ul>
      )}

      <Dialogo abierto={registrando} titulo="Registrar batería recibida" onCerrar={() => setRegistrando(false)}>
        <form className="formulario" onSubmit={registrar}>
          <Campo
            etiqueta="Pedido"
            ayuda={elegibles.length === 0 ? 'Solo aparecen pedidos con batería usada que ya salieron a entrega y no tienen registro.' : undefined}
          >
            <select required value={registro.pedidoId} onChange={(e) => setRegistro({ ...registro, pedidoId: e.target.value })}>
              <option value="">Elige un pedido</option>
              {elegibles.map((p) => <option key={p.id} value={p.id}>#{p.id} {p.clienteNombre} — {nombreEstado(p.estado).toLowerCase()}</option>)}
            </select>
          </Campo>
          <Campo etiqueta="Descripción" ayuda="Marca, amperaje o estado visible. Es opcional.">
            <input value={registro.descripcion} maxLength={200} onChange={(e) => setRegistro({ ...registro, descripcion: e.target.value })} />
          </Campo>
          <div className="dialogo-pie">
            <button type="button" className="boton secundario" onClick={() => setRegistrando(false)}>Cancelar</button>
            <button type="submit" className="boton primario" disabled={pendiente}>{pendiente ? 'Registrando…' : 'Registrar'}</button>
          </div>
        </form>
      </Dialogo>

      <Dialogo abierto={evaluando !== null} titulo={`Evaluar batería del pedido #${evaluando?.bateria.pedidoId ?? ''}`} onCerrar={() => setEvaluando(null)}>
        {evaluando && (
          <form className="formulario" onSubmit={evaluar}>
            <Campo etiqueta="Resultado de la evaluación">
              <select value={evaluando.resultado} onChange={(e) => setEvaluando({ ...evaluando, resultado: e.target.value })}>
                <option value="aceptada">Aceptada</option>
                <option value="rechazada">Rechazada</option>
              </select>
            </Campo>
            {evaluando.resultado === 'aceptada' && (
              <Campo etiqueta="Qué se hará con ella">
                <select value={evaluando.destino} onChange={(e) => setEvaluando({ ...evaluando, destino: e.target.value })}>
                  <option value="lote_mensual">Venderla en el lote mensual</option>
                  <option value="reventa">Revenderla como usada</option>
                </select>
              </Campo>
            )}
            <Campo etiqueta="Observaciones" ayuda="Opcional.">
              <input value={evaluando.observaciones} maxLength={255} onChange={(e) => setEvaluando({ ...evaluando, observaciones: e.target.value })} />
            </Campo>
            <p className="nota">La evaluación se hace una sola vez: después no se puede cambiar.</p>
            <div className="dialogo-pie">
              <button type="button" className="boton secundario" onClick={() => setEvaluando(null)}>Cancelar</button>
              <button type="submit" className="boton primario" disabled={pendiente}>{pendiente ? 'Guardando…' : 'Guardar evaluación'}</button>
            </div>
          </form>
        )}
      </Dialogo>

      <Confirmar
        abierto={eliminando !== null}
        titulo="Eliminar registro de batería"
        texto={`Se eliminará el registro de la batería del pedido #${eliminando?.pedidoId ?? ''}. Podrás registrarla de nuevo si hace falta.`}
        textoConfirmar="Eliminar registro"
        peligro
        pendiente={pendiente}
        onConfirmar={eliminar}
        onCerrar={() => setEliminando(null)}
      />
    </>
  )
}
