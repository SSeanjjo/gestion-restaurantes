-- =====================================================================
-- Comanda · v0.3
--   F-08  Cola persistente de comandas con confirmación de recepción (ACK)
--   F-14  Solicitud de cuenta (estado E-07)
--   CU-02 Cobro, factura electrónica y cierre de caja
-- =====================================================================

-- ---------------------------------------------------------------------
-- Pedido: idempotencia de la tablet, cuenta solicitada y facturación
-- ---------------------------------------------------------------------
ALTER TABLE pedido
    ADD COLUMN clave_idempotencia   VARCHAR(36) NULL,
    ADD COLUMN cuenta_solicitada_en DATETIME(6) NULL,
    ADD COLUMN facturado_en         DATETIME(6) NULL,
    ADD CONSTRAINT uk_pedido_clave_idempotencia UNIQUE (clave_idempotencia);

-- ---------------------------------------------------------------------
-- F-08 · Outbox de comandas: una fila por pedido enviado a cocina
-- ---------------------------------------------------------------------
CREATE TABLE entrega_comanda (
    pedido_id      BIGINT      NOT NULL,
    publicada_en   DATETIME(6) NOT NULL,
    confirmada_en  DATETIME(6),
    intentos       INT         NOT NULL DEFAULT 1,
    PRIMARY KEY (pedido_id),
    CONSTRAINT fk_entrega_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id) ON DELETE CASCADE,
    INDEX ix_entrega_pendiente (confirmada_en, publicada_en)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- CU-02 · Factura electrónica (Contrato 3)
-- ---------------------------------------------------------------------
CREATE TABLE factura (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    numero           VARCHAR(30),
    pedido_id        BIGINT        NOT NULL,
    cajero_id        BIGINT        NOT NULL,
    fecha_emision    DATETIME(6)   NOT NULL,
    subtotal         DECIMAL(12,2) NOT NULL,
    impuesto         DECIMAL(12,2) NOT NULL,
    tarifa_impuesto  DECIMAL(5,4)  NOT NULL,
    total            DECIMAL(12,2) NOT NULL,
    cufe             VARCHAR(96),
    estado_dian      VARCHAR(20),               -- ACEPTADA | RECHAZADA
    mensaje_dian     VARCHAR(255),
    validada_en      DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_factura_numero UNIQUE (numero),
    CONSTRAINT uk_factura_pedido UNIQUE (pedido_id),       -- RD-05: una factura por pedido
    CONSTRAINT fk_factura_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id),
    CONSTRAINT fk_factura_cajero FOREIGN KEY (cajero_id) REFERENCES usuario (id),
    CONSTRAINT ck_factura_total CHECK (total >= 0),
    INDEX ix_factura_fecha (fecha_emision)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- RF-09 · Transacciones de caja (una factura puede tener varios pagos: curso alterno 4a)
CREATE TABLE pago (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    factura_id       BIGINT        NOT NULL,
    metodo           VARCHAR(20)   NOT NULL,    -- EFECTIVO | TARJETA
    monto            DECIMAL(12,2) NOT NULL,
    monto_recibido   DECIMAL(12,2),
    cambio           DECIMAL(12,2),
    autorizacion     VARCHAR(40),
    ultimos_digitos  CHAR(4),
    fecha_hora       DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_pago_factura FOREIGN KEY (factura_id) REFERENCES factura (id) ON DELETE CASCADE,
    CONSTRAINT ck_pago_monto CHECK (monto > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- RF-10 / RF-11 · Cierre de caja diario
CREATE TABLE cierre_caja (
    id                    BIGINT        NOT NULL AUTO_INCREMENT,
    fecha                 DATE          NOT NULL,
    ejecutado_por         BIGINT        NOT NULL,
    ejecutado_en          DATETIME(6)   NOT NULL,
    numero_facturas       INT           NOT NULL,
    numero_transacciones  INT           NOT NULL,
    total_ventas          DECIMAL(14,2) NOT NULL,
    total_efectivo        DECIMAL(14,2) NOT NULL,
    total_tarjeta         DECIMAL(14,2) NOT NULL,
    total_impuesto        DECIMAL(14,2) NOT NULL,
    efectivo_contado      DECIMAL(14,2),
    inconsistencias       VARCHAR(2000),
    observacion           VARCHAR(255),
    comprobante_dian      VARCHAR(60),
    PRIMARY KEY (id),
    CONSTRAINT uk_cierre_fecha UNIQUE (fecha),
    CONSTRAINT fk_cierre_usuario FOREIGN KEY (ejecutado_por) REFERENCES usuario (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
