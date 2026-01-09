package ar.com.leo.super_master_backend.dominio.producto.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCanalDTO;
import ar.com.leo.super_master_backend.dominio.producto.service.ProductoCanalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/productos/{productoId}/margen")
public class ProductoCanalController {

    private final ProductoCanalService service;

    @GetMapping
    public ResponseEntity<ProductoCanalDTO> obtener(
            @PathVariable @Positive(message = "El ID de producto debe ser positivo") Integer productoId) {
        return service.obtener(productoId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    public ResponseEntity<ProductoCanalDTO> guardar(
            @PathVariable @Positive(message = "El ID de producto debe ser positivo") Integer productoId,
            @Valid @RequestBody ProductoCanalDTO dto) {
        // Asegurar que el productoId del path coincida con el del DTO
        ProductoCanalDTO dtoConProductoId = new ProductoCanalDTO(
                dto.id(),
                productoId,
                dto.margenMinorista(),
                dto.margenMayorista(),
                dto.margenFijoMinorista(),
                dto.margenFijoMayorista(),
                dto.notas()
        );
        return ResponseEntity.ok(service.guardar(dtoConProductoId));
    }

    @DeleteMapping
    public ResponseEntity<Void> eliminar(
            @PathVariable @Positive(message = "El ID de producto debe ser positivo") Integer productoId) {
        service.eliminar(productoId);
        return ResponseEntity.noContent().build();
    }

}
