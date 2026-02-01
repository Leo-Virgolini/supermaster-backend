package ar.com.leo.super_master_backend.dominio.dux.controller;

import ar.com.leo.super_master_backend.dominio.common.response.ErrorResponse;
import ar.com.leo.super_master_backend.dominio.dux.model.Item;
import ar.com.leo.super_master_backend.dominio.dux.service.DuxService;
import ar.com.leo.super_master_backend.dominio.dux.service.DuxService.ProductoPrecioData;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dux")
public class DuxController {

    private final DuxService duxService;

    // =====================================================
    // STATUS
    // =====================================================

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> obtenerStatus() {
        return ResponseEntity.ok(Map.of(
                "configurado", duxService.isConfigured(),
                "servicio", "DUX ERP"
        ));
    }

    // =====================================================
    // PRODUCTOS
    // =====================================================

    @GetMapping("/productos")
    public ResponseEntity<List<Item>> obtenerProductos() {
        return ResponseEntity.ok(duxService.obtenerProductos());
    }

    @GetMapping("/productos/{codItem}")
    public ResponseEntity<Item> obtenerProducto(@PathVariable String codItem) {
        Item item = duxService.obtenerProductoPorCodigo(codItem);
        if (item == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(item);
    }

    // =====================================================
    // LISTAS DE PRECIOS
    // =====================================================

    @GetMapping("/listas-precios")
    public ResponseEntity<JsonNode> obtenerListasPrecios() {
        return ResponseEntity.ok(duxService.obtenerListasPrecios());
    }

    @GetMapping("/listas-precios/{nombre}/id")
    public ResponseEntity<Map<String, Object>> obtenerIdListaPrecio(@PathVariable String nombre) {
        long id = duxService.obtenerIdListaPrecio(nombre);
        return ResponseEntity.ok(Map.of(
                "nombre", nombre,
                "id", id
        ));
    }

    /**
     * Actualiza precios de productos en una lista de precios de DUX.
     *
     * <p><b>IMPORTANTE:</b> Es obligatorio enviar el tipo de producto (SIMPLE o COMBO) junto con el precio.
     * Si no se envía el tipo correcto, DUX desconfigura el producto y puede perder su configuración
     * de componentes (en caso de combos) u otras propiedades.</p>
     *
     * @param idLista   ID de la lista de precios en DUX
     * @param productos Lista de productos con SKU, tipo (SIMPLE/COMBO) y precio
     */
    @PostMapping("/listas-precios/{idLista}/precios")
    public ResponseEntity<?> modificarPrecios(
            @PathVariable long idLista,
            @RequestBody List<ProductoPrecioRequest> productos) {

        Map<String, ProductoPrecioData> productosMap = new java.util.HashMap<>();
        for (ProductoPrecioRequest p : productos) {
            productosMap.put(p.sku(), new ProductoPrecioData(p.tipo(), p.precio()));
        }

        int idProceso = duxService.modificarListaPrecios(productosMap, idLista);

        if (idProceso == 0) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.of("No se pudo iniciar el proceso de actualización", "/api/dux/listas-precios/" + idLista + "/precios"));
        }

        return ResponseEntity.ok(Map.of(
                "idProceso", idProceso,
                "mensaje", "Proceso de actualización iniciado"
        ));
    }

    // =====================================================
    // PROCESOS
    // =====================================================

    @GetMapping("/procesos/{idProceso}/estado")
    public ResponseEntity<Map<String, Object>> obtenerEstadoProceso(@PathVariable int idProceso) {
        String estado = duxService.obtenerEstadoProceso(idProceso);
        return ResponseEntity.ok(Map.of(
                "idProceso", idProceso,
                "estado", estado
        ));
    }

    // =====================================================
    // DTOs
    // =====================================================

    /**
     * Request para actualizar precio de un producto en DUX.
     *
     * @param sku    Código del producto (cod_item en DUX)
     * @param tipo   Tipo de producto: "SIMPLE" o "COMBO". <b>OBLIGATORIO</b> - si no se envía
     *               correctamente, DUX desconfigura el producto
     * @param precio Precio del producto
     */
    public record ProductoPrecioRequest(
            String sku,
            String tipo,
            double precio
    ) {}
}
