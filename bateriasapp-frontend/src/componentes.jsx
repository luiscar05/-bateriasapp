import { createContext, useCallback, useContext, useEffect, useId, useRef, useState } from 'react'

export const ETAPAS = ['cotizado', 'confirmado', 'pagado', 'en_entrega', 'entregado', 'facturado']

const NOMBRES = { pendiente_evaluacion: 'Pendiente de evaluación' }

export function nombreEstado(valor) {
  const texto = NOMBRES[valor] ?? (valor ?? '').replaceAll('_', ' ')
  return texto.charAt(0).toUpperCase() + texto.slice(1)
}

/* ---------------------------------------------------------------- Avisos */

const ContextoAvisos = createContext(() => {})
export const useAvisos = () => useContext(ContextoAvisos)

/** Los éxitos se van solos a los 5 s; los errores se quedan hasta que la persona los cierre. */
export function ProveedorAvisos({ children }) {
  const [avisos, setAvisos] = useState([])
  const siguiente = useRef(1)

  const quitar = useCallback((id) => setAvisos((actuales) => actuales.filter((a) => a.id !== id)), [])
  const avisar = useCallback(
    (tipo, texto) => {
      const id = siguiente.current++
      setAvisos((actuales) => [...actuales, { id, tipo, texto }])
      if (tipo === 'ok') setTimeout(() => quitar(id), 5000)
    },
    [quitar],
  )

  return (
    <ContextoAvisos.Provider value={avisar}>
      {children}
      <div className="avisos" aria-live="polite">
        {avisos.map((a) => (
          <div key={a.id} className={`aviso aviso-${a.tipo}`} role={a.tipo === 'error' ? 'alert' : 'status'}>
            <p>{a.texto}</p>
            <button className="cerrar" onClick={() => quitar(a.id)} aria-label="Cerrar aviso">
              <IconoCerrar />
            </button>
          </div>
        ))}
      </div>
    </ContextoAvisos.Provider>
  )
}

/* ----------------------------------------------------------------- Datos */

/** Carga al montar y expone recargar(). El cargador debe ser una función estable (de módulo). */
export function useCarga(cargador) {
  const [datos, setDatos] = useState(null)
  const [error, setError] = useState('')

  const recargar = useCallback(async () => {
    try {
      setDatos(await cargador())
      setError('')
    } catch (e) {
      setError(e.message)
    }
  }, [cargador])

  useEffect(() => {
    recargar()
  }, [recargar])

  return { datos, error, recargar }
}

/** Ejecuta una acción contra la API: bloquea los botones mientras corre y avisa el resultado. */
export function useAccion(recargar) {
  const avisar = useAvisos()
  const [pendiente, setPendiente] = useState(false)

  const ejecutar = useCallback(
    async (accion, textoExito) => {
      setPendiente(true)
      try {
        const resultado = await accion()
        avisar('ok', typeof textoExito === 'function' ? textoExito(resultado) : textoExito)
        await recargar()
        return true
      } catch (e) {
        avisar('error', e.message)
        return false
      } finally {
        setPendiente(false)
      }
    },
    [avisar, recargar],
  )

  return { ejecutar, pendiente }
}

/* ------------------------------------------------------------- Diálogos */

/**
 * Usa el <dialog> nativo: el navegador atrapa el foco, cierra con Esc y devuelve el foco
 * al botón que lo abrió. Lo que lleve data-enfocar recibe el foco al abrir; si no hay,
 * el primer campo del formulario.
 */
export function Dialogo({ abierto, titulo, onCerrar, children }) {
  const ref = useRef(null)
  const idTitulo = useId()

  useEffect(() => {
    const dialogo = ref.current
    if (!dialogo) return
    if (abierto && !dialogo.open) {
      dialogo.showModal()
      const objetivo =
        dialogo.querySelector('[data-enfocar]') ?? dialogo.querySelector('input:not([type=hidden]), select, textarea')
      objetivo?.focus()
    }
    if (!abierto && dialogo.open) dialogo.close()
  }, [abierto])

  return (
    <dialog
      ref={ref}
      className="dialogo"
      aria-labelledby={idTitulo}
      onClose={onCerrar}
      onClick={(evento) => {
        if (evento.target === evento.currentTarget) onCerrar()
      }}
    >
      {abierto && (
        <div className="dialogo-cuerpo">
          <header className="dialogo-cabecera">
            <h2 id={idTitulo}>{titulo}</h2>
            <button className="cerrar" onClick={onCerrar} aria-label="Cerrar">
              <IconoCerrar />
            </button>
          </header>
          {children}
        </div>
      )}
    </dialog>
  )
}

