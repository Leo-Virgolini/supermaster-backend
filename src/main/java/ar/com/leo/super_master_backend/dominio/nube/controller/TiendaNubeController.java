package ar.com.leo.super_master_backend.dominio.nube.controller;

import ar.com.leo.super_master_backend.dominio.nube.dto.StockNubeDTO;
import ar.com.leo.super_master_backend.dominio.nube.dto.VentaNubeDTO;
import ar.com.leo.super_master_backend.dominio.nube.service.TiendaNubeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/nube")
public class TiendaNubeController {

    private final TiendaNubeService tiendaNubeService;

    // =====================================================
    // STATUS
    // =====================================================

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> obtenerStatus() {
        return ResponseEntity.ok(Map.of(
                "configurado", tiendaNubeService.isConfigured(),
                "servicio", "Tienda Nube",
                "stores", tiendaNubeService.getStoresDisponibles()
        ));
    }

    // =====================================================
    // VENTAS
    // =====================================================

    @GetMapping("/ventas")
    public ResponseEntity<List<VentaNubeDTO>> obtenerTodasLasVentas() {
        return ResponseEntity.ok(tiendaNubeService.obtenerTodasLasVentas());
    }

    @GetMapping("/ventas/hogar")
    public ResponseEntity<List<VentaNubeDTO>> obtenerVentasHogar() {
        return ResponseEntity.ok(tiendaNubeService.obtenerVentasHogar());
    }

    @GetMapping("/ventas/gastro")
    public ResponseEntity<List<VentaNubeDTO>> obtenerVentasGastro() {
        return ResponseEntity.ok(tiendaNubeService.obtenerVentasGastro());
    }

    // =====================================================
    // STOCK
    // =====================================================

    @GetMapping("/stock/{sku}")
    public ResponseEntity<StockNubeDTO> obtenerStock(@PathVariable String sku) {
        StockNubeDTO stock = tiendaNubeService.obtenerStockPorSku(sku);
        if (stock == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(stock);
    }

    // =====================================================
    // ÓRDENES
    // =====================================================

    @GetMapping("/ordenes/{numero}")
    public ResponseEntity<JsonNode> buscarOrden(@PathVariable String numero) {
        JsonNode orden = tiendaNubeService.buscarOrdenPorNumero(numero);
        if (orden == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(orden);
    }
}
