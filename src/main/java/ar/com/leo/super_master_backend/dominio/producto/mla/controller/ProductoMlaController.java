package ar.com.leo.super_master_backend.dominio.producto.mla.controller;

import ar.com.leo.super_master_backend.dominio.producto.mla.dto.MlaDTO;
import ar.com.leo.super_master_backend.dominio.producto.mla.service.MlaService;
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
@RequestMapping("/api/productos/{productoId}/mla")
public class ProductoMlaController {

    private final MlaService mlaService;

    // ============================
    // LISTAR LOS MLA DEL PRODUCTO
    // ============================
    @GetMapping
    public ResponseEntity<List<MlaDTO>> listar(@PathVariable @Positive(message = "El ID de producto debe ser positivo") Integer productoId) {
        return ResponseEntity.ok(mlaService.listarPorProducto(productoId));
    }

    // ============================
    // CREAR MLA PARA UN PRODUCTO
    // ============================
    @PostMapping
    public ResponseEntity<MlaDTO> crear(
            @PathVariable @Positive(message = "El ID de producto debe ser positivo") Integer productoId,
            @Valid @RequestBody MlaDTO dto
    ) {
        MlaDTO creado = mlaService.crear(productoId, dto);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{mlaId}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(location).body(creado);
    }

    // ============================
    // ACTUALIZAR MLA EXISTENTE
    // ============================
    @PutMapping("/{mlaId}")
    public ResponseEntity<MlaDTO> actualizar(
            @PathVariable @Positive(message = "El ID de producto debe ser positivo") Integer productoId,
            @PathVariable @Positive(message = "El ID de MLA debe ser positivo") Integer mlaId,
            @Valid @RequestBody MlaDTO dto
    ) {
        return ResponseEntity.ok(mlaService.actualizar(productoId, mlaId, dto));
    }

    // ============================
    // ELIMINAR MLA DEL PRODUCTO
    // ============================
    @DeleteMapping("/{mlaId}")
    public ResponseEntity<Void> eliminar(
            @PathVariable @Positive(message = "El ID de producto debe ser positivo") Integer productoId,
            @PathVariable @Positive(message = "El ID de MLA debe ser positivo") Integer mlaId
    ) {
        mlaService.eliminar(productoId, mlaId);
        return ResponseEntity.noContent().build();
    }

}