package ar.com.leo.super_master_backend.dominio.concepto_gasto.controller;

import ar.com.leo.super_master_backend.dominio.concepto_gasto.dto.ConceptoGastoCreateDTO;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.dto.ConceptoGastoDTO;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.dto.ConceptoGastoUpdateDTO;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.service.ConceptoGastoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/conceptos-gastos")
public class ConceptoGastoController {

    private final ConceptoGastoService service;

    @GetMapping
    public ResponseEntity<List<ConceptoGastoDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConceptoGastoDTO> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @PostMapping
    public ResponseEntity<ConceptoGastoDTO> crear(@RequestBody ConceptoGastoCreateDTO dto) {
        return ResponseEntity.ok(service.crear(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConceptoGastoDTO> actualizar(
            @PathVariable Integer id,
            @RequestBody ConceptoGastoUpdateDTO dto
    ) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

}