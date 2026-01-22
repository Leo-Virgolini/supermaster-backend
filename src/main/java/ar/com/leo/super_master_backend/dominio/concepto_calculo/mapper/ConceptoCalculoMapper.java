package ar.com.leo.super_master_backend.dominio.concepto_calculo.mapper;

import ar.com.leo.super_master_backend.config.GlobalMapperConfig;
import ar.com.leo.super_master_backend.dominio.concepto_calculo.dto.ConceptoCalculoCreateDTO;
import ar.com.leo.super_master_backend.dominio.concepto_calculo.dto.ConceptoCalculoDTO;
import ar.com.leo.super_master_backend.dominio.concepto_calculo.dto.ConceptoCalculoUpdateDTO;
import ar.com.leo.super_master_backend.dominio.concepto_calculo.entity.AplicaSobre;
import ar.com.leo.super_master_backend.dominio.concepto_calculo.entity.ConceptoCalculo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(config = GlobalMapperConfig.class)
public interface ConceptoCalculoMapper {

    // =============================
    // ENTITY → DTO
    // =============================
    @Mapping(source = "aplicaSobre", target = "aplicaSobre", qualifiedByName = "enumToString")
    ConceptoCalculoDTO toDTO(ConceptoCalculo entity);

    // =============================
    // CREATE DTO → ENTITY
    // =============================
    @Mapping(source = "aplicaSobre", target = "aplicaSobre", qualifiedByName = "stringToEnum")
    ConceptoCalculo toEntity(ConceptoCalculoCreateDTO dto);

    // =============================
    // UPDATE DTO → ENTITY (PATCH)
    // =============================
    @Mapping(source = "aplicaSobre", target = "aplicaSobre", qualifiedByName = "stringToEnum")
    void updateEntityFromDTO(ConceptoCalculoUpdateDTO dto, @MappingTarget ConceptoCalculo entity);

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
            return AplicaSobre.COMISION_SOBRE_PVP; // Valor por defecto
        }
        try {
            return AplicaSobre.valueOf(aplicaSobre.toUpperCase());
        } catch (IllegalArgumentException e) {
            return AplicaSobre.COMISION_SOBRE_PVP; // Valor por defecto si no es válido
        }
    }
}
