# BateriasApp

Backend de ventas para una tienda de baterías de vehículos en Bogotá. Las ventas llegan por WhatsApp y se entregan a domicilio con domiciliarios. La API cubre pedidos con precio calculado en el servidor, entregas, baterías usadas recibidas, facturación consecutiva, sugerencia de baterías por vehículo y reportes.

## Reglas de negocio clave

- **Subtotal** = precio base − 30.000 COP si el cliente entrega una batería usada (nunca menor a 0).
- **Recargo** sobre el subtotal: efectivo 0 %, transferencia 0 %, tarjeta 6 %, Addi 13 %.
- **Precio final** = subtotal + recargo, con `BigDecimal` y `setScale(0, RoundingMode.HALF_UP)`.
  Caso de referencia: base 250.000 + Addi + batería usada = **248.600**.
- El precio **siempre** lo calcula el servidor; ningún endpoint lo acepta del cliente.
- La API **sugiere** baterías para un vehículo, pero una persona confirma: nunca crea pedidos sola.
- Flujo del pedido: `cotizado → confirmado → pagado → en_entrega → entregado → facturado`. Se puede `cancelar` antes de `entregado`.

## Requisitos

- Java 21
- Maven 3.9+
- MySQL 8.0
- Docker (opcional, solo para el contenedor)

## Configurar la base de datos

```bash
mysql -u root -p -e "CREATE DATABASE bateriasapp CHARACTER SET utf8mb4;"
mysql -u root -p bateriasapp < db/schema.sql
mysql -u root -p bateriasapp < db/migraciones.sql
```

Si ya tenías la base creada con tu `schema.sql`, ejecuta solo `db/migraciones.sql` **una vez**. Hibernate corre con `ddl-auto=validate`: si falta una columna o tabla, la app no arranca y te dice cuál.

## Variables de entorno

Copia `.env.example` y define las tres variables. Spring Boot **no** lee archivos `.env` por sí solo.

| Variable | Descripción | Por defecto |
|---|---|---|
| `DB_URL` | URL JDBC | `jdbc:mysql://localhost:3306/bateriasapp` |
| `DB_USER` | Usuario de MySQL | (obligatoria) |
| `DB_PASSWORD` | Clave de MySQL | (obligatoria) |

En IntelliJ: *Run → Edit Configurations → Environment variables*. En terminal:

```bash
export DB_USER=root
export DB_PASSWORD=tu_clave
```

## Compilar, probar y correr

```bash
mvn clean compile          # compila
mvn test                   # corre los tests (no necesitan MySQL)
mvn spring-boot:run        # levanta la API en http://localhost:8080
```

Los tests unitarios usan Mockito. Los de integración usan MockMvc con el controller, la validación, el servicio y el manejo de errores reales, y repositorios simulados; por eso corren sin base de datos.

### Docker

```bash
docker build -t bateriasapp .
docker run -p 8080:8080 \
  -e DB_URL=jdbc:mysql://host.docker.internal:3306/bateriasapp \
  -e DB_USER=root -e DB_PASSWORD=tu_clave bateriasapp
```

## Probar con Postman

Importa `postman/bateriasapp.postman_collection.json`. La variable `baseUrl` apunta a `http://localhost:8080`. Los ejemplos suponen que ya existen un cliente, un producto (`precio_base` 250000) y un domiciliario con id 1. La carpeta "Pedidos" incluye el caso de referencia y ejemplos de error 400, 404 y 409.

Flujo de punta a punta: crear pedido → `confirmado` → `pagado` → asignar entrega (el pedido pasa solo a `en_entrega`) → entrega `en_camino` → `entregada` (el pedido pasa solo a `entregado`) → registrar batería usada → evaluarla → generar factura (el pedido pasa solo a `facturado`).

## Endpoints

CRUD existente (sin cambios): `/api/productos`, `/api/clientes`, `/api/domiciliarios`, `/api/vehiculos`.

