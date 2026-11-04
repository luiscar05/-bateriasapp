const formatoPesos = new Intl.NumberFormat('es-CO', {
  style: 'currency',
  currency: 'COP',
  maximumFractionDigits: 0,
});

export const pesos = (valor) => (valor == null ? '—' : formatoPesos.format(valor));

export const fechaHora = (iso) =>
  iso ? new Date(iso).toLocaleString('es-CO', { dateStyle: 'short', timeStyle: 'short' }) : '—';

export const etiqueta = (valor) => (valor ? valor.replaceAll('_', ' ') : '—');

/** Fecha local en formato YYYY-MM-DD, sin desfase por zona horaria. */
export const aFechaIso = (fecha) =>
  `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}-${String(fecha.getDate()).padStart(2, '0')}`;
