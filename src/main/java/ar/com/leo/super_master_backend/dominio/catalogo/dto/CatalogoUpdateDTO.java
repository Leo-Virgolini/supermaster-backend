package ar.com.leo.super_master_backend.dominio.catalogo.dto;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CatalogoUpdateDTO(
        @Size(max = 45)
        String catalogo,
        Boolean exportarConIva,
        BigDecimal recargoPorcentaje
) {}