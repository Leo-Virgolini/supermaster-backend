package ar.com.leo.super_master_backend.dominio.canal.mapper;

import ar.com.leo.super_master_backend.dominio.canal.dto.CanalConceptoDTO;
import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConcepto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface CanalConceptoMapper {

    @Mapping(source = "idCanal.id", target = "canalId")
    @Mapping(source = "idConcepto.id", target = "conceptoId")
    CanalConceptoDTO toDTO(CanalConcepto entity);

    @Mapping(source = "canalId", target = "id.idCanal")
    @Mapping(source = "conceptoId", target = "id.idConcepto")
    @Mapping(source = "canalId", target = "idCanal.id")
    @Mapping(source = "conceptoId", target = "idConcepto.id")
    CanalConcepto toEntity(CanalConceptoDTO dto);
}