package ar.com.leo.super_master_backend.dominio.clasif_gastro.mapper;

import ar.com.leo.super_master_backend.dominio.clasif_gastro.dto.ClasifGastroCreateDTO;
import ar.com.leo.super_master_backend.dominio.clasif_gastro.dto.ClasifGastroDTO;
import ar.com.leo.super_master_backend.dominio.clasif_gastro.dto.ClasifGastroUpdateDTO;
import ar.com.leo.super_master_backend.dominio.clasif_gastro.entity.ClasifGastro;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ClasifGastroMapper {

    // =============================
    // ENTITY → DTO
    // =============================
    @Mapping(source = "padre.id", target = "padreId")
    ClasifGastroDTO toDTO(ClasifGastro entity);

    // =============================
    // CREATE DTO → ENTITY
    // =============================
    @Mapping(
            target = "padre",
            expression = "java(dto.padreId() != null ? new ClasifGastro(dto.padreId()) : null)"
    )
    ClasifGastro toEntity(ClasifGastroCreateDTO dto);

    // =============================
    // UPDATE DTO → ENTITY (PATCH)
    // =============================
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(
            target = "padre",
            expression = "java(dto.padreId() != null ? new ClasifGastro(dto.padreId()) : entity.getPadre())"
    )
    void updateEntityFromDTO(ClasifGastroUpdateDTO dto, @MappingTarget ClasifGastro entity);
}