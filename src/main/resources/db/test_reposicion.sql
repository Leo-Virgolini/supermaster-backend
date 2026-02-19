-- =============================================
-- DATOS DE PRUEBA - Módulo de Reposición
-- =============================================
-- Ejecutar DESPUÉS de reposicion.sql
-- Ejecutar DESPUÉS de los ALTER:
--   ALTER TABLE productos ADD COLUMN moq INT DEFAULT NULL;
--   ALTER TABLE proveedores ADD COLUMN lead_time_dias INT DEFAULT NULL;
--   ALTER TABLE proveedores CHANGE COLUMN porcentaje financiacion_porcentaje DECIMAL(6,3);
-- =============================================

USE supermaster;

-- =============================================
-- 1) MAESTROS (solo si no existen)
-- =============================================

INSERT IGNORE INTO origenes (origen) VALUES ('NACIONAL'), ('IMPORTADO');

INSERT IGNORE INTO tipos (nombre) VALUES ('BOTELLA'), ('VASO'), ('BOWL'), ('ACCESORIO'), ('TAPA');

INSERT IGNORE INTO marcas (nombre) VALUES ('CRISTAL ARGENTINO'), ('VIDRIO SUR'), ('GLASSWARE PRO');

INSERT IGNORE INTO clasif_gral (nombre) VALUES ('HOGAR'), ('GASTRONOMIA'), ('INDUSTRIAL');

INSERT IGNORE INTO materiales (material) VALUES ('VIDRIO'), ('PLASTICO'), ('ACERO');

-- =============================================
-- 2) PROVEEDORES (con lead time)
-- =============================================

INSERT INTO proveedores (proveedor, apodo, plazo_pago, entrega, financiacion_porcentaje, lead_time_dias)
VALUES
  ('Cristalería del Litoral S.A.', 'LITORAL', '30 días', 1, 5.000, 15),
  ('Vidrios Pampeanos S.R.L.', 'PAMPEANOS', '60 días', 1, 8.000, 30),
  ('Importadora Glass World', 'GLASS WORLD', 'Anticipado', 0, 0.000, 60)
ON DUPLICATE KEY UPDATE
  lead_time_dias = VALUES(lead_time_dias),
  financiacion_porcentaje = VALUES(financiacion_porcentaje);

-- Guardar IDs de proveedores para usar después
SET @prov_litoral = (SELECT id_proveedor FROM proveedores WHERE apodo = 'LITORAL');
SET @prov_pampeanos = (SELECT id_proveedor FROM proveedores WHERE apodo = 'PAMPEANOS');
SET @prov_glass = (SELECT id_proveedor FROM proveedores WHERE apodo = 'GLASS WORLD');

-- =============================================
-- 3) PRODUCTOS DE PRUEBA
-- =============================================
-- Escenarios de prueba:
--   - Con/sin MOQ
--   - Con/sin UxB
--   - Con tag PRIO / LIQ / NULL
--   - De diferentes proveedores
--   - Activo / inactivo
-- =============================================

SET @id_origen_nac = (SELECT id_origen FROM origenes WHERE origen = 'NACIONAL' LIMIT 1);
SET @id_origen_imp = (SELECT id_origen FROM origenes WHERE origen = 'IMPORTADO' LIMIT 1);
SET @id_tipo_botella = (SELECT id_tipo FROM tipos WHERE nombre = 'BOTELLA' LIMIT 1);
SET @id_tipo_vaso = (SELECT id_tipo FROM tipos WHERE nombre = 'VASO' LIMIT 1);
SET @id_tipo_bowl = (SELECT id_tipo FROM tipos WHERE nombre = 'BOWL' LIMIT 1);
SET @id_tipo_accesorio = (SELECT id_tipo FROM tipos WHERE nombre = 'ACCESORIO' LIMIT 1);
SET @id_tipo_tapa = (SELECT id_tipo FROM tipos WHERE nombre = 'TAPA' LIMIT 1);
SET @id_clasif_hogar = (SELECT id_clasif_gral FROM clasif_gral WHERE nombre = 'HOGAR' LIMIT 1);
SET @id_clasif_gastro = (SELECT id_clasif_gral FROM clasif_gral WHERE nombre = 'GASTRONOMIA' LIMIT 1);

