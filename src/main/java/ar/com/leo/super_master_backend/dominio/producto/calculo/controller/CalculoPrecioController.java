package ar.com.leo.super_master_backend.dominio.producto.calculo.controller;

import ar.com.leo.super_master_backend.dominio.producto.calculo.dto.FormulaCalculoDTO;
import ar.com.leo.super_master_backend.dominio.producto.calculo.dto.RecalculoMasivoDTO;
import ar.com.leo.super_master_backend.dominio.producto.calculo.service.CalculoPrecioService;
import ar.com.leo.super_master_backend.dominio.producto.calculo.service.RecalculoPrecioFacade;
import ar.com.leo.super_master_backend.dominio.producto.dto.CanalPreciosDTO;

import java.time.LocalDateTime;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/productos")
public class CalculoPrecioController {

    private final CalculoPrecioService service;
    private final RecalculoPrecioFacade recalculoPrecioFacade;

    // -------------------------------------------------
    // 1) Calcular y guardar precios (todas las cuotas)
    // -------------------------------------------------
    @PostMapping("/{idProducto}/canales/{idCanal}/calculo")
    public ResponseEntity<CanalPreciosDTO> calcularYGuardar(
            @PathVariable @Positive(message = "El ID de producto debe ser positivo") Integer idProducto,
            @PathVariable @Positive(message = "El ID de canal debe ser positivo") Integer idCanal
    ) {
        return ResponseEntity.ok(
                service.recalcularYGuardarPrecioCanalTodasCuotas(idProducto, idCanal)
        );
    }

    // -------------------------------------------------
    // 2) Obtener fórmula del cálculo paso a paso
    // -------------------------------------------------
    @GetMapping("/formula")
    public ResponseEntity<FormulaCalculoDTO> obtenerFormula(
            @RequestParam @Positive(message = "El ID de producto debe ser positivo") Integer productoId,
            @RequestParam @Positive(message = "El ID de canal debe ser positivo") Integer canalId,
            @RequestParam(required = false) Integer cuotas
    ) {
        return ResponseEntity.ok(
                service.obtenerFormulaCalculo(productoId, canalId, cuotas)
        );
    }

    // -------------------------------------------------
    // 3) Recalcular TODOS los precios (operación masiva)
    // -------------------------------------------------
    @PostMapping("/calculo/recalcular-todos")
    public ResponseEntity<RecalculoMasivoDTO> recalcularTodos() {
        int total = recalculoPrecioFacade.recalcularTodos();
        return ResponseEntity.ok(new RecalculoMasivoDTO(total, LocalDateTime.now()));
    }

}