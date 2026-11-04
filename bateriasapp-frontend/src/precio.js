// Solo para mostrar un estimado en el formulario mientras se llena.
// El precio real SIEMPRE lo calcula y guarda el servidor.
const DESCUENTO_BATERIA_USADA = 30000
const RECARGOS = { efectivo: 0, transferencia: 0, tarjeta: 0.06, addi: 0.13 }

export function estimarPrecio(precioBase, metodoPago, entregaBateriaUsada) {
  const descuento = entregaBateriaUsada ? DESCUENTO_BATERIA_USADA : 0
  const subtotal = Math.max(Number(precioBase) - descuento, 0)
  const recargo = Math.round(subtotal * (RECARGOS[metodoPago] ?? 0))
  return { descuento, subtotal, porcentaje: RECARGOS[metodoPago] ?? 0, recargo, total: subtotal + recargo }
}
