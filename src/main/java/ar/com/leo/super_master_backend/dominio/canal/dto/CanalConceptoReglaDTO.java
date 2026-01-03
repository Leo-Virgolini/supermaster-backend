package ar.com.leo.super_master_backend.dominio.canal.dto;

public record CanalConceptoReglaDTO(
        Long id,
        Integer canalId,
        Integer conceptoId,
        String tipoRegla,
        Integer tipoId,
        Integer clasifGastroId,
        Integer clasifGralId,
        Integer marcaId,
        Boolean esMaquina
) {
}

