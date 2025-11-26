package ar.com.leo.super_master_backend.dominio.regla_descuento.dto;

public record ReglaDescuentoCreateDTO(
        Integer canalId,
        Integer catalogoId,
        Integer clasifGralId,
        Integer clasifGastroId,
        BigDecimal montoMinimo,
        BigDecimal descuentoPorcentaje,
        Integer prioridad,
        Boolean activo,
        String descripcion
) {
}