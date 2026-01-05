package ar.com.leo.super_master_backend.dominio.producto.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCanalDTO;
import ar.com.leo.super_master_backend.dominio.producto.service.ProductoCanalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/productos/{productoId}/canales")
public class ProductoCanalController {

    private final ProductoCanalService service;

    @GetMapping
    public ResponseEntity<List<ProductoCanalDTO>> listar(@PathVariable @Positive(message = "El ID de producto debe ser positivo") Integer productoId) {
        return ResponseEntity.ok(service.listar(productoId));
    }

    @PostMapping("/{canalId}")
    public ResponseEntity<ProductoCanalDTO> agregar(
            @PathVariable @Positive(message = "El ID de producto debe ser positivo") Integer productoId,
            @PathVariable @Positive(message = "El ID de canal debe ser positivo") Integer canalId
    ) {
        ProductoCanalDTO creado = service.agregar(productoId, canalId);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("")
                .build()
                .toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping("/{canalId}")
    public ResponseEntity<ProductoCanalDTO> actualizar(
            @PathVariable @Positive(message = "El ID de producto debe ser positivo") Integer productoId,
            @PathVariable @Positive(message = "El ID de canal debe ser positivo") Integer canalId,
            @Valid @RequestBody ProductoCanalDTO dto
    ) {
        return ResponseEntity.ok(service.actualizar(productoId, canalId, dto));
    }

    @DeleteMapping("/{canalId}")
    public ResponseEntity<Void> eliminar(
            @PathVariable @Positive(message = "El ID de producto debe ser positivo") Integer productoId,
            @PathVariable @Positive(message = "El ID de canal debe ser positivo") Integer canalId
    ) {
        service.eliminar(productoId, canalId);
        return ResponseEntity.noContent().build();
    }

}