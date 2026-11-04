// Cliente HTTP mínimo. El backend responde todos los errores con el mismo JSON
// (mensaje + detalles), así que aquí se convierten en un Error con texto legible.
async function pedir(metodo, ruta, cuerpo) {
  let respuesta
  try {
    respuesta = await fetch(ruta, {
      method: metodo,
      headers: cuerpo ? { 'Content-Type': 'application/json' } : undefined,
      body: cuerpo ? JSON.stringify(cuerpo) : undefined,
    })
  } catch {
    throw new Error('No se pudo conectar con el servidor. Revisa que el backend esté encendido en el puerto 8080.')
  }
  if (respuesta.status === 204) return null

  let datos = null
  try {
    datos = await respuesta.json()
  } catch {
    // respuesta sin JSON
  }
  if (!respuesta.ok) {
    const detalles = datos?.detalles?.length ? ` (${datos.detalles.join('; ')})` : ''
    throw new Error((datos?.mensaje ?? `El servidor respondió con error ${respuesta.status} en ${ruta}`) + detalles)
  }
  return datos
}

export const api = {
  get: (ruta) => pedir('GET', ruta),
  post: (ruta, cuerpo) => pedir('POST', ruta, cuerpo),
  put: (ruta, cuerpo) => pedir('PUT', ruta, cuerpo),
  del: (ruta) => pedir('DELETE', ruta),
}

const formatoPesos = new Intl.NumberFormat('es-CO', {
  style: 'currency',
  currency: 'COP',
  maximumFractionDigits: 0,
})
export const pesos = (valor) => formatoPesos.format(valor ?? 0)

export const fechaHora = (valor) =>
  valor ? new Date(valor).toLocaleString('es-CO', { dateStyle: 'medium', timeStyle: 'short' }) : 'Sin fecha'
