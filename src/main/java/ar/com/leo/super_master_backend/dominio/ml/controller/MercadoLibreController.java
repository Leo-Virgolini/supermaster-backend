package ar.com.leo.super_master_backend.dominio.ml.controller;

import ar.com.leo.super_master_backend.dominio.ml.dto.ConfiguracionMlDTO;
import ar.com.leo.super_master_backend.dominio.ml.dto.CostoEnvioMasivoResponseDTO;
import ar.com.leo.super_master_backend.dominio.ml.dto.CostoEnvioResponseDTO;
import ar.com.leo.super_master_backend.dominio.ml.dto.CostoVentaResponseDTO;
import ar.com.leo.super_master_backend.dominio.ml.service.ConfiguracionMlService;
import ar.com.leo.super_master_backend.dominio.ml.service.MercadoLibreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ml")
public class MercadoLibreController {

    private final MercadoLibreService mercadoLibreService;
    private final ConfiguracionMlService configuracionMlService;

    /**
     * Calcula y guarda el costo de envío para productos de ML.
     *
     * Prioridad de parámetros:
     * 1. Si se proporciona 'mla', calcula solo para ese MLA.
     * 2. Si se proporciona 'productoId', busca el MLA asociado al producto y calcula.
     * 3. Si no se proporciona ninguno, calcula para todos los MLAs en la base de datos.
     *
     * Lógica de cálculo:
     * - PVP >= umbral ($33,000): Consulta API ML para obtener costo real de envío gratis
     * - PVP < umbral: Usa tiers fijos ($1,115 / $2,300 / $2,810)
     *
     * El cálculo es iterativo hasta que el costo de envío se estabilice.
     *
     * @param mla (Opcional) Código MLA del producto (ej: MLA123456789)
     * @param productoId (Opcional) ID del producto para buscar su MLA asociado
     * @return DTO con el costo de envío calculado o resumen del procesamiento masivo
     */
    @PostMapping("/costo-envio")
    public ResponseEntity<?> calcularCostoEnvio(
            @RequestParam(required = false) String mla,
            @RequestParam(required = false) Integer productoId) {

        // Si se proporciona MLA, calcular solo para ese producto
        if (mla != null && !mla.isBlank()) {
            CostoEnvioResponseDTO response = mercadoLibreService.calcularCostoEnvioGratis(mla);
            return ResponseEntity.ok(response);
        }

        // Si se proporciona productoId, buscar su MLA y calcular
        if (productoId != null) {
            CostoEnvioResponseDTO response = mercadoLibreService.calcularCostoEnvioPorProducto(productoId);
            return ResponseEntity.ok(response);
        }

        // Si no se proporciona ninguno, calcular para todos
        CostoEnvioMasivoResponseDTO response = mercadoLibreService.calcularCostoEnvioTodos();
        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene los costos de venta (comisiones) de un producto en MercadoLibre.
     *
     * Parámetros (al menos uno requerido):
     * - 'mla': Código MLA del producto (ej: MLA123456789)
     * - 'productoId': ID del producto para buscar su MLA asociado
     *
     * @param mla (Opcional) Código MLA del producto
     * @param productoId (Opcional) ID del producto
     * @return DTO con los costos de venta (comisión, costo fijo, total)
     */
    @GetMapping("/costo-venta")
    public ResponseEntity<CostoVentaResponseDTO> obtenerCostoVenta(
            @RequestParam(required = false) String mla,
            @RequestParam(required = false) Integer productoId) {

        // Si se proporciona MLA
        if (mla != null && !mla.isBlank()) {
            return ResponseEntity.ok(mercadoLibreService.obtenerCostoVenta(mla));
        }

        // Si se proporciona productoId
        if (productoId != null) {
            return ResponseEntity.ok(mercadoLibreService.obtenerCostoVentaPorProducto(productoId));
        }

        // Si no se proporciona ninguno, devolver error
        return ResponseEntity.badRequest().body(
                new CostoVentaResponseDTO(null, null, null, null, null, null, null,
                        "Debe proporcionar 'mla' o 'productoId'"));
    }

    // =====================================================
    // CONFIGURACION
    // =====================================================

    @GetMapping("/configuracion")
    public ResponseEntity<ConfiguracionMlDTO> obtenerConfiguracion() {
        return ResponseEntity.ok(configuracionMlService.obtener());
    }

    @PutMapping("/configuracion")
    public ResponseEntity<ConfiguracionMlDTO> actualizarConfiguracion(
            @Valid @RequestBody ConfiguracionMlDTO dto) {
        return ResponseEntity.ok(configuracionMlService.actualizar(dto));
    }
}
