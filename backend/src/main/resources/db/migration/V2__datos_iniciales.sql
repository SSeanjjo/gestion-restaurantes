-- =====================================================================
-- Comanda · Datos iniciales (mesas, usuarios, catálogo y recetas)
-- El STOCK de los ingredientes NO se define aquí: está en
-- R__stock_inventario.sql, que se puede editar y volver a aplicar.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Mesas (12, como en el mockup IU-01)
-- ---------------------------------------------------------------------
INSERT INTO mesa (numero, capacidad, estado) VALUES
 (1, 4, 'DISPONIBLE'), (2, 4, 'DISPONIBLE'), (3, 2, 'DISPONIBLE'), (4, 4, 'DISPONIBLE'),
 (5, 6, 'DISPONIBLE'), (6, 4, 'DISPONIBLE'), (7, 2, 'DISPONIBLE'), (8, 4, 'DISPONIBLE'),
 (9, 6, 'DISPONIBLE'), (10, 4, 'DISPONIBLE'), (11, 2, 'DISPONIBLE'), (12, 8, 'DISPONIBLE');

-- ---------------------------------------------------------------------
-- Usuarios de prueba (contraseñas en el README; hash BCrypt)
-- ---------------------------------------------------------------------
INSERT INTO usuario (username, nombre, password_hash, rol) VALUES
 ('mesero1', 'Laura Gómez',   '$2a$10$tLtOCDxjYQTh94KYjOyGL.0H4QTRSE4VihURPh0C95w5GyH0CtSQW', 'MESERO'),
 ('mesero2', 'Andrés Ríos',   '$2a$10$tLtOCDxjYQTh94KYjOyGL.0H4QTRSE4VihURPh0C95w5GyH0CtSQW', 'MESERO'),
 ('cocina',  'Cocina',        '$2a$10$CG5GpF8hmW20Xx4Q/k/qF.LVGj/uDiJP6s/cYkkVRiEQwPjGX4VTi', 'COCINERO'),
 ('caja',    'Caja',          '$2a$10$IjBR9csOIWdKhEI7Wvoy1OpUMvaD4/2aQU0Ej64VbAGuuDev0FI5q', 'CAJERO'),
 ('admin',   'Administrador', '$2a$10$KhP7jZTV/ElIXyH8A.S7POei3EYD1Jp.NWjKRYYhVXOoFTOyTVhV2', 'ADMINISTRADOR');

-- ---------------------------------------------------------------------
-- Ingredientes (stock inicial 0; ver R__stock_inventario.sql)
-- ---------------------------------------------------------------------
INSERT INTO ingrediente (nombre, unidad, stock_minimo) VALUES
 ('Arroz',             'g',   2000),
 ('Fríjol',            'g',   1500),
 ('Carne molida',      'g',   1000),
 ('Chicharrón',        'g',   500),
 ('Chorizo',           'und', 5),
 ('Huevo',             'und', 12),
 ('Plátano maduro',    'und', 5),
 ('Plátano verde',     'und', 5),
 ('Aguacate',          'und', 3),
 ('Arepa',             'und', 10),
 ('Pollo',             'g',   1500),
 ('Gallina',           'g',   1500),
 ('Cerdo',             'g',   1000),
 ('Mojarra',           'und', 3),
 ('Mariscos',          'g',   1000),
 ('Papa criolla',      'g',   1500),
 ('Papa sabanera',     'g',   2000),
 ('Yuca',              'g',   1000),
 ('Mazorca',           'und', 5),
 ('Guascas',           'g',   50),
 ('Crema de leche',    'ml',  500),
 ('Alcaparras',        'g',   100),
 ('Cilantro',          'g',   500),
 ('Cebolla',           'g',   1000),
 ('Tomate',            'g',   1000),
 ('Arveja',            'g',   500),
 ('Zanahoria',         'g',   500),
 ('Limón',             'und', 20),
 ('Maracuyá',          'g',   1000),
 ('Azúcar',            'g',   1000),
 ('Panela',            'g',   500),
 ('Queso campesino',   'g',   500),
 ('Leche',             'ml',  2000),
 ('Oblea',             'und', 10),
 ('Arequipe',          'g',   300),
 ('Gaseosa 350 ml',    'und', 12);

-- ---------------------------------------------------------------------
-- Productos del menú
-- ---------------------------------------------------------------------
INSERT INTO producto_menu (nombre, descripcion, precio, categoria, estado) VALUES
 ('Ajiaco santafereño',  'Con pollo desmechado, mazorca, crema y alcaparras',  22000, 'Sopas',          'ACTIVO'),
 ('Sancocho de gallina', 'Con yuca, plátano verde, mazorca y cilantro',        20000, 'Sopas',          'ACTIVO'),
 ('Sopa de verduras',    'Papa, zanahoria y arveja',                           12000, 'Sopas',          'ACTIVO'),
 ('Bandeja paisa',       'Arroz, fríjol, carne molida, chicharrón, chorizo, huevo, maduro, aguacate y arepa', 28000, 'Platos fuertes', 'ACTIVO'),
 ('Arroz con pollo',     'Con arveja y zanahoria',                             18000, 'Platos fuertes', 'ACTIVO'),
 ('Lomo de cerdo',       'A la plancha con papa criolla y hogao',              26000, 'Platos fuertes', 'ACTIVO'),
 ('Mojarra frita',       'Con patacón, arroz de coco y ensalada',              30000, 'Platos fuertes', 'ACTIVO'),
 ('Cazuela de mariscos', 'Mariscos en salsa de crema',                         35000, 'Platos fuertes', 'INACTIVO'),
 ('Limonada natural',    'Vaso de 16 oz',                                       5000, 'Bebidas',        'ACTIVO'),
 ('Jugo de maracuyá',    'En agua, vaso de 16 oz',                              6000, 'Bebidas',        'ACTIVO'),
 ('Aguapanela con queso','Caliente, con queso campesino',                       5000, 'Bebidas',        'ACTIVO'),
 ('Gaseosa',             'Botella de 350 ml',                                   4000, 'Bebidas',        'ACTIVO'),
 ('Arroz con leche',     'Porción individual',                                  7000, 'Postres',        'ACTIVO'),
 ('Oblea con arequipe',  'Dos obleas con arequipe',                             6000, 'Postres',        'ACTIVO');

