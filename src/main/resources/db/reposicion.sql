-- =============================================
-- Script de reposición de productos
-- Tablas: ordenes_compra, orden_compra_lineas,
--         reposicion_config
-- ALTER: productos.tag_reposicion
-- =============================================

-- Tag de reposición en productos
ALTER TABLE productos ADD COLUMN tag_reposicion ENUM('PRIO','LIQ') DEFAULT NULL;

-- Órdenes de compra a proveedores
CREATE TABLE ordenes_compra (
  id_orden_compra INT AUTO_INCREMENT PRIMARY KEY,
  id_proveedor INT NOT NULL,
  estado ENUM('BORRADOR','ENVIADA','RECIBIDA_PARCIAL','COMPLETA','CANCELADA') NOT NULL DEFAULT 'BORRADOR',
  observaciones VARCHAR(500),
  fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_modificacion DATETIME,
  FOREIGN KEY (id_proveedor) REFERENCES proveedores(id_proveedor)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Líneas de cada orden de compra
CREATE TABLE orden_compra_lineas (
  id_linea INT AUTO_INCREMENT PRIMARY KEY,
  id_orden_compra INT NOT NULL,
  id_producto INT NOT NULL,
  cantidad_pedida INT NOT NULL,
  cantidad_recibida INT NOT NULL DEFAULT 0,
  costo_unitario DECIMAL(10,2),
  FOREIGN KEY (id_orden_compra) REFERENCES ordenes_compra(id_orden_compra) ON DELETE CASCADE,
  FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Configuración singleton de reposición
CREATE TABLE reposicion_config (
  id INT PRIMARY KEY DEFAULT 1,
  meses_cobertura INT NOT NULL DEFAULT 2,
  peso_mes1 DECIMAL(3,2) NOT NULL DEFAULT 0.50,
  peso_mes2 DECIMAL(3,2) NOT NULL DEFAULT 0.30,
  peso_mes3 DECIMAL(3,2) NOT NULL DEFAULT 0.20,
  id_empresa_dux INT,
  id_sucursal_dux INT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO reposicion_config (id) VALUES (1);
