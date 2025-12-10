package ar.com.leo.super_master_backend.dominio.producto.calculo.controller;

import ar.com.leo.super_master_backend.dominio.producto.calculo.dto.PrecioCalculadoDTO;
import ar.com.leo.super_master_backend.dominio.producto.calculo.service.CalculoPrecioService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/productos")
public class CalculoPrecioController {

    private final CalculoPrecioService service;

    // -------------------------------------------------
    // 1) Obtener cálculo sin persistir
    // -------------------------------------------------
    @GetMapping("/{idProducto}/canales/{idCanal}/calculo")
    public ResponseEntity<PrecioCalculadoDTO> calcularPrecio(
            @PathVariable @Positive(message = "El ID de producto debe ser positivo") Integer idProducto,
            @PathVariable @Positive(message = "El ID de canal debe ser positivo") Integer idCanal,
            @RequestParam(required = false) Integer cuotas
    ) {
        return ResponseEntity.ok(
                service.calcularPrecioCanal(idProducto, idCanal, cuotas)
        );
    }

    // -------------------------------------------------
    // 2) Recalcular y guardar en producto_canal_precios
    // -------------------------------------------------
    @PostMapping("/{idProducto}/canales/{idCanal}/recalcular")
    public ResponseEntity<PrecioCalculadoDTO> recalcularYGuardar(
            @PathVariable @Positive(message = "El ID de producto debe ser positivo") Integer idProducto,
            @PathVariable @Positive(message = "El ID de canal debe ser positivo") Integer idCanal
    ) {
        return ResponseEntity.ok(
                service.recalcularYGuardarPrecioCanal(idProducto, idCanal)
        );
    }

}