-- ---------------------------------------------------------------------
-- Catálogo de recetas: cantidad por UNA unidad del plato
-- ---------------------------------------------------------------------
CREATE TEMPORARY TABLE tmp_receta (
    producto     VARCHAR(100),
    ingrediente  VARCHAR(100),
    cantidad     DECIMAL(12,2)
);

INSERT INTO tmp_receta (producto, ingrediente, cantidad) VALUES
 -- Sopas
 ('Ajiaco santafereño',  'Pollo',          150),
 ('Ajiaco santafereño',  'Papa criolla',   100),
 ('Ajiaco santafereño',  'Papa sabanera',  150),
 ('Ajiaco santafereño',  'Mazorca',          1),
 ('Ajiaco santafereño',  'Guascas',          5),
 ('Ajiaco santafereño',  'Crema de leche',  30),
 ('Ajiaco santafereño',  'Alcaparras',      10),
 ('Sancocho de gallina', 'Gallina',        200),
 ('Sancocho de gallina', 'Yuca',           150),
 ('Sancocho de gallina', 'Papa sabanera',  100),
 ('Sancocho de gallina', 'Plátano verde',    1),
 ('Sancocho de gallina', 'Mazorca',          1),
 ('Sancocho de gallina', 'Cilantro',         5),
 ('Sopa de verduras',    'Papa sabanera',  100),
 ('Sopa de verduras',    'Zanahoria',       50),
 ('Sopa de verduras',    'Arveja',          40),
 ('Sopa de verduras',    'Cebolla',         30),
 ('Sopa de verduras',    'Cilantro',         5),
 -- Platos fuertes
 ('Bandeja paisa',       'Arroz',          150),
 ('Bandeja paisa',       'Fríjol',         150),
 ('Bandeja paisa',       'Carne molida',   120),
 ('Bandeja paisa',       'Chicharrón',     100),
 ('Bandeja paisa',       'Chorizo',          1),
 ('Bandeja paisa',       'Huevo',            1),
 ('Bandeja paisa',       'Plátano maduro',   1),
 ('Bandeja paisa',       'Aguacate',      0.25),
 ('Bandeja paisa',       'Arepa',            1),
 ('Arroz con pollo',     'Arroz',          200),
 ('Arroz con pollo',     'Pollo',          150),
 ('Arroz con pollo',     'Arveja',          40),
 ('Arroz con pollo',     'Zanahoria',       40),
 ('Arroz con pollo',     'Cebolla',         30),
 ('Lomo de cerdo',       'Cerdo',          220),
 ('Lomo de cerdo',       'Papa criolla',   150),
 ('Lomo de cerdo',       'Tomate',          50),
 ('Lomo de cerdo',       'Cebolla',         30),
 ('Mojarra frita',       'Mojarra',          1),
 ('Mojarra frita',       'Plátano verde',    1),
 ('Mojarra frita',       'Arroz',          150),
 ('Mojarra frita',       'Tomate',          50),
 ('Cazuela de mariscos', 'Mariscos',       250),
 ('Cazuela de mariscos', 'Crema de leche',  80),
 ('Cazuela de mariscos', 'Cebolla',         30),
 -- Bebidas
 ('Limonada natural',    'Limón',            2),
 ('Limonada natural',    'Azúcar',          20),
 ('Jugo de maracuyá',    'Maracuyá',       120),
 ('Jugo de maracuyá',    'Azúcar',          20),
 ('Aguapanela con queso','Panela',          40),
 ('Aguapanela con queso','Queso campesino', 50),
 ('Gaseosa',             'Gaseosa 350 ml',   1),
 -- Postres
 ('Arroz con leche',     'Arroz',           60),
 ('Arroz con leche',     'Leche',          200),
 ('Arroz con leche',     'Azúcar',          30),
 ('Oblea con arequipe',  'Oblea',            2),
 ('Oblea con arequipe',  'Arequipe',        40);

INSERT INTO receta_ingrediente (producto_id, ingrediente_id, cantidad)
SELECT p.id, i.id, t.cantidad
FROM tmp_receta t
JOIN producto_menu p ON p.nombre = t.producto
JOIN ingrediente   i ON i.nombre = t.ingrediente;

DROP TEMPORARY TABLE tmp_receta;
