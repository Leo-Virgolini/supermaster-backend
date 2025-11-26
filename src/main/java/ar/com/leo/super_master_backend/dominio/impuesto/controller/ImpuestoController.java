package ar.com.leo.super_master_backend.dominio.impuesto.controller;

import ar.com.leo.super_master_backend.dominio.impuesto.dto.ImpuestoCreateDTO;
import ar.com.leo.super_master_backend.dominio.impuesto.dto.ImpuestoDTO;
import ar.com.leo.super_master_backend.dominio.impuesto.dto.ImpuestoUpdateDTO;
import ar.com.leo.super_master_backend.dominio.impuesto.service.ImpuestoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/impuestos")
public class ImpuestoController {

    private final ImpuestoService service;

    // -------------------------
    // LISTAR TODOS
    // -------------------------
    @GetMapping
    public ResponseEntity<List<ImpuestoDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    // -------------------------
    // OBTENER POR ID
    // -------------------------
    @GetMapping("/{id}")
    public ResponseEntity<ImpuestoDTO> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    // -------------------------
    // CREAR
    // -------------------------
    @PostMapping
    public ResponseEntity<ImpuestoDTO> crear(@RequestBody ImpuestoCreateDTO dto) {
        return ResponseEntity.ok(service.crear(dto));
    }

    // -------------------------
    // ACTUALIZAR
    // -------------------------
    @PutMapping("/{id}")
    public ResponseEntity<ImpuestoDTO> actualizar(
            @PathVariable Integer id,
            @RequestBody ImpuestoUpdateDTO dto
    ) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    // -------------------------
    // ELIMINAR
    // -------------------------
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

}