-- Proveedor LITORAL (lead time 15 días)
INSERT INTO productos (sku, cod_ext, descripcion, titulo_web, activo, id_origen, id_tipo, id_clasif_gral, id_proveedor, uxb, moq, costo, iva, tag_reposicion, fecha_creacion)
VALUES
  ('TEST-BOT-001', 'EXT001', 'Botella 750ml Cristal', 'Botella 750ml', 1, @id_origen_nac, @id_tipo_botella, @id_clasif_hogar, @prov_litoral, 12, NULL, 850.00, 21.000, NULL, NOW()),
  ('TEST-BOT-002', 'EXT002', 'Botella 500ml Ámbar', 'Botella 500ml Ámbar', 1, @id_origen_nac, @id_tipo_botella, @id_clasif_hogar, @prov_litoral, 24, 48, 620.00, 21.000, 'PRIO', NOW()),
  ('TEST-VAS-001', 'EXT003', 'Vaso Pinta 473ml', 'Vaso Pinta', 1, @id_origen_nac, @id_tipo_vaso, @id_clasif_gastro, @prov_litoral, 6, NULL, 450.00, 21.000, NULL, NOW()),
  ('TEST-VAS-002', 'EXT004', 'Vaso Shot 50ml', 'Vaso Shot', 1, @id_origen_nac, @id_tipo_vaso, @id_clasif_gastro, @prov_litoral, 48, 96, 120.00, 21.000, NULL, NOW())
ON DUPLICATE KEY UPDATE
  uxb = VALUES(uxb), moq = VALUES(moq), tag_reposicion = VALUES(tag_reposicion),
  id_proveedor = VALUES(id_proveedor), costo = VALUES(costo);

-- Proveedor PAMPEANOS (lead time 30 días)
INSERT INTO productos (sku, cod_ext, descripcion, titulo_web, activo, id_origen, id_tipo, id_clasif_gral, id_proveedor, uxb, moq, costo, iva, tag_reposicion, fecha_creacion)
VALUES
  ('TEST-BOW-001', 'EXT005', 'Bowl Ensaladera 2L', 'Bowl 2L', 1, @id_origen_nac, @id_tipo_bowl, @id_clasif_hogar, @prov_pampeanos, 6, 12, 1200.00, 21.000, NULL, NOW()),
  ('TEST-BOW-002', 'EXT006', 'Bowl Postre 300ml', 'Bowl Postre', 1, @id_origen_nac, @id_tipo_bowl, @id_clasif_gastro, @prov_pampeanos, 12, NULL, 380.00, 21.000, 'PRIO', NOW()),
  ('TEST-TAP-001', 'EXT007', 'Tapa Twist-Off 63mm', 'Tapa 63mm', 1, @id_origen_nac, @id_tipo_tapa, @id_clasif_hogar, @prov_pampeanos, 100, 500, 45.00, 21.000, NULL, NOW())
ON DUPLICATE KEY UPDATE
  uxb = VALUES(uxb), moq = VALUES(moq), tag_reposicion = VALUES(tag_reposicion),
  id_proveedor = VALUES(id_proveedor), costo = VALUES(costo);

-- Proveedor GLASS WORLD (lead time 60 días - importado)
INSERT INTO productos (sku, cod_ext, descripcion, titulo_web, activo, id_origen, id_tipo, id_clasif_gral, id_proveedor, uxb, moq, costo, iva, tag_reposicion, fecha_creacion)
VALUES
  ('TEST-ACC-001', 'EXT008', 'Dispensador Jugo 8L', 'Dispensador 8L', 1, @id_origen_imp, @id_tipo_accesorio, @id_clasif_gastro, @prov_glass, 1, 10, 15000.00, 21.000, NULL, NOW()),
  ('TEST-VAS-003', 'EXT009', 'Vaso Martini 250ml Premium', 'Vaso Martini Premium', 1, @id_origen_imp, @id_tipo_vaso, @id_clasif_gastro, @prov_glass, 6, NULL, 2800.00, 21.000, NULL, NOW())