| Método | Ruta | Descripción | Éxito |
|---|---|---|---|
| GET | `/api/pedidos` | Lista pedidos | 200 |
| GET | `/api/pedidos/{id}` | Obtiene un pedido | 200 |
| POST | `/api/pedidos` | Crea pedido (queda `cotizado`) | 201 |
| PUT | `/api/pedidos/{id}` | Cambia producto, método de pago o batería usada y recalcula el precio | 200 |
| PUT | `/api/pedidos/{id}/estado` | Cambia el estado validando la transición | 200 |
| DELETE | `/api/pedidos/{id}` | Elimina un pedido | 204 |
| GET | `/api/entregas` · `/api/entregas/{id}` | Consulta entregas | 200 |
| POST | `/api/entregas` | Asigna domiciliario a un pedido pagado | 201 |
| PUT | `/api/entregas/{id}` | Corrige domiciliario, dirección u hora | 200 |
| PUT | `/api/entregas/{id}/estado` | `en_camino`, `entregada` (con `instalada`) o `fallida` | 200 |
| DELETE | `/api/entregas/{id}` | Elimina una entrega `asignada` | 204 |
| GET | `/api/baterias-usadas` · `/api/baterias-usadas/{id}` | Consulta baterías usadas | 200 |
| POST | `/api/baterias-usadas` | Registra la batería recibida de un pedido | 201 |
| PUT | `/api/baterias-usadas/{id}/evaluacion` | Evalúa en bodega: aceptada (con destino) o rechazada | 200 |
| DELETE | `/api/baterias-usadas/{id}` | Elimina una batería aún pendiente | 204 |
| GET | `/api/facturas` · `/api/facturas/{id}` · `/api/facturas/pedido/{pedidoId}` | Consulta facturas | 200 |
| POST | `/api/facturas` | Genera la factura de un pedido entregado | 201 |
| GET | `/api/vehiculos/{id}/baterias-sugeridas` | Sugiere baterías compatibles (solo sugiere) | 200 |
| GET | `/api/reportes/ventas?desde=YYYY-MM-DD&hasta=YYYY-MM-DD` | Ventas por rango y método de pago | 200 |
| GET | `/api/reportes/baterias-usadas-pendientes` | Baterías pendientes de evaluación | 200 |
| GET | `/api/reportes/pedidos-por-estado` | Conteo de pedidos por estado | 200 |

### Errores

Todos los errores tienen el mismo formato:

```json
{
  "timestamp": "2026-10-27T15:40:12.123",
  "status": 409,
  "error": "Conflict",
  "mensaje": "Transición no permitida: cotizado -> pagado",
  "detalles": [],
  "ruta": "/api/pedidos/5/estado"
}
```

- **400**: validación (`detalles` lista cada campo), JSON mal formado, parámetro faltante o fechas incoherentes.
- **404**: el recurso no existe.
- **409**: transición de estado inválida, pedido no modificable, o regla de negocio incumplida.

## Supuestos y decisiones

1. **Estados en minúscula en la BD y la API** (`en_entrega`), como ya hace tu CHECK de `pedidos`. En el código se manejan con enums; se aceptan en cualquier capitalización.
2. **`FACTURADO` no se pone a mano.** Solo se alcanza al generar la factura, para que nunca exista un pedido facturado sin factura.
3. **Entrega y pedido van sincronizados:** asignar entrega pasa el pedido de `pagado` a `en_entrega`; marcar la entrega `entregada` pasa el pedido a `entregado`. Una entrega `fallida` deja el pedido en `en_entrega` para reasignarla.
4. **Número de factura:** `FAC-000001`, consecutivo y sin huecos, con una fila bloqueada en `consecutivos_factura`.
5. **Subtotal y recargo de la factura** se reconstruyen desde el precio final pactado del pedido, no desde el precio actual del producto.
6. **Un producto no disponible no se puede vender** (409).
7. **Compatibilidad de baterías:** amperaje entre el recomendado y +30 %, y CCA igual o mayor al recomendado. Si el vehículo no tiene un dato, ese criterio no se aplica; sin ninguno, no hay sugerencias.
8. **Ventas** = pedidos `pagado`, `en_entrega`, `entregado` o `facturado`, filtrados por `fecha_creacion` (la tabla no guarda fecha de pago).
9. **Un PUT de pedido sin cambios reales no recalcula** el precio, así un cambio posterior en el precio del producto no altera lo ya pactado.
10. Las **baterías usadas** solo se registran para pedidos con `entrega_bateria_usada = true` en estado `en_entrega`, `entregado` o `facturado`, y se evalúan una sola vez.
