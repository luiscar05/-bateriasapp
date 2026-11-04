import { useState } from 'react'
import { api, pesos } from '../api.js'
import { Campo, ErrorCarga, Esqueleto, useAvisos, useCarga, Vacio } from '../componentes.jsx'

const cargarVehiculos = () => api.get('/api/vehiculos')

export default function Sugerencias() {
  const avisar = useAvisos()
  const { datos: vehiculos, error, recargar } = useCarga(cargarVehiculos)
  const [vehiculoId, setVehiculoId] = useState('')
  const [resultado, setResultado] = useState(null)
  const [consultando, setConsultando] = useState(false)

  if (error && !vehiculos) return <ErrorCarga mensaje={error} onReintentar={recargar} />
  if (!vehiculos) return <Esqueleto />

  const elegir = async (id) => {
    setVehiculoId(id)
    setResultado(null)
    if (!id) return
    setConsultando(true)
    try {
      setResultado(await api.get(`/api/vehiculos/${id}/baterias-sugeridas`))
    } catch (e) {
      avisar('error', e.message)
    } finally {
      setConsultando(false)
    }
  }

  return (
    <>
      <div className="barra barra-simple">
        <Campo etiqueta="Vehículo">
          <select value={vehiculoId} onChange={(e) => elegir(e.target.value)}>
            <option value="">Elige un vehículo</option>
            {vehiculos.map((v) => <option key={v.id} value={v.id}>{v.marca} {v.modelo} {v.anio}</option>)}
          </select>
        </Campo>
      </div>

      {vehiculos.length === 0 && (
        <Vacio titulo="No hay vehículos de referencia" texto="Agrégalos en la tabla de vehículos para poder consultar sugerencias." />
      )}
      {consultando && <Esqueleto />}

      {resultado && (
        <>
          <div className="aviso-fijo">
            <p>{resultado.aviso}</p>
            <p className="suave">
              Referencia del vehículo: {resultado.amperajeRecomendado ?? 'sin dato'} Ah y {resultado.ccaRecomendado ?? 'sin dato'} CCA.
            </p>
          </div>
          {resultado.sugerencias.length === 0 ? (
            <Vacio titulo="No hay baterías compatibles disponibles" texto="Revisa el inventario o elige la batería a mano al crear el pedido." />
          ) : (
            <ul className="lista">
              {resultado.sugerencias.map((b) => (
                <li key={b.id} className="fila fila-sugerencia">
                  <div className="fila-principal">
                    <p className="fila-titulo">{b.nombre}</p>
                    <p className="fila-detalle suave">
                      {b.amperaje} Ah, {b.cca} CCA. Caja {b.tipoCaja ?? 'sin dato'}, polaridad {b.polaridad ?? 'sin dato'}. Condición: {b.condicion}.
                    </p>
                  </div>
                  <p className="precio">{pesos(b.precioBase)}</p>
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </>
  )
}
