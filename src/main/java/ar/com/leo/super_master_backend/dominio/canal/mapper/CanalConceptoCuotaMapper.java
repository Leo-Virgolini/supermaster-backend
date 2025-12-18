package ar.com.leo.super_master_backend.dominio.canal.mapper;

import ar.com.leo.super_master_backend.dominio.canal.dto.CanalConceptoCuotaCreateDTO;
import ar.com.leo.super_master_backend.dominio.canal.dto.CanalConceptoCuotaDTO;
import ar.com.leo.super_master_backend.dominio.canal.dto.CanalConceptoCuotaUpdateDTO;
import ar.com.leo.super_master_backend.dominio.canal.entity.Canal;
import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConceptoCuota;
import ar.com.leo.super_master_backend.dominio.canal.entity.TipoCuota;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE
)
public interface CanalConceptoCuotaMapper {

    // =============================
    // ENTITY → DTO
    // =============================
    @Mapping(source = "canal.id", target = "canalId")
    @Mapping(source = "tipo", target = "tipo", qualifiedByName = "enumToString")
    CanalConceptoCuotaDTO toDTO(CanalConceptoCuota entity);

    // =============================
    // CREATE DTO → ENTITY
    // =============================
    @Mapping(target = "id", ignore = true)
    @Mapping(source = "canalId", target = "canal", qualifiedByName = "canalFromId")
    @Mapping(source = "tipo", target = "tipo", qualifiedByName = "stringToEnum")
    CanalConceptoCuota toEntity(CanalConceptoCuotaCreateDTO dto);

    // =============================
    // UPDATE DTO → ENTITY (PATCH)
    // =============================
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "canal", ignore = true)
    @Mapping(source = "tipo", target = "tipo", qualifiedByName = "stringToEnum")
    void updateEntityFromDTO(CanalConceptoCuotaUpdateDTO dto, @MappingTarget CanalConceptoCuota entity);

    // =============================
    // MÉTODOS DE CONVERSIÓN
    // =============================
    @Named("enumToString")
    default String enumToString(TipoCuota tipoCuota) {
        return tipoCuota != null ? tipoCuota.name() : null;
    }

    @Named("stringToEnum")
    default TipoCuota stringToEnum(String tipoCuota) {
        if (tipoCuota == null || tipoCuota.isBlank()) {
            return TipoCuota.NORMAL; // Valor por defecto
        }
        try {
            return TipoCuota.valueOf(tipoCuota.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TipoCuota.NORMAL; // Valor por defecto si no es válido
        }
    }

    @Named("canalFromId")
    default Canal canalFromId(Integer id) {
        return id != null ? new Canal(id) : null;
    }
}

