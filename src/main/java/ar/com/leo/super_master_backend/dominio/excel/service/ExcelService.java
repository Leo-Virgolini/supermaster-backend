package ar.com.leo.super_master_backend.dominio.excel.service;

import ar.com.leo.super_master_backend.dominio.excel.dto.ImportCompletoResultDTO;
import ar.com.leo.super_master_backend.dominio.excel.dto.ImportResultDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ExcelService {

    /**
     * Importa datos desde un archivo Excel
     * @param file Archivo Excel a importar
     * @param tipo Tipo de importación (productos, precios, etc.)
     * @return Resultado de la importación con estadísticas
     * @throws IOException Si hay error leyendo el archivo
     */
    ImportResultDTO importar(MultipartFile file, String tipo) throws IOException;

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
     * Importa todo el archivo Excel completo, procesando todas las hojas
     * @param file Archivo Excel a importar
     * @return Resultado de la importación completa con estadísticas por hoja
     * @throws IOException Si hay error leyendo el archivo
     */
    ImportCompletoResultDTO importarCompleto(MultipartFile file) throws IOException;

    /**
     * Importación única de migración: Importa TODO el Excel completo a la base de datos
     * Este método está diseñado para ser usado UNA SOLA VEZ para migrar todos los datos
     * @param file Archivo Excel completo (SUPER MASTER.xlsm)
     * @return Resultado de la importación completa con estadísticas por hoja
     * @throws IOException Si hay error leyendo el archivo
     */
    ImportCompletoResultDTO importarMigracionCompleta(MultipartFile file) throws IOException;
}

