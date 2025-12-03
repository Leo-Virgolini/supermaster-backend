package ar.com.leo.super_master_backend.dominio.excel.controller;

import ar.com.leo.super_master_backend.dominio.excel.dto.ImportCompletoResultDTO;
import ar.com.leo.super_master_backend.dominio.excel.service.ExcelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/excel")
public class ExcelController {

    private final ExcelService excelService;

    /**
     * Exporta datos a un archivo Excel
     * 
     * @param tipo Tipo de exportación: "catalogo", "productos", "precios", etc.
     * @param canalId ID del canal (requerido para catálogo)
     * @param catalogoId ID del catálogo (requerido para catálogo)
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
    
}

