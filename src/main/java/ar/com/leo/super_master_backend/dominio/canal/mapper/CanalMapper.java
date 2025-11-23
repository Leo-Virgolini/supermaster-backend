package ar.com.leo.super_master_backend.dominio.canal.mapper;

import ar.com.leo.super_master_backend.dominio.canal.dto.CanalCreateDTO;
import ar.com.leo.super_master_backend.dominio.canal.dto.CanalDTO;
import ar.com.leo.super_master_backend.dominio.canal.dto.CanalUpdateDTO;
import ar.com.leo.super_master_backend.dominio.canal.entity.Canal;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CanalMapper {

    @Mapping(source = "idCanalBase.id", target = "canalBaseId")
    CanalDTO toDTO(Canal entity);

    @Mapping(source = "canalBaseId", target = "idCanalBase.id")
    Canal toEntity(CanalCreateDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "canalBaseId", target = "idCanalBase.id")
    void update(@MappingTarget Canal entity, CanalUpdateDTO dto);
}