package ar.com.leo.super_master_backend.dominio.ml.controller;

import ar.com.leo.super_master_backend.dominio.ml.dto.ConfiguracionMlDTO;
import ar.com.leo.super_master_backend.dominio.ml.dto.CostoEnvioMasivoResponseDTO;
import ar.com.leo.super_master_backend.dominio.ml.dto.CostoEnvioResponseDTO;
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
     * Si se proporciona el parámetro 'mla', calcula solo para ese producto.
     * Si no se proporciona 'mla', calcula para todos los MLAs en la base de datos.
     *
     * Lógica de cálculo:
     * - PVP >= umbral ($33,000): Consulta API ML para obtener costo real de envío gratis
     * - PVP < umbral: Usa tiers fijos ($1,115 / $2,300 / $2,810)
     *
     * El cálculo es iterativo hasta que el costo de envío se estabilice.
     *
     * @param mla (Opcional) Código MLA del producto (ej: MLA123456789)
     * @return DTO con el costo de envío calculado o resumen del procesamiento masivo
     */
    @PostMapping("/costo-envio")
    public ResponseEntity<?> calcularCostoEnvio(@RequestParam(required = false) String mla) {

        // Si se proporciona MLA, calcular solo para ese producto
        if (mla != null && !mla.isBlank()) {
            CostoEnvioResponseDTO response = mercadoLibreService.calcularCostoEnvioGratis(mla);
            return ResponseEntity.ok(response);
        }

        // Si no se proporciona MLA, calcular para todos
        CostoEnvioMasivoResponseDTO response = mercadoLibreService.calcularCostoEnvioTodos();
        return ResponseEntity.ok(response);
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
