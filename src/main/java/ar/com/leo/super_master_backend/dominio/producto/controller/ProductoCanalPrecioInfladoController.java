package ar.com.leo.super_master_backend.dominio.producto.controller;

import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCanalPrecioInfladoCreateDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCanalPrecioInfladoDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCanalPrecioInfladoUpdateDTO;
import ar.com.leo.super_master_backend.dominio.producto.service.ProductoCanalPrecioInfladoService;
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
@RequestMapping("/api/productos/{productoId}/canales/{canalId}/precios-inflados")
public class ProductoCanalPrecioInfladoController {

    private final ProductoCanalPrecioInfladoService service;

    @GetMapping
    public ResponseEntity<ProductoCanalPrecioInfladoDTO> obtener(
            @PathVariable @Positive(message = "El ID de producto debe ser positivo") Integer productoId,
            @PathVariable @Positive(message = "El ID de canal debe ser positivo") Integer canalId) {
        return ResponseEntity.ok(service.obtenerPorProductoYCanal(productoId, canalId));
    }

    @PostMapping
    public ResponseEntity<ProductoCanalPrecioInfladoDTO> crear(
            @PathVariable @Positive(message = "El ID de producto debe ser positivo") Integer productoId,
            @PathVariable @Positive(message = "El ID de canal debe ser positivo") Integer canalId,
            @Valid @RequestBody ProductoCanalPrecioInfladoCreateDTO dto) {
        if (!dto.productoId().equals(productoId) || !dto.canalId().equals(canalId)) {
            throw new IllegalArgumentException("Los IDs del path deben coincidir con los del cuerpo de la petición");
        }
        ProductoCanalPrecioInfladoDTO creado = service.crear(dto);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("")
                .build()
                .toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping
    public ResponseEntity<ProductoCanalPrecioInfladoDTO> actualizar(
            @PathVariable @Positive(message = "El ID de producto debe ser positivo") Integer productoId,
            @PathVariable @Positive(message = "El ID de canal debe ser positivo") Integer canalId,
            @Valid @RequestBody ProductoCanalPrecioInfladoUpdateDTO dto) {
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
