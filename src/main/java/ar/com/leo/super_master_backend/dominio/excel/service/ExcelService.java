package ar.com.leo.super_master_backend.dominio.excel.service;

import ar.com.leo.super_master_backend.dominio.excel.dto.ExportResultDTO;
import ar.com.leo.super_master_backend.dominio.excel.dto.ImportCompletoResultDTO;
import ar.com.leo.super_master_backend.dominio.excel.dto.ImportCostosResultDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoFilter;
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

    /**
     * Importa costos desde un archivo Excel (.xls/.xlsx) actualizando productos existentes.
     *
     * Columnas esperadas:
     * - CODIGO: sku del producto
     * - PRODUCTO: descripcion
     * - COSTO: costo del producto
     * - CODIGO EXTERNO: cod_ext
     * - PROVEEDOR: nombre del proveedor (se crea si no existe)
     * - TIPO DE PRODUCTO: "COMBO" → esCombo=true, otro → esCombo=false
     * - ULTIMA ACT. COSTO: fecha_ult_costo (formato dd/MM/yyyy)
     * - UNIDADES POR BULTO: uxb
     * - PORCENTAJE IVA: iva
     *
     * @param file Archivo Excel con los costos
     * @return Resultado de la importación con estadísticas
     * @throws IOException Si hay error leyendo el archivo
     */
    ImportCostosResultDTO importarCostos(MultipartFile file) throws IOException;

    /**
     * Exporta productos con precios a un archivo Excel (.xlsx).
     * Incluye columnas fijas del producto y columnas dinámicas por canal/cuotas.
     *
     * @param filter Filtros a aplicar (texto, marcaId, canalId, etc.)
     * @return Bytes del archivo Excel generado
     * @throws IOException Si hay error generando el archivo
     */
    byte[] exportarPrecios(ProductoFilter filter) throws IOException;

    /**
     * Construye un sufijo para el nombre del archivo basándose en los filtros aplicados.
     *
     * @param filter Filtros aplicados
     * @return Sufijo para agregar al nombre del archivo (ej: "_canal1_3cuotas")
     */
    String construirSufijoArchivoPrecios(ProductoFilter filter);

    /**
     * Exporta productos de un catálogo a un archivo Excel (.xlsx).
     * Columnas: SKU, PRODUCTO, PVP nombre_canal, UxB
     *
     * @param catalogoId ID del catálogo
     * @param canalId ID del canal para obtener el PVP
     * @param cuotas Cantidad de cuotas (0 = contado)
     * @param clasifGralId ID de clasificación general (opcional)
     * @param clasifGastroId ID de clasificación gastro (opcional)
     * @param tipoId ID del tipo (opcional)
     * @param marcaId ID de la marca (opcional)
     * @param esMaquina Filtro por máquina (opcional)
     * @param ordenarPor Campos de ordenamiento separados por coma. Valores: clasifGral, clasifGastro, tipo, marca, esMaquina
     * @return Bytes del archivo Excel generado
     * @throws IOException Si hay error generando el archivo
     */
    byte[] exportarCatalogo(Integer catalogoId, Integer canalId, Integer cuotas,
                            Integer clasifGralId, Integer clasifGastroId, Integer tipoId, Integer marcaId,
                            Boolean esMaquina, String ordenarPor) throws IOException;

    /**
     * Exporta datos para subir a Mercado Libre.
     * Usa el canal "ML" internamente.
     * Columnas: SKU, PRECIO (pvp_inflado), MLA
     *
     * @param cuotas Cantidad de cuotas (null = sin cuotas)
     * @return ExportResultDTO con el archivo y advertencias
     * @throws IOException Si hay error generando el archivo
     */
    ExportResultDTO exportarMercadoLibre(Integer cuotas) throws IOException;

    /**
     * Exporta datos para subir a Tienda Nube.
     * Usa el canal "KT HOGAR" internamente.
     * Columnas: SKU, PVP_NUBE (pvp), PVP_INFLADO (pvp_inflado)
     *
     * @param cuotas Cantidad de cuotas (null = sin cuotas)
     * @return ExportResultDTO con el archivo y advertencias
     * @throws IOException Si hay error generando el archivo
     */
    ExportResultDTO exportarNube(Integer cuotas) throws IOException;
}

