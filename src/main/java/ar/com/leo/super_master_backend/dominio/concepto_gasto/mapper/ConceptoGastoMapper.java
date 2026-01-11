package ar.com.leo.super_master_backend.dominio.concepto_gasto.mapper;

import ar.com.leo.super_master_backend.config.GlobalMapperConfig;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.dto.ConceptoGastoCreateDTO;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.dto.ConceptoGastoDTO;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.dto.ConceptoGastoUpdateDTO;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.entity.AplicaSobre;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.entity.ConceptoGasto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(config = GlobalMapperConfig.class)
public interface ConceptoGastoMapper {

    // =============================
    // ENTITY → DTO
    // =============================
    @Mapping(source = "aplicaSobre", target = "aplicaSobre", qualifiedByName = "enumToString")
    ConceptoGastoDTO toDTO(ConceptoGasto entity);

    // =============================
    // CREATE DTO → ENTITY
    // =============================
    @Mapping(source = "aplicaSobre", target = "aplicaSobre", qualifiedByName = "stringToEnum")
    ConceptoGasto toEntity(ConceptoGastoCreateDTO dto);

    // =============================
    // UPDATE DTO → ENTITY (PATCH)
    // =============================
    @Mapping(source = "aplicaSobre", target = "aplicaSobre", qualifiedByName = "stringToEnum")
    void updateEntityFromDTO(ConceptoGastoUpdateDTO dto, @MappingTarget ConceptoGasto entity);

    // =============================
    // MÉTODOS DE CONVERSIÓN
    // =============================
    @Named("enumToString")
    default String enumToString(AplicaSobre aplicaSobre) {
        return aplicaSobre != null ? aplicaSobre.name() : null;
    }

    @Named("stringToEnum")
    default AplicaSobre stringToEnum(String aplicaSobre) {
        if (aplicaSobre == null || aplicaSobre.isBlank()) {
            return AplicaSobre.PVP; // Valor por defecto
        }
        try {
            return AplicaSobre.valueOf(aplicaSobre.toUpperCase());
        } catch (IllegalArgumentException e) {
            return AplicaSobre.PVP; // Valor por defecto si no es válido
        }
    }
}