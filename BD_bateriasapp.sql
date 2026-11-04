USE bateriasapp;

-- 1) Estados del pedido
ALTER TABLE pedidos DROP CHECK pedidos_chk_2;
ALTER TABLE pedidos ADD CONSTRAINT chk_pedidos_estado CHECK (
    estado IN ('cotizado','confirmado','pagado','en_entrega','entregado','facturado','cancelado')
);

-- 2) Entregas: dirección y estado
ALTER TABLE entregas
    ADD COLUMN direccion VARCHAR(200) NOT NULL DEFAULT '',
    ADD COLUMN estado    VARCHAR(30)  NOT NULL DEFAULT 'asignada',
    ADD CONSTRAINT chk_entregas_estado CHECK (estado IN ('asignada','en_camino','entregada','fallida'));

-- 3) Baterías usadas: se reemplaza la tabla vacía por la del nuevo diseño
DROP TABLE baterias_usadas;
CREATE TABLE baterias_usadas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    pedido_id INT NOT NULL UNIQUE,
    descripcion VARCHAR(200) NULL,
    estado_evaluacion VARCHAR(30) NOT NULL DEFAULT 'pendiente_evaluacion',
    destino VARCHAR(30) NULL,
    fecha_recepcion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_evaluacion DATETIME NULL,
    observaciones VARCHAR(255) NULL,
    FOREIGN KEY (pedido_id) REFERENCES pedidos(id),
    CONSTRAINT chk_bateria_estado  CHECK (estado_evaluacion IN ('pendiente_evaluacion','aceptada','rechazada')),
    CONSTRAINT chk_bateria_destino CHECK (destino IS NULL OR destino IN ('lote_mensual','reventa')),
    CONSTRAINT chk_bateria_coherencia CHECK (
        (estado_evaluacion = 'aceptada' AND destino IS NOT NULL)
        OR (estado_evaluacion <> 'aceptada' AND destino IS NULL))
);

-- 4) Facturas: subtotal y recargo
ALTER TABLE facturas
    ADD COLUMN subtotal DECIMAL(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN recargo  DECIMAL(10,2) NOT NULL DEFAULT 0;

-- 5) Consecutivo de facturas
CREATE TABLE consecutivos_factura (
    id INT PRIMARY KEY,
    ultimo_numero INT NOT NULL
);
INSERT INTO consecutivos_factura (id, ultimo_numero) VALUES (1, 0);

SHOW TABLES;

DESCRIBE entregas;      -- debe incluir direccion y estado
DESCRIBE facturas;      -- debe incluir subtotal y recargo

USE bateriasapp;
INSERT INTO clientes (nombre, cedula, telefono, direccion) VALUES ('Ana Gómez', '1000000001', '3001234567', 'Calle 100 # 15-20');
INSERT INTO productos (nombre, tipo, amperaje, cca, precio_base) VALUES ('Batería 60Ah', 'bateria', 60, 540, 250000);
INSERT INTO domiciliarios (nombre, telefono) VALUES ('Carlos Ruiz', '3109876543');