package ar.com.leo.super_master_backend.dominio.precio.controller;

import ar.com.leo.super_master_backend.dominio.producto.calculo.dto.FormulaCalculoDTO;
import ar.com.leo.super_master_backend.dominio.producto.calculo.dto.RecalculoMasivoDTO;
import ar.com.leo.super_master_backend.dominio.producto.calculo.service.CalculoPrecioService;
import ar.com.leo.super_master_backend.dominio.producto.calculo.service.RecalculoPrecioFacade;
import ar.com.leo.super_master_backend.dominio.producto.dto.CanalPreciosDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoConPreciosDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoFilter;
import ar.com.leo.super_master_backend.dominio.producto.service.ProductoService;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/precios")
public class PrecioController {

    private final ProductoService productoService;
    private final CalculoPrecioService calculoPrecioService;
    private final RecalculoPrecioFacade recalculoPrecioFacade;

    // =====================================================
    // LISTAR PRODUCTOS CON PRECIOS (PAGINADO)
    // =====================================================
    @GetMapping
    public ResponseEntity<Page<ProductoConPreciosDTO>> listar(

            // =======================
            // 0) FILTRO POR ID
            // =======================
            @RequestParam(required = false) Integer productoId,

            // =======================
            // 1) TEXTO
            // =======================
            @RequestParam(required = false) String texto,

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

            // =======================
            // 8) ORDENAMIENTO ESPECIAL
            // =======================
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(required = false) Integer sortCanalId,

            // =======================
            // 9) FILTRAR PRECIOS POR CANAL
            // =======================
            @RequestParam(required = false) Integer canalId,

            // =======================
            // 10) FILTRAR PRECIOS POR CUOTAS
            // =======================
            @RequestParam(required = false) Integer cuotas,

            Pageable pageable
    ) {

        ProductoFilter filter = new ProductoFilter(
                productoId,
                texto,
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
                sortBy,
                sortDir,
                sortCanalId,
                canalId,
                cuotas
        );

        return ResponseEntity.ok(productoService.listarConPrecios(filter, pageable));
    }

    // =====================================================
    // OBTENER FÓRMULA DEL CÁLCULO PASO A PASO
    // =====================================================
    @GetMapping("/formula")
    public ResponseEntity<FormulaCalculoDTO> obtenerFormula(
            @RequestParam @Positive(message = "El ID de producto debe ser positivo") Integer productoId,
            @RequestParam @Positive(message = "El ID de canal debe ser positivo") Integer canalId,
            @RequestParam @PositiveOrZero(message = "Las cuotas deben ser 0 o positivas") Integer cuotas
    ) {
        return ResponseEntity.ok(
                calculoPrecioService.obtenerFormulaCalculo(productoId, canalId, cuotas)
        );
    }

    // =====================================================
    // CALCULAR Y GUARDAR PRECIOS (TODAS LAS CUOTAS)
    // =====================================================
    @PostMapping("/calcular")
    public ResponseEntity<CanalPreciosDTO> calcular(
            @RequestParam @Positive(message = "El ID de producto debe ser positivo") Integer productoId,
            @RequestParam @Positive(message = "El ID de canal debe ser positivo") Integer canalId
    ) {
        return ResponseEntity.ok(
                calculoPrecioService.recalcularYGuardarPrecioCanalTodasCuotas(productoId, canalId)
        );
    }

    // =====================================================
    // RECALCULAR TODOS LOS PRECIOS (OPERACIÓN MASIVA)
    // =====================================================
    @PostMapping("/recalcular-todos")
    public ResponseEntity<RecalculoMasivoDTO> recalcularTodos() {
        int total = recalculoPrecioFacade.recalcularTodos();
        return ResponseEntity.ok(new RecalculoMasivoDTO(total, LocalDateTime.now()));
    }

}
