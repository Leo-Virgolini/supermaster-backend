package ar.com.leo.super_master_backend.dominio.producto.controller;

import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCreateDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoFilter;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoUpdateDTO;
import ar.com.leo.super_master_backend.dominio.producto.service.ProductoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;

    @GetMapping
    public ResponseEntity<Page<ProductoDTO>> listar(Pageable pageable) {
        return ResponseEntity.ok(productoService.listar(pageable));
    }

    @PostMapping
    public ResponseEntity<ProductoDTO> crear(@RequestBody ProductoCreateDTO dto) {
        return ResponseEntity.ok(productoService.crear(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoDTO> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(productoService.obtener(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductoDTO> actualizar(@PathVariable Integer id, @RequestBody ProductoUpdateDTO dto) {
        return ResponseEntity.ok(productoService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // =====================================================
// BUSCAR / FILTRAR PRODUCTOS
// =====================================================
    @GetMapping("/buscar")
    public Page<ProductoDTO> buscar(

            // =======================
            // 1) TEXTO
            // =======================
            @RequestParam(required = false) String texto,

            // =======================
            // 2) BOOLEANOS / NUMÉRICOS
            // =======================
            @RequestParam(required = false) Boolean esCombo,
            @RequestParam(required = false) Integer uxb,

            // =======================
            // 3) MANY-TO-ONE
            // =======================
            @RequestParam(required = false) Integer marcaId,
            @RequestParam(required = false) Integer origenId,
            @RequestParam(required = false) Integer tipoId,
            @RequestParam(required = false) Integer clasifGralId,
            @RequestParam(required = false) Integer clasifGastroId,
            @RequestParam(required = false) Integer proveedorId,
            @RequestParam(required = false) Integer materialId,

            // =======================
            // 4) RANGOS (costo / IVA)
            // =======================
            @RequestParam(required = false) BigDecimal costoMin,
            @RequestParam(required = false) BigDecimal costoMax,
            @RequestParam(required = false) BigDecimal ivaMin,
            @RequestParam(required = false) BigDecimal ivaMax,

            // =======================
            // 5) FECHAS
            // =======================
            @RequestParam(required = false) LocalDate desdeFechaUltCosto,
            @RequestParam(required = false) LocalDate hastaFechaUltCosto,

            @RequestParam(required = false) LocalDate desdeFechaCreacion,
            @RequestParam(required = false) LocalDate hastaFechaCreacion,

            @RequestParam(required = false) LocalDate desdeFechaModificacion,
            @RequestParam(required = false) LocalDate hastaFechaModificacion,

            // =======================
            // 6) MANY-TO-MANY
            // =======================
            @RequestParam(required = false) List<Integer> aptoIds,
            @RequestParam(required = false) List<Integer> canalIds,
            @RequestParam(required = false) List<Integer> catalogoIds,
            @RequestParam(required = false) List<Integer> clienteIds,
            @RequestParam(required = false) List<Integer> mlaIds,

            Pageable pageable
    ) {

        ProductoFilter filter = new ProductoFilter(

                // TEXTO
                texto,

                // BOOLEANOS / NUMÉRICOS
                esCombo,
                uxb,

                // MANY-TO-ONE
                marcaId,
                origenId,
                tipoId,
                clasifGralId,
                clasifGastroId,
                proveedorId,
                materialId,

                // RANGOS
                costoMin,
                costoMax,
                ivaMin,
                ivaMax,

                // FECHAS ANTIGUAS
                desdeFechaUltCosto,
                hastaFechaUltCosto,

                // NUEVAS FECHAS
                desdeFechaCreacion,
                hastaFechaCreacion,
                desdeFechaModificacion,
                hastaFechaModificacion,

                // MANY-TO-MANY
                aptoIds,
                canalIds,
                catalogoIds,
                clienteIds,
                mlaIds
        );

        return productoService.filtrar(filter, pageable);
    }

}