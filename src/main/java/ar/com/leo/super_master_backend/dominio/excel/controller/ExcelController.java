package ar.com.leo.super_master_backend.dominio.excel.controller;

import ar.com.leo.super_master_backend.dominio.excel.dto.ExportResultDTO;
import ar.com.leo.super_master_backend.dominio.excel.dto.ImportCompletoResultDTO;
import ar.com.leo.super_master_backend.dominio.excel.dto.ImportCostosResultDTO;
import ar.com.leo.super_master_backend.dominio.excel.service.ExcelService;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/excel")
public class ExcelController {

    private final ExcelService excelService;

    /**
     * Exporta datos a un archivo Excel
     *
     * @param tipo         Tipo de exportación: "catalogo", "productos", "precios", etc.
     * @param canalId      ID del canal (requerido para catálogo)
     * @param catalogoId   ID del catálogo (requerido para catálogo)
     * @param clasifGralId ID de clasificación general primer nivel (opcional)
     * @return Archivo Excel descargable
     */
    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(
            @RequestParam("tipo") String tipo,
            @RequestParam(value = "canalId", required = false) Integer canalId,
            @RequestParam(value = "catalogoId", required = false) Integer catalogoId,
            @RequestParam(value = "clasifGralId", required = false) Integer clasifGralId
    ) {
        try {
            byte[] excelBytes = excelService.exportar(tipo, canalId, catalogoId, clasifGralId);

            String filename = String.format("%s_%s.xlsx",
                    tipo,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelBytes.length);

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Importación única de migración: Importa TODO el Excel completo a la base de datos
     * Este endpoint está diseñado para ser usado UNA SOLA VEZ para migrar todos los datos
     * del archivo SUPER MASTER.xlsm a la base de datos MySQL
     *
     * @param file Archivo Excel completo (SUPER MASTER.xlsm)
     * @return Resultado de la importación completa con estadísticas por hoja
     */
    @PostMapping("/importar-migracion")
    public ResponseEntity<ImportCompletoResultDTO> importarMigracion(
            @RequestParam("archivo") MultipartFile file
    ) {
        try {
            ImportCompletoResultDTO resultado = excelService.importarMigracionCompleta(file);
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            log.error("Error de validación al importar migración: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                    ImportCompletoResultDTO.withErrors(0, 0, 1,
                            new java.util.HashMap<>(),
                            java.util.List.of(e.getMessage()))
            );
        } catch (IOException e) {
            log.error("Error de I/O al importar migración: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ImportCompletoResultDTO.withErrors(0, 0, 1,
                            new java.util.HashMap<>(),
                            java.util.List.of("Error de lectura/escritura: " + e.getMessage()))
            );
        } catch (Exception e) {
            log.error("Error inesperado al importar migración: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ImportCompletoResultDTO.withErrors(0, 0, 1,
                            new java.util.HashMap<>(),
                            java.util.List.of("Error inesperado: " + e.getMessage()))
            );
        }
    }

    /**
     * Importa costos desde un archivo Excel (.xls/.xlsx) actualizando productos existentes.
     * <p>
     * Columnas esperadas:
     * - CODIGO: sku del producto (requerido)
     * - PRODUCTO: descripcion
     * - COSTO: costo del producto
     * - CODIGO EXTERNO: cod_ext
     * - PROVEEDOR: nombre del proveedor (se crea si no existe)
     * - TIPO DE PRODUCTO: "COMBO" o "SIMPLE"
     * - ULTIMA ACT. COSTO: fecha (formato dd/MM/yyyy)
     * - UNIDADES POR BULTO: uxb
     * - PORCENTAJE IVA: iva
     *
     * @param file Archivo Excel con los costos
     * @return Resultado de la importación con estadísticas
     */
    @PostMapping("/importar-costos")
    public ResponseEntity<ImportCostosResultDTO> importarCostos(
            @RequestParam("archivo") MultipartFile file
    ) {
        try {
            ImportCostosResultDTO resultado = excelService.importarCostos(file);
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            log.error("Error de validación al importar costos: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                    ImportCostosResultDTO.withErrors(List.of(e.getMessage()))
            );
        } catch (IOException e) {
            log.error("Error de I/O al importar costos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ImportCostosResultDTO.withErrors(List.of("Error de lectura: " + e.getMessage()))
            );
        } catch (Exception e) {
            log.error("Error inesperado al importar costos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ImportCostosResultDTO.withErrors(List.of("Error inesperado: " + e.getMessage()))
            );
        }
    }

    /**
     * Exporta productos con precios a un archivo Excel (.xlsx).
     * Incluye columnas fijas del producto y columnas dinámicas por canal/cuotas.
     * Headers en mayúsculas, negrita y fondo gris claro. Bordes finos en toda la tabla.
     * Acepta los mismos filtros que GET /api/productos/precios.
     *
     * @return Archivo Excel descargable
     */
    @GetMapping("/exportar-precios")
    public ResponseEntity<?> exportarPrecios(
            // 0) ID
            @RequestParam(required = false) Integer productoId,

            // 1) TEXTO
            @RequestParam(required = false) String texto,

            // 2) BOOLEANOS / NUMÉRICOS
            @RequestParam(required = false) Boolean esCombo,
            @RequestParam(required = false) Integer uxb,
            @RequestParam(required = false) Boolean esMaquina,
            @RequestParam(required = false) Boolean tieneMla,
            @RequestParam(required = false) Boolean activo,

            // 3) MANY-TO-ONE
            @RequestParam(required = false) Integer marcaId,
            @RequestParam(required = false) Integer origenId,
            @RequestParam(required = false) Integer tipoId,
            @RequestParam(required = false) Integer clasifGralId,
            @RequestParam(required = false) Integer clasifGastroId,
            @RequestParam(required = false) Integer proveedorId,
            @RequestParam(required = false) Integer materialId,

            // 4) RANGOS (costo / IVA / stock)
            @RequestParam(required = false) BigDecimal costoMin,
            @RequestParam(required = false) BigDecimal costoMax,
            @RequestParam(required = false) BigDecimal ivaMin,
            @RequestParam(required = false) BigDecimal ivaMax,
            @RequestParam(required = false) Integer stockMin,
            @RequestParam(required = false) Integer stockMax,

            // 5) RANGO PVP
            @RequestParam(required = false) BigDecimal pvpMin,
            @RequestParam(required = false) BigDecimal pvpMax,
            @RequestParam(required = false) Integer pvpCanalId,

            // 6) FECHAS
            @RequestParam(required = false) LocalDate desdeFechaUltCosto,
            @RequestParam(required = false) LocalDate hastaFechaUltCosto,
            @RequestParam(required = false) LocalDate desdeFechaCreacion,
            @RequestParam(required = false) LocalDate hastaFechaCreacion,
            @RequestParam(required = false) LocalDate desdeFechaModificacion,
            @RequestParam(required = false) LocalDate hastaFechaModificacion,

            // 7) MANY-TO-MANY
            @RequestParam(required = false) List<Integer> aptoIds,
            @RequestParam(required = false) List<Integer> canalIds,
            @RequestParam(required = false) List<Integer> catalogoIds,
            @RequestParam(required = false) List<Integer> clienteIds,
            @RequestParam(required = false) List<Integer> mlaIds,

            // 8) ORDENAMIENTO ESPECIAL
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(required = false) Integer sortCanalId,

            // 9) FILTRAR PRECIOS POR CANAL
            @RequestParam(required = false) Integer canalId,

            // 10) FILTRAR PRECIOS POR CUOTAS
            @RequestParam(required = false) Integer cuotas
    ) {
        try {
            ProductoFilter filter = new ProductoFilter(
                    productoId,
                    texto,
                    esCombo,
                    uxb,
                    esMaquina,
                    tieneMla,
                    activo,
                    marcaId,
                    origenId,
                    tipoId,
                    clasifGralId,
                    clasifGastroId,
                    proveedorId,
                    materialId,
                    costoMin,
                    costoMax,
                    ivaMin,
                    ivaMax,
                    stockMin,
                    stockMax,
                    pvpMin,
                    pvpMax,
                    pvpCanalId,
                    desdeFechaUltCosto,
                    hastaFechaUltCosto,
                    desdeFechaCreacion,
                    hastaFechaCreacion,
                    desdeFechaModificacion,
                    hastaFechaModificacion,
                    aptoIds,
                    canalIds,
                    catalogoIds,
                    clienteIds,
                    mlaIds,
                    sortBy,
                    sortDir,
                    sortCanalId,
                    canalId,
                    cuotas
            );

            byte[] excelBytes = excelService.exportarPrecios(filter);

            String filename = String.format("precios_%s.xlsx",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelBytes.length);

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            log.error("Error de validación al exportar precios: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", e.getMessage(), "path", "/api/excel/exportar-precios"));
        } catch (IOException e) {
            log.error("Error de I/O al exportar precios: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", e.getMessage(), "path", "/api/excel/exportar-precios"));
        } catch (Exception e) {
            log.error("Error inesperado al exportar precios: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", e.getMessage(), "path", "/api/excel/exportar-precios"));
        }
    }

    /**
     * Exporta productos de un catálogo a un archivo Excel (.xlsx).
     * Columnas: SKU, PRODUCTO, PVP (nombre del canal), UxB
     *
     * @param catalogoId ID del catálogo (requerido)
     * @param canalId    ID del canal para obtener el PVP (requerido)
     * @param cuotas     Cantidad de cuotas (0 = contado, por defecto)
     * @param clasifGralId ID de clasificación general (opcional)
     * @param clasifGastroId ID de clasificación gastro (opcional)
     * @param tipoId ID del tipo (opcional)
     * @param marcaId ID de la marca (opcional)
     * @param esMaquina Filtro por máquina (opcional)
     * @param ordenarPor Campos de ordenamiento separados por coma (opcional). Valores: clasifGral, clasifGastro, tipo, marca, esMaquina
     * @return Archivo Excel descargable
     */
    @GetMapping("/exportar-catalogo")
    public ResponseEntity<?> exportarCatalogo(
            @RequestParam("catalogoId") Integer catalogoId,
            @RequestParam("canalId") Integer canalId,
            @RequestParam(value = "cuotas", required = false, defaultValue = "0") Integer cuotas,
            @RequestParam(value = "clasifGralId", required = false) Integer clasifGralId,
            @RequestParam(value = "clasifGastroId", required = false) Integer clasifGastroId,
            @RequestParam(value = "tipoId", required = false) Integer tipoId,
            @RequestParam(value = "marcaId", required = false) Integer marcaId,
            @RequestParam(value = "esMaquina", required = false) Boolean esMaquina,
            @RequestParam(value = "ordenarPor", required = false) String ordenarPor
    ) {
        try {
            byte[] excelBytes = excelService.exportarCatalogo(catalogoId, canalId, cuotas, clasifGralId, clasifGastroId, tipoId, marcaId, esMaquina, ordenarPor);

            String filename = String.format("catalogo_%d_%s.xlsx",
                    catalogoId,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelBytes.length);

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            log.error("Error de validación al exportar catálogo: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", e.getMessage(), "path", "/api/excel/exportar-catalogo"));
        } catch (IOException e) {
            log.error("Error de I/O al exportar catálogo: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", e.getMessage(), "path", "/api/excel/exportar-catalogo"));
        } catch (Exception e) {
            log.error("Error inesperado al exportar catálogo: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", e.getMessage(), "path", "/api/excel/exportar-catalogo"));
        }
    }

    /**
     * Exporta datos para subir precios a Mercado Libre.
     * Usa el canal "ML" internamente.
     * Columnas: SKU, PRECIO (pvp_inflado), MLA
     * Si hay productos sin MLA o con precio inválido, se excluyen del archivo
     * y se devuelven en el header "X-Advertencias".
     *
     * @param cuotas Cantidad de cuotas (null = sin cuotas)
     * @return Archivo Excel descargable con header de advertencias si aplica
     */
    @GetMapping("/exportar-mercadolibre")
    public ResponseEntity<?> exportarMercadoLibre(
            @RequestParam(value = "cuotas", required = false) Integer cuotas
    ) {
        try {
            ExportResultDTO result = excelService.exportarMercadoLibre(cuotas);

            String filename = String.format("mercadolibre_%s.xlsx",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(result.archivo().length);

            // Agregar advertencias en header si existen
            if (result.tieneAdvertencias()) {
                headers.add("X-Advertencias", String.join(" | ", result.advertencias()));
                headers.add("Access-Control-Expose-Headers", "X-Advertencias");
            }

            return new ResponseEntity<>(result.archivo(), headers, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            log.error("Error de validación al exportar para Mercado Libre: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", e.getMessage(), "path", "/api/excel/exportar-mercadolibre"));
        } catch (IOException e) {
            log.error("Error de I/O al exportar para Mercado Libre: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", e.getMessage(), "path", "/api/excel/exportar-mercadolibre"));
        } catch (Exception e) {
            log.error("Error inesperado al exportar para Mercado Libre: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", e.getMessage(), "path", "/api/excel/exportar-mercadolibre"));
        }
    }

    /**
     * Exporta datos para subir precios a Tienda Nube.
     * Usa el canal "KT HOGAR" internamente.
     * Columnas: SKU, PVP_NUBE (pvp), PVP_INFLADO (pvp_inflado)
     * Si hay productos con precio inválido, se excluyen del archivo
     * y se devuelven en el header "X-Advertencias".
     *
     * @param cuotas Cantidad de cuotas (null = sin cuotas)
     * @return Archivo Excel descargable con header de advertencias si aplica
     */
    @GetMapping("/exportar-nube")
    public ResponseEntity<?> exportarNube(
            @RequestParam(value = "cuotas", required = false) Integer cuotas
    ) {
        try {
            ExportResultDTO result = excelService.exportarNube(cuotas);

            String filename = String.format("nube_%s.xlsx",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(result.archivo().length);

            // Agregar advertencias en header si existen
            if (result.tieneAdvertencias()) {
                headers.add("X-Advertencias", String.join(" | ", result.advertencias()));
                headers.add("Access-Control-Expose-Headers", "X-Advertencias");
            }

            return new ResponseEntity<>(result.archivo(), headers, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            log.error("Error de validación al exportar para Tienda Nube: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", e.getMessage(), "path", "/api/excel/exportar-nube"));
        } catch (IOException e) {
            log.error("Error de I/O al exportar para Tienda Nube: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", e.getMessage(), "path", "/api/excel/exportar-nube"));
        } catch (Exception e) {
            log.error("Error inesperado al exportar para Tienda Nube: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", e.getMessage(), "path", "/api/excel/exportar-nube"));
        }
    }
}

