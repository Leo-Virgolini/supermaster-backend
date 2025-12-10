package ar.com.leo.super_master_backend.dominio.excel.service;

import ar.com.leo.super_master_backend.dominio.excel.dto.ImportCompletoResultDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ExcelService {

    /**
     * Exporta datos a un archivo Excel
     * @param tipo Tipo de exportación (catalogo, productos, precios, etc.)
     * @param canalId ID del canal (requerido para catálogo)
     * @param catalogoId ID del catálogo (requerido para catálogo)
     * @param clasifGralId ID de clasificación general primer nivel (opcional)
     * @return Bytes del archivo Excel generado
     * @throws IOException Si hay error generando el archivo
     */
    byte[] exportar(String tipo, Integer canalId, Integer catalogoId, Integer clasifGralId) throws IOException;

    /**
     * Importación única de migración: Importa TODO el Excel completo a la base de datos
     * Este método está diseñado para ser usado UNA SOLA VEZ para migrar todos los datos
     * @param file Archivo Excel completo (SUPER MASTER.xlsm)
     * @return Resultado de la importación completa con estadísticas por hoja
     * @throws IOException Si hay error leyendo el archivo
     */
    ImportCompletoResultDTO importarMigracionCompleta(MultipartFile file) throws IOException;
}

