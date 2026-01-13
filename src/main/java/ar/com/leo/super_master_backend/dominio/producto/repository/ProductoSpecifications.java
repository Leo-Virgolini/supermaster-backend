package ar.com.leo.super_master_backend.dominio.producto.repository;

import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ProductoSpecifications {

    /* ==========================================================
       1) BÚSQUEDA POR TEXTO (sku, descripcion, tituloWeb, codExt, mla, mlau)
       ========================================================== */
    public static Specification<Producto> textoLike(String texto) {
        return (root, query, cb) -> {
            if (texto == null || texto.isBlank()) return null;

            String pattern = "%" + texto.toLowerCase() + "%";

            // LEFT JOIN para incluir productos sin MLA
            jakarta.persistence.criteria.Join<Producto, ?> mlaJoin = root.join("mla", JoinType.LEFT);

            return cb.or(
                    cb.like(cb.lower(root.get("sku")), pattern),
                    cb.like(cb.lower(root.get("codExt")), pattern),
                    cb.like(cb.lower(root.get("descripcion")), pattern),
                    cb.like(cb.lower(root.get("tituloWeb")), pattern),
                    cb.like(cb.lower(mlaJoin.get("mla")), pattern),
                    cb.like(cb.lower(mlaJoin.get("mlau")), pattern)
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
            return cb.greaterThanOrEqualTo(root.get("fechaUltCosto"), fecha.atStartOfDay());
        };
    }

    public static Specification<Producto> hastaFechaUltCosto(LocalDate fecha) {
        return (root, query, cb) -> {
            if (fecha == null) return null;
            return cb.lessThanOrEqualTo(root.get("fechaUltCosto"), fecha.plusDays(1).atStartOfDay());
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
            return root.join("mla", JoinType.LEFT).get("id").in(ids);
        };
    }

    /**
     * Filtra productos que tienen precios calculados en los canales especificados.
     * Usa la relación con ProductoCanalPrecio que tiene id_canal.
     */
    public static Specification<Producto> canalIds(List<Integer> ids) {
        return (root, query, cb) -> {
            if (ids == null || ids.isEmpty()) return null;
            Join<Producto, ?> preciosJoin = root.join("productoCanalPrecios", JoinType.INNER);
            return preciosJoin.get("canal").get("id").in(ids);
        };
    }

    public static Specification<Producto> desdeFechaCreacion(LocalDate f) {
        return (root, query, cb) ->
                f == null ? null :
                        cb.greaterThanOrEqualTo(
                                root.get("fechaCreacion"),
                                f.atStartOfDay()
                        );
    }

    public static Specification<Producto> hastaFechaCreacion(LocalDate f) {
        return (root, query, cb) ->
                f == null ? null :
                        cb.lessThanOrEqualTo(
                                root.get("fechaCreacion"),
                                f.plusDays(1).atStartOfDay()
                        );
    }

    public static Specification<Producto> desdeFechaModificacion(LocalDate f) {
        return (root, query, cb) ->
                f == null ? null :
                        cb.greaterThanOrEqualTo(
                                root.get("fechaModificacion"),
                                f.atStartOfDay()
                        );
    }

    public static Specification<Producto> hastaFechaModificacion(LocalDate f) {
        return (root, query, cb) ->
                f == null ? null :
                        cb.lessThanOrEqualTo(
                                root.get("fechaModificacion"),
                                f.plusDays(1).atStartOfDay()
                        );
    }

    /* ============================================
       7) NUEVOS FILTROS: esMaquina, tieneMla, activo, stock
       ============================================ */

    /**
     * Filtra por esMaquina a través de la relación con ClasifGastro.
     * Si esMaquina es null, no aplica filtro.
     */
    public static Specification<Producto> esMaquina(Boolean esMaquina) {
        return (root, query, cb) -> {
            if (esMaquina == null) return null;
            Join<Producto, ?> clasifGastroJoin = root.join("clasifGastro", JoinType.LEFT);
            if (esMaquina) {
                return cb.equal(clasifGastroJoin.get("esMaquina"), true);
            } else {
                return cb.or(
                        cb.isNull(clasifGastroJoin.get("id")),
                        cb.equal(clasifGastroJoin.get("esMaquina"), false)
                );
            }
        };
    }

    /**
     * Filtra productos que tienen o no tienen MLA asignado.
     * tieneMla=true: solo productos con MLA
     * tieneMla=false: solo productos sin MLA
     */
    public static Specification<Producto> tieneMla(Boolean tieneMla) {
        return (root, query, cb) -> {
            if (tieneMla == null) return null;
            if (tieneMla) {
                return cb.isNotNull(root.get("mla"));
            } else {
                return cb.isNull(root.get("mla"));
            }
        };
    }

    /**
     * Filtra por estado activo/inactivo.
     */
    public static Specification<Producto> activo(Boolean activo) {
        return (root, query, cb) -> {
            if (activo == null) return null;
            return cb.equal(root.get("activo"), activo);
        };
    }

    /**
     * Filtra por stock mínimo.
     */
    public static Specification<Producto> stockMin(Integer min) {
        return (root, query, cb) -> {
            if (min == null) return null;
            return cb.ge(root.get("stock"), min);
        };
    }

    /**
     * Filtra por stock máximo.
     */
    public static Specification<Producto> stockMax(Integer max) {
        return (root, query, cb) -> {
            if (max == null) return null;
            return cb.le(root.get("stock"), max);
        };
    }

    /**
     * Filtra por rango de PVP en un canal específico.
     * Requiere canalId para saber en qué canal buscar el precio.
     * Solo considera precios de contado (cuotas = null).
     */
    public static Specification<Producto> pvpEnRango(BigDecimal pvpMin, BigDecimal pvpMax, Integer canalId) {
        return (root, query, cb) -> {
            if (canalId == null || (pvpMin == null && pvpMax == null)) return null;

            jakarta.persistence.criteria.Join<Producto, ?> preciosJoin = root.join("productoCanalPrecios", JoinType.INNER);

            java.util.ArrayList<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            // Filtrar por canal
            predicates.add(cb.equal(preciosJoin.get("canal").get("id"), canalId));

            // Solo contado (cuotas = null)
            predicates.add(cb.isNull(preciosJoin.get("cuotas")));

            // Rango de PVP
            if (pvpMin != null) {
                predicates.add(cb.ge(preciosJoin.get("pvp"), pvpMin));
            }
            if (pvpMax != null) {
                predicates.add(cb.le(preciosJoin.get("pvp"), pvpMax));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

}