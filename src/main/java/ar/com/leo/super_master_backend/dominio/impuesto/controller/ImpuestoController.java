package ar.com.leo.super_master_backend.dominio.impuesto.controller;

import ar.com.leo.super_master_backend.dominio.impuesto.dto.ImpuestoCreateDTO;
import ar.com.leo.super_master_backend.dominio.impuesto.dto.ImpuestoDTO;
import ar.com.leo.super_master_backend.dominio.impuesto.dto.ImpuestoUpdateDTO;
import ar.com.leo.super_master_backend.dominio.impuesto.service.ImpuestoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
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
    public ResponseEntity<ImpuestoDTO> obtener(@PathVariable @Positive(message = "El ID debe ser positivo") Integer id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    // -------------------------
    // CREAR
    // -------------------------
    @PostMapping
    public ResponseEntity<ImpuestoDTO> crear(@Valid @RequestBody ImpuestoCreateDTO dto) {
        ImpuestoDTO creado = service.crear(dto);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(location).body(creado);
    }

    // -------------------------
    // ACTUALIZAR
    // -------------------------
    @PutMapping("/{id}")
    public ResponseEntity<ImpuestoDTO> actualizar(
            @PathVariable @Positive(message = "El ID debe ser positivo") Integer id,
            @Valid @RequestBody ImpuestoUpdateDTO dto
    ) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    // -------------------------
    // ELIMINAR
    // -------------------------
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive(message = "El ID debe ser positivo") Integer id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

}