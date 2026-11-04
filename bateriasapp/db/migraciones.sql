-- =====================================================================
-- db/migraciones.sql  -- ejecútalo UNA vez sobre la BD `bateriasapp`
-- (después de db/schema.sql). Cada bloque explica por qué existe:
-- Hibernate corre con ddl-auto=validate y no crea ni modifica nada.
-- =====================================================================
USE bateriasapp;

-- 1) pedidos.estado: tu CHECK solo admite cotizado/confirmado/entregado y el flujo necesita 7.
--    MySQL nombró el CHECK en línea como pedidos_chk_2 (el de metodo_pago es pedidos_chk_1).
--    Confírmalo antes con:  SHOW CREATE TABLE pedidos;
ALTER TABLE pedidos DROP CHECK pedidos_chk_2;
ALTER TABLE pedidos ADD CONSTRAINT chk_pedidos_estado CHECK (
    estado IN ('cotizado','confirmado','pagado','en_entrega','entregado','facturado','cancelado')
);

-- 2) entregas: faltan la dirección y el estado de la entrega.
--    La columna existente fecha_entrega se usa como fecha/hora de la entrega.
ALTER TABLE entregas
    ADD COLUMN direccion VARCHAR(200) NOT NULL DEFAULT '',
    ADD COLUMN estado    VARCHAR(30)  NOT NULL DEFAULT 'asignada',
    ADD CONSTRAINT chk_entregas_estado CHECK (estado IN ('asignada','en_camino','entregada','fallida'));

-- 3) baterias_usadas: la tabla no existía.
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
    -- una batería aceptada SIEMPRE tiene destino; las demás nunca
    CONSTRAINT chk_bateria_coherencia CHECK (
        (estado_evaluacion = 'aceptada' AND destino IS NOT NULL)
        OR (estado_evaluacion <> 'aceptada' AND destino IS NULL))
);

-- 4) facturas: faltan subtotal y recargo (solo existía total).
ALTER TABLE facturas
    ADD COLUMN subtotal DECIMAL(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN recargo  DECIMAL(10,2) NOT NULL DEFAULT 0;

-- 5) consecutivos_factura: una fila con el último número emitido. La app la bloquea
--    (SELECT ... FOR UPDATE) para que dos facturas simultáneas nunca repitan número.
--    Si ya emitiste facturas a mano, cambia el 0 por tu último número.
CREATE TABLE consecutivos_factura (
    id INT PRIMARY KEY,
    ultimo_numero INT NOT NULL
);
INSERT INTO consecutivos_factura (id, ultimo_numero) VALUES (1, 0);