export function Confirmar({ abierto, titulo, texto, textoConfirmar, peligro = false, pendiente = false, onConfirmar, onCerrar }) {
  return (
    <Dialogo abierto={abierto} titulo={titulo} onCerrar={onCerrar}>
      <p className="dialogo-texto">{texto}</p>
      <div className="dialogo-pie">
        <button className="boton secundario" data-enfocar onClick={onCerrar}>
          Volver
        </button>
        <button className={`boton ${peligro ? 'peligro-lleno' : 'primario'}`} onClick={onConfirmar} disabled={pendiente}>
          {pendiente ? 'Procesando…' : textoConfirmar}
        </button>
      </div>
    </Dialogo>
  )
}

/* ------------------------------------------------------------ Elementos */

export function Campo({ etiqueta, ayuda, children }) {
  return (
    <label className="campo">
      <span className="campo-etiqueta">{etiqueta}</span>
      {children}
      {ayuda && <span className="campo-ayuda">{ayuda}</span>}
    </label>
  )
}

export function Insignia({ valor }) {
  return <span className={`insignia insignia-${valor}`}>{nombreEstado(valor)}</span>
}

/**
 * La barra de carga: una celda por etapa del pedido, como el indicador de una batería.
 * Con el pedido entregado o facturado queda completa y en verde.
 */
export function CargaPedido({ estado }) {
  const cancelado = estado === 'cancelado'
  const posicion = ETAPAS.indexOf(estado)
  const completa = estado === 'entregado' || estado === 'facturado'
  const descripcion = cancelado ? 'Pedido cancelado' : `Etapa ${posicion + 1} de ${ETAPAS.length}: ${nombreEstado(estado)}`

  return (
    <div className={`carga${completa ? ' carga-completa' : ''}${cancelado ? ' carga-cancelada' : ''}`}>
      <span className="celdas" role="img" aria-label={descripcion}>
        {ETAPAS.map((etapa, i) => (
          <i key={etapa} className={!cancelado && i <= posicion ? 'llena' : ''} />
        ))}
      </span>
      <span className="carga-texto">{nombreEstado(estado)}</span>
    </div>
  )
}

export function Vacio({ titulo, texto, children }) {
  return (
    <div className="vacio">
      <p className="vacio-titulo">{titulo}</p>
      <p>{texto}</p>
      {children}
    </div>
  )
}

export function ErrorCarga({ mensaje, onReintentar }) {
  return (
    <div className="vacio" role="alert">
      <p className="vacio-titulo">No se pudieron cargar los datos</p>
      <p>{mensaje}</p>
      <button className="boton secundario" onClick={onReintentar}>
        Reintentar
      </button>
    </div>
  )
}

export function Esqueleto() {
  return (
    <div className="lista" aria-busy="true" aria-label="Cargando">
      {[0, 1, 2].map((i) => (
        <div key={i} className="fila esqueleto" />
      ))}
    </div>
  )
}

export function IconoCerrar() {
  return (
    <svg width="16" height="16" viewBox="0 0 16 16" aria-hidden="true">
      <path d="M3.5 3.5l9 9m0-9l-9 9" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" fill="none" />
    </svg>
  )
}

export function IconoBateria() {
  return (
    <svg width="30" height="20" viewBox="0 0 30 20" aria-hidden="true">
      <rect x="1" y="2" width="24" height="16" rx="3.5" fill="none" stroke="currentColor" strokeWidth="2" />
      <rect x="26" y="7" width="3" height="6" rx="1" fill="currentColor" />
      <rect x="5" y="6" width="4" height="8" rx="1" fill="var(--carga)" />
      <rect x="11" y="6" width="4" height="8" rx="1" fill="var(--carga)" />
      <rect x="17" y="6" width="4" height="8" rx="1" fill="var(--carga)" />
    </svg>
  )
}
