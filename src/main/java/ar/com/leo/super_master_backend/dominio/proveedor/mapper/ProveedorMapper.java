package ar.com.leo.super_master_backend.dominio.proveedor.mapper;

import ar.com.leo.super_master_backend.dominio.proveedor.dto.ProveedorCreateDTO;
import ar.com.leo.super_master_backend.dominio.proveedor.dto.ProveedorDTO;
import ar.com.leo.super_master_backend.dominio.proveedor.dto.ProveedorUpdateDTO;
import ar.com.leo.super_master_backend.dominio.proveedor.entity.Proveedor;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE
)
public interface ProveedorMapper {

    // ===============================
    // ENTITY → DTO
    // ===============================
    ProveedorDTO toDTO(Proveedor entity);


    // ===============================
    // CREATE DTO → ENTITY
    // ===============================
    Proveedor toEntity(ProveedorCreateDTO dto);


    // ===============================
    // UPDATE DTO → ENTITY (PATCH)
    // ===============================
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDTO(ProveedorUpdateDTO dto, @MappingTarget Proveedor entity);
}