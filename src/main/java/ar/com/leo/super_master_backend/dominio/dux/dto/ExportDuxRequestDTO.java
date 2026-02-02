package ar.com.leo.super_master_backend.dominio.dux.dto;

import java.util.List;

public record ExportDuxRequestDTO(
        List<String> skus
) {}