ON DUPLICATE KEY UPDATE
  uxb = VALUES(uxb), moq = VALUES(moq), tag_reposicion = VALUES(tag_reposicion),
  id_proveedor = VALUES(id_proveedor), costo = VALUES(costo);

-- Producto en LIQUIDACIÓN (no debe aparecer en sugerencias)
INSERT INTO productos (sku, cod_ext, descripcion, titulo_web, activo, id_origen, id_tipo, id_clasif_gral, id_proveedor, uxb, moq, costo, iva, tag_reposicion, fecha_creacion)
VALUES
  ('TEST-LIQ-001', 'EXT010', 'Frasco Descontinuado 1L', 'Frasco 1L (DISC)', 1, @id_origen_nac, @id_tipo_botella, @id_clasif_hogar, @prov_litoral, 6, NULL, 300.00, 21.000, 'LIQ', NOW())
ON DUPLICATE KEY UPDATE
  tag_reposicion = VALUES(tag_reposicion);

-- Producto INACTIVO (no debe aparecer en sugerencias)
INSERT INTO productos (sku, cod_ext, descripcion, titulo_web, activo, id_origen, id_tipo, id_clasif_gral, id_proveedor, uxb, moq, costo, iva, tag_reposicion, fecha_creacion)
VALUES
  ('TEST-INA-001', 'EXT011', 'Vaso Modelo Viejo', 'Vaso Viejo', 0, @id_origen_nac, @id_tipo_vaso, @id_clasif_hogar, @prov_litoral, 6, NULL, 200.00, 21.000, NULL, NOW())
ON DUPLICATE KEY UPDATE
  activo = VALUES(activo);

-- Producto SIN proveedor (no debe aparecer en sugerencias)
INSERT INTO productos (sku, cod_ext, descripcion, titulo_web, activo, id_origen, id_tipo, id_clasif_gral, id_proveedor, uxb, moq, costo, iva, tag_reposicion, fecha_creacion)
VALUES
  ('TEST-SIN-001', 'EXT012', 'Muestra sin proveedor', 'Muestra', 1, @id_origen_nac, @id_tipo_accesorio, @id_clasif_hogar, NULL, NULL, NULL, 100.00, 21.000, NULL, NOW())
ON DUPLICATE KEY UPDATE
  id_proveedor = VALUES(id_proveedor);

-- =============================================
-- 4) ÓRDENES DE COMPRA (para probar pendientes)
-- =============================================
-- Estas OC simulan compras previas y afectan
-- el campo "pendienteProveedores" en la fórmula.
-- =============================================

-- Guardar IDs de productos de prueba
SET @prod_bot_001 = (SELECT id_producto FROM productos WHERE sku = 'TEST-BOT-001');
SET @prod_bot_002 = (SELECT id_producto FROM productos WHERE sku = 'TEST-BOT-002');
SET @prod_vas_001 = (SELECT id_producto FROM productos WHERE sku = 'TEST-VAS-001');
SET @prod_vas_002 = (SELECT id_producto FROM productos WHERE sku = 'TEST-VAS-002');
SET @prod_bow_001 = (SELECT id_producto FROM productos WHERE sku = 'TEST-BOW-001');
SET @prod_bow_002 = (SELECT id_producto FROM productos WHERE sku = 'TEST-BOW-002');
SET @prod_tap_001 = (SELECT id_producto FROM productos WHERE sku = 'TEST-TAP-001');
SET @prod_acc_001 = (SELECT id_producto FROM productos WHERE sku = 'TEST-ACC-001');
SET @prod_vas_003 = (SELECT id_producto FROM productos WHERE sku = 'TEST-VAS-003');

-- OC #1: ENVIADA a LITORAL (parcialmente recibida = pendiente proveedor)
--   Esto suma a "pendienteProveedores" en la fórmula
INSERT INTO ordenes_compra (id_proveedor, estado, observaciones, fecha_creacion)
VALUES (@prov_litoral, 'ENVIADA', 'OC de prueba - enviada', DATE_SUB(NOW(), INTERVAL 10 DAY));

SET @oc1 = LAST_INSERT_ID();

