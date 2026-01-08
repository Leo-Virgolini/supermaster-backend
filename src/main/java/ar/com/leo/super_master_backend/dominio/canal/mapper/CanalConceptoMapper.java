package ar.com.leo.super_master_backend.dominio.canal.mapper;

import ar.com.leo.super_master_backend.dominio.canal.dto.CanalConceptoDTO;
import ar.com.leo.super_master_backend.dominio.canal.entity.Canal;
import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConcepto;
import ar.com.leo.super_master_backend.dominio.common.mapper.GlobalMapperConfig;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.entity.ConceptoGasto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface CanalConceptoMapper {

    // =============================
    // ENTITY → DTO
    // =============================
    @Mapping(source = "canal.id", target = "canalId")
    @Mapping(source = "concepto.id", target = "conceptoId")
    CanalConceptoDTO toDTO(CanalConcepto entity);

    // =============================
    // DTO → ENTITY
    // =============================
    @Mapping(target = "id.idCanal", source = "canalId")
    @Mapping(target = "id.idConcepto", source = "conceptoId")
    @Mapping(target = "canal", expression = "java(new Canal(dto.canalId()))")
    @Mapping(target = "concepto", expression = "java(new ConceptoGasto(dto.conceptoId()))")
    CanalConcepto toEntity(CanalConceptoDTO dto);
}