package ar.com.leo.super_master_backend.dominio.canal.controller;

import ar.com.leo.super_master_backend.dominio.canal.dto.CanalConceptoDTO;
import ar.com.leo.super_master_backend.dominio.canal.service.CanalConceptoService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/canales/{canalId}/conceptos")
public class CanalConceptoController {

    private final CanalConceptoService canalConceptoService;

    // ==========================================
    // LISTAR CONCEPTOS DEL CANAL
    // ==========================================
    @GetMapping
    @PreAuthorize("hasAuthority('CANALES_VER')")
    public ResponseEntity<List<CanalConceptoDTO>> listar(
            @PathVariable @Positive Integer canalId) {
        return ResponseEntity.ok(canalConceptoService.listarPorCanal(canalId));
    }

    // ==========================================
    // ASIGNAR CONCEPTO AL CANAL
    // ==========================================
    @PostMapping("/{conceptoId}")
    @PreAuthorize("hasAuthority('CANALES_EDITAR')")
    public ResponseEntity<CanalConceptoDTO> asignar(
            @PathVariable @Positive Integer canalId,
            @PathVariable @Positive Integer conceptoId) {
        return ResponseEntity.ok(canalConceptoService.asignarConcepto(canalId, conceptoId));
    }

    // ==========================================
    // QUITAR CONCEPTO DEL CANAL
    // ==========================================
    @DeleteMapping("/{conceptoId}")
    @PreAuthorize("hasAuthority('CANALES_EDITAR')")
    public ResponseEntity<Void> eliminar(
            @PathVariable @Positive Integer canalId,
            @PathVariable @Positive Integer conceptoId) {
        canalConceptoService.eliminarConcepto(canalId, conceptoId);
        return ResponseEntity.noContent().build();
    }
}
