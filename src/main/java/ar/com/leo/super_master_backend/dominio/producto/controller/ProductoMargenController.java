package ar.com.leo.super_master_backend.dominio.producto.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoMargenDTO;
import ar.com.leo.super_master_backend.dominio.producto.service.ProductoMargenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/productos/{productoId}/margen")
public class ProductoMargenController {

    private final ProductoMargenService service;

    @GetMapping
    public ResponseEntity<ProductoMargenDTO> obtener(
            @PathVariable @Positive(message = "El ID de producto debe ser positivo") Integer productoId) {
        return service.obtener(productoId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    public ResponseEntity<ProductoMargenDTO> guardar(
            @PathVariable @Positive(message = "El ID de producto debe ser positivo") Integer productoId,
            @Valid @RequestBody ProductoMargenDTO dto) {
        // Asegurar que el productoId del path coincida con el del DTO
        ProductoMargenDTO dtoConProductoId = new ProductoMargenDTO(
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
