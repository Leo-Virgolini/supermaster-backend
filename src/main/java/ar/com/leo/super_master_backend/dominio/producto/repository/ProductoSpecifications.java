package ar.com.leo.super_master_backend.dominio.producto.repository;

import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class ProductoSpecifications {

    private static final ZoneId ZONA_ARG = ZoneId.of("America/Argentina/Buenos_Aires");

    /* ==========================================================
       1) BÚSQUEDA POR TEXTO (sku, descripcion, tituloWeb, codExt)
       ========================================================== */
    public static Specification<Producto> textoLike(String texto) {
        return (root, query, cb) -> {
            if (texto == null || texto.isBlank()) return null;

            String pattern = "%" + texto.toLowerCase() + "%";

            return cb.or(
                    cb.like(cb.lower(root.get("sku")), pattern),
                    cb.like(cb.lower(root.get("codExt")), pattern),
                    cb.like(cb.lower(root.get("descripcion")), pattern),
                    cb.like(cb.lower(root.get("tituloWeb")), pattern)
            );
        };
    }

    /* ================================
       2) FILTROS BOOLEANOS Y NUMÉRICOS
       ================================ */
    public static Specification<Producto> esCombo(Boolean esCombo) {
        return (root, query, cb) -> {
            if (esCombo == null) return null;
            return cb.equal(root.get("esCombo"), esCombo);
        };
    }

    public static Specification<Producto> uxb(Integer uxb) {
        return (root, query, cb) -> {
            if (uxb == null) return null;
            return cb.equal(root.get("uxb"), uxb);
        };
    }

    /* ===========================
       3) FILTROS MANY-TO-ONE
       =========================== */
    public static Specification<Producto> marcaId(Integer marcaId) {
        return (root, query, cb) -> {
            if (marcaId == null) return null;
            return cb.equal(root.get("idMarca").get("id"), marcaId);
        };
    }

    public static Specification<Producto> origenId(Integer origenId) {
        return (root, query, cb) -> {
            if (origenId == null) return null;
            return cb.equal(root.get("idOrigen").get("id"), origenId);
        };
    }

    public static Specification<Producto> tipoId(Integer tipoId) {
        return (root, query, cb) -> {
            if (tipoId == null) return null;
            return cb.equal(root.get("idTipo").get("id"), tipoId);
        };
    }

    public static Specification<Producto> clasifGralId(Integer clasifGralId) {
        return (root, query, cb) -> {
            if (clasifGralId == null) return null;
            return cb.equal(root.get("idClasifGral").get("id"), clasifGralId);
        };
    }

    public static Specification<Producto> clasifGastroId(Integer clasifGastroId) {
        return (root, query, cb) -> {
            if (clasifGastroId == null) return null;
            return cb.equal(root.get("idClasifGastro").get("id"), clasifGastroId);
        };
    }

    public static Specification<Producto> proveedorId(Integer proveedorId) {
        return (root, query, cb) -> {
            if (proveedorId == null) return null;
            return cb.equal(root.get("idProveedor").get("id"), proveedorId);
        };
    }

    public static Specification<Producto> materialId(Integer materialId) {
        return (root, query, cb) -> {
            if (materialId == null) return null;
            return cb.equal(root.get("idMaterial").get("id"), materialId);
        };
    }

    /* ======================
       4) RANGOS: COSTO / IVA
       ====================== */
    public static Specification<Producto> costoMin(BigDecimal min) {
        return (root, query, cb) -> {
            if (min == null) return null;
            return cb.ge(root.get("costo"), min);
        };
    }

    public static Specification<Producto> costoMax(BigDecimal max) {
        return (root, query, cb) -> {
            if (max == null) return null;
            return cb.le(root.get("costo"), max);
        };
    }

    public static Specification<Producto> ivaMin(BigDecimal min) {
        return (root, query, cb) -> {
            if (min == null) return null;
            return cb.ge(root.get("iva"), min);
        };
    }

    public static Specification<Producto> ivaMax(BigDecimal max) {
        return (root, query, cb) -> {
            if (max == null) return null;
            return cb.le(root.get("iva"), max);
        };
    }

    /* ============================================
       5) RANGO DE FECHAS (fechaUltCosto)
       ============================================ */
    public static Specification<Producto> desdeFechaUltCosto(LocalDate fecha) {
        return (root, query, cb) -> {
            if (fecha == null) return null;
            return cb.greaterThanOrEqualTo(root.get("fechaUltCosto"), fecha.atStartOfDay(ZONA_ARG));
        };
    }

    public static Specification<Producto> hastaFechaUltCosto(LocalDate fecha) {
        return (root, query, cb) -> {
            if (fecha == null) return null;
            return cb.lessThanOrEqualTo(root.get("fechaUltCosto"), fecha.plusDays(1).atStartOfDay(ZONA_ARG));
        };
    }

    /* ============================================
       6) MANY TO MANY (IN LIST) — al menos uno
       ============================================ */

    public static Specification<Producto> aptoIds(List<Integer> ids) {
        return (root, query, cb) -> {
            if (ids == null || ids.isEmpty()) return null;
            return root.join("productosApto").get("apto").get("id").in(ids);
        };
    }

    public static Specification<Producto> canalIds(List<Integer> ids) {
        return (root, query, cb) -> {
            if (ids == null || ids.isEmpty()) return null;
            return root.join("productoCanales").get("canal").get("id").in(ids);
        };
    }

    public static Specification<Producto> catalogoIds(List<Integer> ids) {
        return (root, query, cb) -> {
            if (ids == null || ids.isEmpty()) return null;
            return root.join("catalogos").get("id").in(ids);
        };
    }

    public static Specification<Producto> clienteIds(List<Integer> ids) {
        return (root, query, cb) -> {
            if (ids == null || ids.isEmpty()) return null;
            return root.join("clientes").get("id").in(ids);
        };
    }

    public static Specification<Producto> mlaIds(List<Integer> ids) {
        return (root, query, cb) -> {
            if (ids == null || ids.isEmpty()) return null;
            return root.join("mlas").get("id").in(ids);
        };
    }

    public static Specification<Producto> desdeFechaCreacion(LocalDate f) {
        return (root, query, cb) ->
                f == null ? null :
                        cb.greaterThanOrEqualTo(
                                root.get("fechaCreacion"),
                                f.atStartOfDay(ZONA_ARG).toInstant()
                        );
    }

    public static Specification<Producto> hastaFechaCreacion(LocalDate f) {
        return (root, query, cb) ->
                f == null ? null :
                        cb.lessThanOrEqualTo(
                                root.get("fechaCreacion"),
                                f.plusDays(1).atStartOfDay(ZONA_ARG).toInstant()
                        );
    }

    public static Specification<Producto> desdeFechaModificacion(LocalDate f) {
        return (root, query, cb) ->
                f == null ? null :
                        cb.greaterThanOrEqualTo(
                                root.get("fechaModificacion"),
                                f.atStartOfDay(ZONA_ARG).toInstant()
                        );
    }

    public static Specification<Producto> hastaFechaModificacion(LocalDate f) {
        return (root, query, cb) ->
                f == null ? null :
                        cb.lessThanOrEqualTo(
                                root.get("fechaModificacion"),
                                f.plusDays(1).atStartOfDay(ZONA_ARG).toInstant()
                        );
    }

}