INSERT INTO orden_compra_lineas (id_orden_compra, id_producto, cantidad_pedida, cantidad_recibida, costo_unitario)
VALUES
  (@oc1, @prod_bot_001, 120, 0, 850.00),   -- 120 pendientes de recibir
  (@oc1, @prod_vas_001, 60, 30, 450.00);    -- 30 pendientes de recibir

-- OC #2: RECIBIDA_PARCIAL a PAMPEANOS
INSERT INTO ordenes_compra (id_proveedor, estado, observaciones, fecha_creacion)
VALUES (@prov_pampeanos, 'RECIBIDA_PARCIAL', 'OC de prueba - parcial', DATE_SUB(NOW(), INTERVAL 20 DAY));

SET @oc2 = LAST_INSERT_ID();

INSERT INTO orden_compra_lineas (id_orden_compra, id_producto, cantidad_pedida, cantidad_recibida, costo_unitario)
VALUES
  (@oc2, @prod_bow_001, 24, 12, 1200.00),   -- 12 pendientes
  (@oc2, @prod_tap_001, 1000, 500, 45.00);   -- 500 pendientes

-- OC #3: COMPLETA a LITORAL (NO suma a pendientes, ya se recibió todo)
INSERT INTO ordenes_compra (id_proveedor, estado, observaciones, fecha_creacion)
VALUES (@prov_litoral, 'COMPLETA', 'OC completada hace 2 meses', DATE_SUB(NOW(), INTERVAL 60 DAY));

SET @oc3 = LAST_INSERT_ID();

INSERT INTO orden_compra_lineas (id_orden_compra, id_producto, cantidad_pedida, cantidad_recibida, costo_unitario)
VALUES
  (@oc3, @prod_bot_002, 96, 96, 600.00),
  (@oc3, @prod_vas_002, 192, 192, 110.00);

-- OC #4: CANCELADA (NO suma a pendientes)
INSERT INTO ordenes_compra (id_proveedor, estado, observaciones, fecha_creacion)
VALUES (@prov_glass, 'CANCELADA', 'OC cancelada', DATE_SUB(NOW(), INTERVAL 45 DAY));

SET @oc4 = LAST_INSERT_ID();

INSERT INTO orden_compra_lineas (id_orden_compra, id_producto, cantidad_pedida, cantidad_recibida, costo_unitario)
VALUES
  (@oc4, @prod_acc_001, 20, 0, 14500.00);

-- OC #5: BORRADOR a GLASS WORLD (NO suma a pendientes, aún no se envió)
--   Esto además testea la prevención de duplicados: si intentás
--   generar OC para GLASS WORLD, debería fallar con 400.
INSERT INTO ordenes_compra (id_proveedor, estado, observaciones, fecha_creacion)
VALUES (@prov_glass, 'BORRADOR', 'Borrador para testear duplicados', NOW());

SET @oc5 = LAST_INSERT_ID();

INSERT INTO orden_compra_lineas (id_orden_compra, id_producto, cantidad_pedida, cantidad_recibida, costo_unitario)
VALUES
  (@oc5, @prod_vas_003, 30, 0, 2800.00);

-- =============================================
-- 5) CONFIGURACIÓN DE REPOSICIÓN
-- =============================================
-- Ajustar idEmpresaDux e idSucursalDux con los
-- valores reales de tu cuenta DUX.
-- =============================================

UPDATE reposicion_config
SET meses_cobertura = 2,
    peso_mes1 = 0.50,
    peso_mes2 = 0.30,
    peso_mes3 = 0.20,
    id_empresa_dux = NULL,   -- << REEMPLAZAR con tu ID de empresa DUX
    id_sucursal_dux = NULL   -- << REEMPLAZAR con tu ID de sucursal DUX
WHERE id = 1;

