package ar.com.leo.super_master_backend.dominio.producto.controller;

import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoAptoDTO;
import ar.com.leo.super_master_backend.dominio.producto.service.ProductoAptoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/productos/{productoId}/aptos")
public class ProductoAptoController {

    private final ProductoAptoService productoAptoService;

    @GetMapping
    public ResponseEntity<List<ProductoAptoDTO>> listar(@PathVariable Integer productoId) {
        return ResponseEntity.ok(productoAptoService.listar(productoId));
    }

    @PostMapping("/{aptoId}")
    public ResponseEntity<ProductoAptoDTO> agregar(
            @PathVariable Integer productoId,
            @PathVariable Integer aptoId
    ) {
        return ResponseEntity.ok(productoAptoService.agregar(productoId, aptoId));
    }

    @DeleteMapping("/{aptoId}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Integer productoId,
            @PathVariable Integer aptoId
    ) {
        productoAptoService.eliminar(productoId, aptoId);
        return ResponseEntity.noContent().build();
    }

}