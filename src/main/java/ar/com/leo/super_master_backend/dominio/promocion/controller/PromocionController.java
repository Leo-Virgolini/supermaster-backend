package ar.com.leo.super_master_backend.dominio.promocion.controller;

import ar.com.leo.super_master_backend.dominio.promocion.dto.PromocionCreateDTO;
import ar.com.leo.super_master_backend.dominio.promocion.dto.PromocionDTO;
import ar.com.leo.super_master_backend.dominio.promocion.dto.PromocionUpdateDTO;
import ar.com.leo.super_master_backend.dominio.promocion.service.PromocionService;
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
@RequestMapping("/api/promociones")
public class PromocionController {

    private final PromocionService service;

    @GetMapping
    public ResponseEntity<List<PromocionDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromocionDTO> obtenerPorId(
            @PathVariable @Positive(message = "El ID debe ser positivo") Integer id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<PromocionDTO> obtenerPorCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(service.obtenerPorCodigo(codigo));
    }

    @PostMapping
    public ResponseEntity<PromocionDTO> crear(@Valid @RequestBody PromocionCreateDTO dto) {
        PromocionDTO creado = service.crear(dto);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromocionDTO> actualizar(
            @PathVariable @Positive(message = "El ID debe ser positivo") Integer id,
            @Valid @RequestBody PromocionUpdateDTO dto) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable @Positive(message = "El ID debe ser positivo") Integer id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
