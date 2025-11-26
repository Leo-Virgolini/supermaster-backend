package ar.com.leo.super_master_backend.dominio.producto.controller;

import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCanalPrecioDTO;
import ar.com.leo.super_master_backend.dominio.producto.service.ProductoCanalPrecioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/productos/{productoId}/canales/{canalId}/precio")
public class ProductoCanalPrecioController {

    private final ProductoCanalPrecioService service;

    @GetMapping
    public ResponseEntity<ProductoCanalPrecioDTO> obtener(
            @PathVariable Integer productoId,
            @PathVariable Integer canalId
    ) {
        return ResponseEntity.ok(service.obtener(productoId, canalId));
    }

    @PostMapping("/recalcular")
    public ResponseEntity<ProductoCanalPrecioDTO> recalcular(
            @PathVariable Integer productoId,
            @PathVariable Integer canalId
    ) {
        return ResponseEntity.ok(service.recalcular(productoId, canalId));
    }

}