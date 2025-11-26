package ar.com.leo.super_master_backend.dominio.producto.controller;

import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCatalogoDTO;
import ar.com.leo.super_master_backend.dominio.producto.service.ProductoCatalogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/productos/{productoId}/catalogos")
public class ProductoCatalogoController {

    private final ProductoCatalogoService service;

    @GetMapping
    public ResponseEntity<List<ProductoCatalogoDTO>> listar(@PathVariable Integer productoId) {
        return ResponseEntity.ok(service.listar(productoId));
    }

    @PostMapping("/{catalogoId}")
    public ResponseEntity<ProductoCatalogoDTO> agregar(
            @PathVariable Integer productoId,
            @PathVariable Integer catalogoId
    ) {
        return ResponseEntity.ok(service.agregar(productoId, catalogoId));
    }

    @DeleteMapping("/{catalogoId}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Integer productoId,
            @PathVariable Integer catalogoId
    ) {
        service.eliminar(productoId, catalogoId);
        return ResponseEntity.noContent().build();
    }

}