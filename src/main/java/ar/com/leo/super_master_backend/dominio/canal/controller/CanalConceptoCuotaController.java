package ar.com.leo.super_master_backend.dominio.canal.controller;

import ar.com.leo.super_master_backend.dominio.canal.dto.CanalConceptoCuotaCreateDTO;
import ar.com.leo.super_master_backend.dominio.canal.dto.CanalConceptoCuotaDTO;
import ar.com.leo.super_master_backend.dominio.canal.dto.CanalConceptoCuotaUpdateDTO;
import ar.com.leo.super_master_backend.dominio.canal.service.CanalConceptoCuotaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/canal-concepto-cuotas")
public class CanalConceptoCuotaController {

    private final CanalConceptoCuotaService service;

    @GetMapping
    public ResponseEntity<Page<CanalConceptoCuotaDTO>> listar(Pageable pageable) {
        return ResponseEntity.ok(service.listar(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CanalConceptoCuotaDTO> obtener(
            @PathVariable @Positive(message = "El ID debe ser positivo") Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @GetMapping("/canal/{canalId}")
    public ResponseEntity<List<CanalConceptoCuotaDTO>> listarPorCanal(
            @PathVariable @Positive(message = "El ID del canal debe ser positivo") Integer canalId) {
        return ResponseEntity.ok(service.listarPorCanal(canalId));
    }

    @GetMapping("/canal/{canalId}/cuotas/{cuotas}")
    public ResponseEntity<List<CanalConceptoCuotaDTO>> listarPorCanalYCuotas(
            @PathVariable @Positive(message = "El ID del canal debe ser positivo") Integer canalId,
            @PathVariable @Positive(message = "El número de cuotas debe ser positivo") Integer cuotas) {
        return ResponseEntity.ok(service.listarPorCanalYCuotas(canalId, cuotas));
    }

    @PostMapping
    public ResponseEntity<CanalConceptoCuotaDTO> crear(@Valid @RequestBody CanalConceptoCuotaCreateDTO dto) {
        CanalConceptoCuotaDTO creado = service.crear(dto);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CanalConceptoCuotaDTO> actualizar(
            @PathVariable @Positive(message = "El ID debe ser positivo") Long id,
            @Valid @RequestBody CanalConceptoCuotaUpdateDTO dto
    ) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable @Positive(message = "El ID debe ser positivo") Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

