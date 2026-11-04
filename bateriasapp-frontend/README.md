# BateriasApp — Frontend (React + Vite)

Interfaz para operar la tienda: pedidos, entregas, baterías usadas y sugerencias por vehículo.

## Requisitos
- Node.js 20 o superior
- El backend corriendo en http://localhost:8080

## Cómo correrlo
```bash
npm install
npm run dev
```
Abre http://localhost:5173. Vite reenvía `/api` al backend (`vite.config.js`), así que no hace falta CORS.

## Qué hace cada pantalla
- **Pedidos:** cada pedido muestra su etapa como una barra de carga de seis celdas y una sola acción principal (la siguiente). Filtra por etapa, busca por cliente o número, y crea pedidos con un precio estimado visible; el servidor calcula el definitivo.
- **Entregas:** asigna un domiciliario (también desde «Asignar entrega» en un pedido pagado), marca que salió, confirma la entrega con o sin instalación, o márcala como fallida.
- **Baterías usadas:** registra la batería recibida y evalúala en bodega (aceptada con destino, o rechazada).
- **Sugerencias:** elige un vehículo y mira las baterías compatibles. Solo sugiere.

## Estructura
- `src/App.jsx`: cabecera, navegación y sección activa.
- `src/componentes.jsx`: diálogos, avisos, barra de carga y hooks compartidos.
- `src/pages/`: una pantalla por archivo.
- `src/api.js` y `src/precio.js`: acceso a la API y estimado de precio.