-- =============================================
-- RESUMEN DE DATOS DE PRUEBA
-- =============================================
--
-- PROVEEDORES (3):
--   LITORAL      - lead_time=15 días, financiación=5%
--   PAMPEANOS    - lead_time=30 días, financiación=8%
--   GLASS WORLD  - lead_time=60 días, financiación=0%
--
-- PRODUCTOS ACTIVOS CON PROVEEDOR (9):
--   TEST-BOT-001 | LITORAL    | uxb=12  | moq=NULL | tag=NULL
--   TEST-BOT-002 | LITORAL    | uxb=24  | moq=48   | tag=PRIO
--   TEST-VAS-001 | LITORAL    | uxb=6   | moq=NULL | tag=NULL
--   TEST-VAS-002 | LITORAL    | uxb=48  | moq=96   | tag=NULL
--   TEST-BOW-001 | PAMPEANOS  | uxb=6   | moq=12   | tag=NULL
--   TEST-BOW-002 | PAMPEANOS  | uxb=12  | moq=NULL | tag=PRIO
--   TEST-TAP-001 | PAMPEANOS  | uxb=100 | moq=500  | tag=NULL
--   TEST-ACC-001 | GLASS WORLD| uxb=1   | moq=10   | tag=NULL
--   TEST-VAS-003 | GLASS WORLD| uxb=6   | moq=NULL | tag=NULL
--
-- EXCLUIDOS DEL CÁLCULO (3):
--   TEST-LIQ-001 | tag=LIQ (liquidación)
--   TEST-INA-001 | activo=false
--   TEST-SIN-001 | sin proveedor
--
-- ÓRDENES DE COMPRA (5):
--   OC#1 ENVIADA       → LITORAL    → bot-001: 120 pend, vas-001: 30 pend
--   OC#2 RECIB_PARCIAL → PAMPEANOS  → bow-001: 12 pend, tap-001: 500 pend
--   OC#3 COMPLETA      → LITORAL    → (0 pendientes)
--   OC#4 CANCELADA     → GLASS WORLD→ (0 pendientes)
--   OC#5 BORRADOR      → GLASS WORLD→ (0 pendientes, bloquea generar OC)
--
-- PENDIENTES PROVEEDOR (que afectan la fórmula):
--   TEST-BOT-001: +120 unidades en camino
--   TEST-VAS-001: +30 unidades en camino
--   TEST-BOW-001: +12 unidades en camino
--   TEST-TAP-001: +500 unidades en camino
--
-- =============================================
-- FLUJO DE PRUEBA SUGERIDO
-- =============================================
--
-- 1. Configurar DUX:
--    PUT /api/reposicion/config
--    { "idEmpresaDux": <tu_id>, "idSucursalDux": <tu_id> }
--
-- 2. Calcular sugerencias:
--    POST /api/reposicion/calcular  → 202
--    GET  /api/reposicion/calcular/estado  (polling)
--
-- 3. Ver resultados:
--    GET /api/reposicion/resultado
--    Verificar:
--      - TEST-LIQ-001 NO aparece (liquidación)
--      - TEST-INA-001 NO aparece (inactivo)
--      - TEST-SIN-001 NO aparece (sin proveedor)
--      - TEST-BOT-001 tiene pendienteProveedores=120
--      - Urgentes al principio de la lista
--      - PRIO antes que productos normales
--
-- 4. Descargar Excel:
--    GET /api/reposicion/resultado/excel
--    Verificar hojas por proveedor + hoja RESUMEN
--
-- 5. Ajustar pedido manualmente:
--    PUT /api/reposicion/resultado/ajustar
--    { "ajustes": [
--        { "productoId": <id_bot_001>, "pedido": 0 },
--        { "productoId": <id_vas_001>, "pedido": 100 }
--    ]}
--
-- 6. Generar OC (solo para LITORAL y PAMPEANOS):
--    POST /api/reposicion/generar-ordenes?proveedorId=<id_litoral>  → 201
--    POST /api/reposicion/generar-ordenes?proveedorId=<id_pampeanos> → 201
--
-- 7. Intentar generar OC para GLASS WORLD (tiene BORRADOR):
--    POST /api/reposicion/generar-ordenes?proveedorId=<id_glass>  → 400
--    "Ya existen órdenes en BORRADOR para: GLASS WORLD"
--
-- 8. Gestionar OC generadas:
--    GET  /api/ordenes-compra?estado=BORRADOR
--    PUT  /api/ordenes-compra/<id>  (editar líneas)
--    POST /api/ordenes-compra/<id>/enviar
--    POST /api/ordenes-compra/<id>/recepcion
--
-- 9. Excel de OC individual:
--    GET /api/reposicion/resultado/excel/oc/<id>
--
-- =============================================
