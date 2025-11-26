package ar.com.leo.super_master_backend.dominio.clasif_gral.mapper;

import ar.com.leo.super_master_backend.dominio.clasif_gral.dto.ClasifGralCreateDTO;
import ar.com.leo.super_master_backend.dominio.clasif_gral.dto.ClasifGralDTO;
import ar.com.leo.super_master_backend.dominio.clasif_gral.dto.ClasifGralUpdateDTO;
import ar.com.leo.super_master_backend.dominio.clasif_gral.entity.ClasifGral;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ClasifGralMapper {

    // =============================
    // ENTITY → DTO
    // =============================
    @Mapping(source = "padre.id", target = "padreId")
    ClasifGralDTO toDTO(ClasifGral entity);

    // =============================
    // CREATE DTO → ENTITY
    // =============================
    @Mapping(
            target = "padre",
            expression = "java(dto.padreId() != null ? new ClasifGral(dto.padreId()) : null)"
    )
    ClasifGral toEntity(ClasifGralCreateDTO dto);

    // =============================
    // UPDATE DTO → ENTITY (PATCH)
    // =============================
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(
            target = "padre",
            expression = "java(dto.padreId() != null ? new ClasifGral(dto.padreId()) : entity.getPadre())"
    )
    void updateEntityFromDTO(ClasifGralUpdateDTO dto, @MappingTarget ClasifGral entity);
}