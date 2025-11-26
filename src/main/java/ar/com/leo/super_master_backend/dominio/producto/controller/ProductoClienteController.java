package ar.com.leo.super_master_backend.dominio.producto.controller;

import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoClienteDTO;
import ar.com.leo.super_master_backend.dominio.producto.service.ProductoClienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/productos/{productoId}/clientes")
public class ProductoClienteController {

    private final ProductoClienteService service;

    @GetMapping
    public ResponseEntity<List<ProductoClienteDTO>> listar(@PathVariable Integer productoId) {
        return ResponseEntity.ok(service.listar(productoId));
    }

    @PostMapping("/{clienteId}")
    public ResponseEntity<ProductoClienteDTO> agregar(
            @PathVariable Integer productoId,
            @PathVariable Integer clienteId
    ) {
        return ResponseEntity.ok(service.agregar(productoId, clienteId));
    }

    @DeleteMapping("/{clienteId}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Integer productoId,
            @PathVariable Integer clienteId
    ) {
        service.eliminar(productoId, clienteId);
        return ResponseEntity.noContent().build();
    }

}