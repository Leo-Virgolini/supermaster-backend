package ar.com.leo.super_master_backend.dominio.producto.controller;

import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCanalPromocionCreateDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCanalPromocionDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCanalPromocionUpdateDTO;
import ar.com.leo.super_master_backend.dominio.producto.service.ProductoCanalPromocionService;
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
@RequestMapping("/api/productos/{productoId}/canales/{canalId}/promociones")
public class ProductoCanalPromocionController {

    private final ProductoCanalPromocionService service;

    @GetMapping
    public ResponseEntity<ProductoCanalPromocionDTO> obtener(
            @PathVariable @Positive(message = "El ID de producto debe ser positivo") Integer productoId,
            @PathVariable @Positive(message = "El ID de canal debe ser positivo") Integer canalId) {
        return ResponseEntity.ok(service.obtenerPorProductoYCanal(productoId, canalId));
    }

    @PostMapping
    public ResponseEntity<ProductoCanalPromocionDTO> crear(
            @PathVariable @Positive(message = "El ID de producto debe ser positivo") Integer productoId,
            @PathVariable @Positive(message = "El ID de canal debe ser positivo") Integer canalId,
            @Valid @RequestBody ProductoCanalPromocionCreateDTO dto) {
        // Asegurar que los IDs del path coincidan con los del DTO
        if (!dto.productoId().equals(productoId) || !dto.canalId().equals(canalId)) {
            throw new IllegalArgumentException("Los IDs del path deben coincidir con los del cuerpo de la petición");
        }
        ProductoCanalPromocionDTO creado = service.crear(dto);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("")
                .build()
                .toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping
    public ResponseEntity<ProductoCanalPromocionDTO> actualizar(
            @PathVariable @Positive(message = "El ID de producto debe ser positivo") Integer productoId,
            @PathVariable @Positive(message = "El ID de canal debe ser positivo") Integer canalId,
            @Valid @RequestBody ProductoCanalPromocionUpdateDTO dto) {
        return ResponseEntity.ok(service.actualizar(productoId, canalId, dto));
    }

    @DeleteMapping
    public ResponseEntity<Void> eliminar(
            @PathVariable @Positive(message = "El ID de producto debe ser positivo") Integer productoId,
            @PathVariable @Positive(message = "El ID de canal debe ser positivo") Integer canalId) {
        service.eliminar(productoId, canalId);
        return ResponseEntity.noContent().build();
    }
}
