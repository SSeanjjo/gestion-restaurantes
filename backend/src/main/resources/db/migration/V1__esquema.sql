-- =====================================================================
-- Comanda · Esquema inicial (CU-01 Gestionar Pedido)
-- Motor: MariaDB 11 · Charset utf8mb4
-- =====================================================================

CREATE TABLE usuario (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    username       VARCHAR(50)  NOT NULL,
    nombre         VARCHAR(100) NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    rol            VARCHAR(20)  NOT NULL,          -- MESERO | COCINERO | CAJERO | ADMINISTRADOR
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uk_usuario_username UNIQUE (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE mesa (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    numero     INT         NOT NULL,
    capacidad  INT         NOT NULL DEFAULT 4,
    estado     VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE', -- DISPONIBLE | OCUPADA | RESERVADA
    PRIMARY KEY (id),
    CONSTRAINT uk_mesa_numero UNIQUE (numero)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Inventario y catálogo de recetas
-- ---------------------------------------------------------------------
CREATE TABLE ingrediente (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    nombre        VARCHAR(100)  NOT NULL,
    unidad        VARCHAR(10)   NOT NULL,             -- g | ml | und
    stock         DECIMAL(12,2) NOT NULL DEFAULT 0,
    stock_minimo  DECIMAL(12,2) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_ingrediente_nombre UNIQUE (nombre),
    CONSTRAINT ck_ingrediente_stock CHECK (stock >= 0),
    CONSTRAINT ck_ingrediente_minimo CHECK (stock_minimo >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE producto_menu (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    nombre       VARCHAR(100)  NOT NULL,
    descripcion  VARCHAR(255),
    precio       DECIMAL(12,2) NOT NULL,
    categoria    VARCHAR(50)   NOT NULL,
    estado       VARCHAR(20)   NOT NULL DEFAULT 'ACTIVO', -- ACTIVO | INACTIVO (suspendido)
    PRIMARY KEY (id),
    CONSTRAINT uk_producto_nombre UNIQUE (nombre),
    CONSTRAINT ck_producto_precio CHECK (precio >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Receta: cantidad de cada ingrediente necesaria para UNA unidad del plato
CREATE TABLE receta_ingrediente (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    producto_id     BIGINT        NOT NULL,
    ingrediente_id  BIGINT        NOT NULL,
    cantidad        DECIMAL(12,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_receta UNIQUE (producto_id, ingrediente_id),
    CONSTRAINT ck_receta_cantidad CHECK (cantidad > 0),
    CONSTRAINT fk_receta_producto FOREIGN KEY (producto_id) REFERENCES producto_menu (id) ON DELETE CASCADE,
    CONSTRAINT fk_receta_ingrediente FOREIGN KEY (ingrediente_id) REFERENCES ingrediente (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Pedidos
-- ---------------------------------------------------------------------
CREATE TABLE pedido (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    mesa_id           BIGINT        NOT NULL,
    mesero_id         BIGINT        NOT NULL,
    fecha_hora        DATETIME(6)   NOT NULL,
    estado            VARCHAR(30)   NOT NULL,  -- REGISTRADO | EN_PREPARACION | LISTO_PARA_SERVIR | ENTREGADO | FACTURADO | CANCELADO
    total             DECIMAL(12,2) NOT NULL DEFAULT 0,
    notificado_en     DATETIME(6),
    entregado_en      DATETIME(6),
    anulado_en        DATETIME(6),
    anulado_por       BIGINT,
    motivo_anulacion  VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT fk_pedido_mesa FOREIGN KEY (mesa_id) REFERENCES mesa (id),
    CONSTRAINT fk_pedido_mesero FOREIGN KEY (mesero_id) REFERENCES usuario (id),
    CONSTRAINT fk_pedido_anulado_por FOREIGN KEY (anulado_por) REFERENCES usuario (id),
    INDEX ix_pedido_estado (estado),
    INDEX ix_pedido_mesa_estado (mesa_id, estado)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE detalle_pedido (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    pedido_id        BIGINT        NOT NULL,
    producto_id      BIGINT        NOT NULL,
    cantidad         INT           NOT NULL,
    precio_unitario  DECIMAL(12,2) NOT NULL,
    subtotal         DECIMAL(12,2) NOT NULL,
    preparado        BOOLEAN       NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT ck_detalle_cantidad CHECK (cantidad > 0),
    CONSTRAINT fk_detalle_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id) ON DELETE CASCADE,
    CONSTRAINT fk_detalle_producto FOREIGN KEY (producto_id) REFERENCES producto_menu (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Trazabilidad de cada entrada, descuento o reintegro de inventario
CREATE TABLE movimiento_inventario (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    ingrediente_id  BIGINT        NOT NULL,
    tipo            VARCHAR(20)   NOT NULL,  -- ENTRADA | SALIDA_PEDIDO | REINTEGRO
    cantidad        DECIMAL(12,2) NOT NULL,
    pedido_id       BIGINT,
    usuario_id      BIGINT,
    fecha_hora      DATETIME(6)   NOT NULL,
    observacion     VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT fk_mov_ingrediente FOREIGN KEY (ingrediente_id) REFERENCES ingrediente (id),
    CONSTRAINT fk_mov_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id),
    CONSTRAINT fk_mov_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    INDEX ix_mov_ingrediente (ingrediente_id, fecha_hora)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
