CREATE TABLE venta_diaria_cache (
  id INT AUTO_INCREMENT PRIMARY KEY,
  sku VARCHAR(45) NOT NULL,
  fecha DATE NOT NULL,
  cantidad INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_sku_fecha (sku, fecha),
  INDEX idx_fecha (fecha)
);

ALTER TABLE reposicion_config ADD COLUMN ultimo_ventas_fetch DATE DEFAULT NULL;
ALTER TABLE reposicion_config ADD COLUMN sucursales_hash VARCHAR(64) DEFAULT NULL;
