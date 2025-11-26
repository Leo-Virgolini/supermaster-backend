package ar.com.leo.super_master_backend.dominio.producto.mla.controller;

import ar.com.leo.super_master_backend.dominio.producto.mla.dto.MlaDTO;
import ar.com.leo.super_master_backend.dominio.producto.mla.service.MlaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<List<MlaDTO>> listar(@PathVariable Integer productoId) {
        return ResponseEntity.ok(mlaService.listarPorProducto(productoId));
    }

    // ============================
    // CREAR MLA PARA UN PRODUCTO
    // ============================
    @PostMapping
    public ResponseEntity<MlaDTO> crear(
            @PathVariable Integer productoId,
            @RequestBody MlaDTO dto
    ) {
        return ResponseEntity.ok(mlaService.crear(productoId, dto));
    }

    // ============================
    // ACTUALIZAR MLA EXISTENTE
    // ============================
    @PutMapping("/{mlaId}")
    public ResponseEntity<MlaDTO> actualizar(
            @PathVariable Integer productoId,
            @PathVariable Integer mlaId,
            @RequestBody MlaDTO dto
    ) {
        return ResponseEntity.ok(mlaService.actualizar(productoId, mlaId, dto));
    }

    // ============================
    // ELIMINAR MLA DEL PRODUCTO
    // ============================
    @DeleteMapping("/{mlaId}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Integer productoId,
            @PathVariable Integer mlaId
    ) {
        mlaService.eliminar(productoId, mlaId);
        return ResponseEntity.noContent().build();
    }

}