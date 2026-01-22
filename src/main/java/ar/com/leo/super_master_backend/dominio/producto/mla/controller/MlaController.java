package ar.com.leo.super_master_backend.dominio.producto.mla.controller;

import ar.com.leo.super_master_backend.dominio.producto.mla.dto.MlaDTO;
import ar.com.leo.super_master_backend.dominio.producto.mla.service.MlaService;
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
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoResumenDTO;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mlas")
public class MlaController {

    private final MlaService mlaService;

    @GetMapping
    public ResponseEntity<Page<MlaDTO>> listar(@RequestParam(required = false) String search, Pageable pageable) {
        return ResponseEntity.ok(mlaService.listar(search, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MlaDTO> obtener(
            @PathVariable @Positive(message = "El ID debe ser positivo") Integer id) {
        return ResponseEntity.ok(mlaService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<MlaDTO> crear(@Valid @RequestBody MlaDTO dto) {
        MlaDTO creado = mlaService.crear(dto);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MlaDTO> actualizar(
            @PathVariable @Positive(message = "El ID debe ser positivo") Integer id,
            @Valid @RequestBody MlaDTO dto) {
        return ResponseEntity.ok(mlaService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable @Positive(message = "El ID debe ser positivo") Integer id) {
        mlaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/productos")
    public ResponseEntity<List<ProductoResumenDTO>> listarProductos(
            @PathVariable @Positive(message = "El ID debe ser positivo") Integer id) {
        return ResponseEntity.ok(mlaService.listarProductos(id));
    }
}
