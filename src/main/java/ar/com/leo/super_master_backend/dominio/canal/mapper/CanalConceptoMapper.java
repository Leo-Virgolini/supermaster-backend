package ar.com.leo.super_master_backend.dominio.canal.mapper;

import ar.com.leo.super_master_backend.dominio.canal.dto.CanalConceptoDTO;
import ar.com.leo.super_master_backend.dominio.canal.entity.Canal;
import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConcepto;
import ar.com.leo.super_master_backend.config.GlobalMapperConfig;
import ar.com.leo.super_master_backend.dominio.concepto_calculo.entity.ConceptoCalculo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface CanalConceptoMapper {

    // =============================
    // ENTITY → DTO
    // =============================
    @Mapping(source = "canal.id", target = "canalId")
    @Mapping(source = "concepto.id", target = "conceptoId")
    @Mapping(source = "concepto.concepto", target = "concepto")
    @Mapping(source = "concepto.porcentaje", target = "porcentaje")
    @Mapping(source = "concepto.aplicaSobre", target = "aplicaSobre")
    @Mapping(source = "concepto.descripcion", target = "descripcion")
    CanalConceptoDTO toDTO(CanalConcepto entity);

    // =============================
    // DTO → ENTITY
    // =============================
    @Mapping(target = "id.canalId", source = "canalId")
    @Mapping(target = "id.conceptoId", source = "conceptoId")
    @Mapping(target = "canal", expression = "java(new Canal(dto.canalId()))")
    @Mapping(target = "concepto", expression = "java(new ConceptoCalculo(dto.conceptoId()))")
    CanalConcepto toEntity(CanalConceptoDTO dto);
}