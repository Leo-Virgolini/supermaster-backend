package ar.com.leo.super_master_backend.dominio.producto.controller;

import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCreateDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoFilter;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoUpdateDTO;
import ar.com.leo.super_master_backend.dominio.producto.service.ProductoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;

    // =====================================================
    // LISTAR / FILTRAR PRODUCTOS
    // =====================================================
    @GetMapping
    public ResponseEntity<Page<ProductoDTO>> listar(

            // =======================
            // 1) TEXTO
            // =======================
            @RequestParam(required = false) String search,

            // =======================
            // 2) BOOLEANOS / NUMÉRICOS
            // =======================
            @RequestParam(required = false) Boolean esCombo,
            @RequestParam(required = false) Integer uxb,
            @RequestParam(required = false) Boolean esMaquina,
            @RequestParam(required = false) Boolean tieneMla,
            @RequestParam(required = false) Boolean activo,

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
            // 4) RANGOS (costo / IVA / stock)
            // =======================
            @RequestParam(required = false) BigDecimal costoMin,
            @RequestParam(required = false) BigDecimal costoMax,
            @RequestParam(required = false) BigDecimal ivaMin,
            @RequestParam(required = false) BigDecimal ivaMax,
            @RequestParam(required = false) Integer stockMin,
            @RequestParam(required = false) Integer stockMax,

            // =======================
            // 5) RANGO PVP
            // =======================
            @RequestParam(required = false) BigDecimal pvpMin,
            @RequestParam(required = false) BigDecimal pvpMax,
            @RequestParam(required = false) Integer pvpCanalId,

            // =======================
            // 6) FECHAS
            // =======================
            @RequestParam(required = false) LocalDate desdeFechaUltCosto,
            @RequestParam(required = false) LocalDate hastaFechaUltCosto,

            @RequestParam(required = false) LocalDate desdeFechaCreacion,
            @RequestParam(required = false) LocalDate hastaFechaCreacion,

            @RequestParam(required = false) LocalDate desdeFechaModificacion,
            @RequestParam(required = false) LocalDate hastaFechaModificacion,

            // =======================
            // 7) MANY-TO-MANY
            // =======================
            @RequestParam(required = false) List<Integer> aptoIds,
            @RequestParam(required = false) List<Integer> canalIds,
            @RequestParam(required = false) List<Integer> catalogoIds,
            @RequestParam(required = false) List<Integer> clienteIds,
            @RequestParam(required = false) List<Integer> mlaIds,

            Pageable pageable
    ) {

        ProductoFilter filter = new ProductoFilter(
                null,  // productoId
                search,
                esCombo,
                uxb,
                esMaquina,
                tieneMla,
                activo,
                marcaId,
                origenId,
                tipoId,
                clasifGralId,
                clasifGastroId,
                proveedorId,
                materialId,
                costoMin,
                costoMax,
                ivaMin,
                ivaMax,
                stockMin,
                stockMax,
                pvpMin,
                pvpMax,
                pvpCanalId,
                desdeFechaUltCosto,
                hastaFechaUltCosto,
                desdeFechaCreacion,
                hastaFechaCreacion,
                desdeFechaModificacion,
                hastaFechaModificacion,
                aptoIds,
                canalIds,
                catalogoIds,
                clienteIds,
                mlaIds,
                null,  // canalId (no aplica, no devuelve precios)
                null   // cuotas (no aplica, no devuelve precios)
        );

        return ResponseEntity.ok(productoService.filtrar(filter, pageable));
    }

    // =====================================================
    // CRUD
    // =====================================================

    @PostMapping
    public ResponseEntity<ProductoDTO> crear(@Valid @RequestBody ProductoCreateDTO dto) {
        ProductoDTO creado = productoService.crear(dto);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoDTO> obtener(@PathVariable @Positive(message = "El ID debe ser positivo") Integer id) {
        return ResponseEntity.ok(productoService.obtener(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductoDTO> actualizar(
            @PathVariable @Positive(message = "El ID debe ser positivo") Integer id,
            @Valid @RequestBody ProductoUpdateDTO dto) {
        return ResponseEntity.ok(productoService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive(message = "El ID debe ser positivo") Integer id) {
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

}
