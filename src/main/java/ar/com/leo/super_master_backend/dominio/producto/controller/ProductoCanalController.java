package ar.com.leo.super_master_backend.dominio.producto.controller;

import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCanalDTO;
import ar.com.leo.super_master_backend.dominio.producto.service.ProductoCanalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/productos/{productoId}/canales")
public class ProductoCanalController {

    private final ProductoCanalService service;

    @GetMapping
    public ResponseEntity<List<ProductoCanalDTO>> listar(@PathVariable Integer productoId) {
        return ResponseEntity.ok(service.listar(productoId));
    }

    @PostMapping("/{canalId}")
    public ResponseEntity<ProductoCanalDTO> agregar(
            @PathVariable Integer productoId,
            @PathVariable Integer canalId
    ) {
        return ResponseEntity.ok(service.agregar(productoId, canalId));
    }

    @PutMapping("/{canalId}")
    public ResponseEntity<ProductoCanalDTO> actualizar(
            @PathVariable Integer productoId,
            @PathVariable Integer canalId,
            @RequestBody ProductoCanalDTO dto
    ) {
        return ResponseEntity.ok(service.actualizar(productoId, canalId, dto));
    }

    @DeleteMapping("/{canalId}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Integer productoId,
            @PathVariable Integer canalId
    ) {
        service.eliminar(productoId, canalId);
        return ResponseEntity.noContent().build();
    }

}