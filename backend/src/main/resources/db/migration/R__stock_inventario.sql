-- =====================================================================
-- Comanda · Semilla de STOCK de inventario (migración repetible)
--
-- Cómo usarla:
--   1. Edite las cantidades de este archivo.
--   2. Reinicie el backend (docker compose restart backend).
--   Flyway detecta que el archivo cambió y lo vuelve a aplicar.
--
-- IMPORTANTE: esta semilla FIJA el stock al valor indicado (no suma).
-- Para SUMAR una entrada de mercancía con el sistema en marcha use:
--   POST /api/inventario/entradas   (rol ADMINISTRADOR)
--
-- Valores de demostración:
--   · Chicharrón 150 g  → alcanza para una sola bandeja paisa (sirve para
--                          probar la validación concurrente F-05).
--   · Mojarra 0 und     → "Mojarra frita" aparece sin stock (F-02).
-- Revisión: 1
-- =====================================================================

UPDATE ingrediente SET stock = 20000 WHERE nombre = 'Arroz';
UPDATE ingrediente SET stock = 10000 WHERE nombre = 'Fríjol';
UPDATE ingrediente SET stock =  5000 WHERE nombre = 'Carne molida';
UPDATE ingrediente SET stock =   150 WHERE nombre = 'Chicharrón';
UPDATE ingrediente SET stock =    30 WHERE nombre = 'Chorizo';
UPDATE ingrediente SET stock =    60 WHERE nombre = 'Huevo';
UPDATE ingrediente SET stock =    30 WHERE nombre = 'Plátano maduro';
UPDATE ingrediente SET stock =    30 WHERE nombre = 'Plátano verde';
UPDATE ingrediente SET stock =    15 WHERE nombre = 'Aguacate';
UPDATE ingrediente SET stock =    40 WHERE nombre = 'Arepa';
UPDATE ingrediente SET stock = 10000 WHERE nombre = 'Pollo';
UPDATE ingrediente SET stock =  8000 WHERE nombre = 'Gallina';
UPDATE ingrediente SET stock =  6000 WHERE nombre = 'Cerdo';
UPDATE ingrediente SET stock =     0 WHERE nombre = 'Mojarra';
UPDATE ingrediente SET stock =  3000 WHERE nombre = 'Mariscos';
UPDATE ingrediente SET stock =  8000 WHERE nombre = 'Papa criolla';
UPDATE ingrediente SET stock = 12000 WHERE nombre = 'Papa sabanera';
UPDATE ingrediente SET stock =  6000 WHERE nombre = 'Yuca';
UPDATE ingrediente SET stock =    30 WHERE nombre = 'Mazorca';
UPDATE ingrediente SET stock =   300 WHERE nombre = 'Guascas';
UPDATE ingrediente SET stock =  3000 WHERE nombre = 'Crema de leche';
UPDATE ingrediente SET stock =   500 WHERE nombre = 'Alcaparras';
UPDATE ingrediente SET stock =   700 WHERE nombre = 'Cilantro';
UPDATE ingrediente SET stock =  5000 WHERE nombre = 'Cebolla';
UPDATE ingrediente SET stock =  5000 WHERE nombre = 'Tomate';
UPDATE ingrediente SET stock =  3000 WHERE nombre = 'Arveja';
UPDATE ingrediente SET stock =  3000 WHERE nombre = 'Zanahoria';
UPDATE ingrediente SET stock =   100 WHERE nombre = 'Limón';
UPDATE ingrediente SET stock =  5000 WHERE nombre = 'Maracuyá';
UPDATE ingrediente SET stock =  5000 WHERE nombre = 'Azúcar';
UPDATE ingrediente SET stock =  2000 WHERE nombre = 'Panela';
UPDATE ingrediente SET stock =  2500 WHERE nombre = 'Queso campesino';
UPDATE ingrediente SET stock = 10000 WHERE nombre = 'Leche';
UPDATE ingrediente SET stock =    50 WHERE nombre = 'Oblea';
UPDATE ingrediente SET stock =  1500 WHERE nombre = 'Arequipe';
UPDATE ingrediente SET stock =    48 WHERE nombre = 'Gaseosa 350 ml';
