package ar.com.leo.super_master_backend.dominio.promocion.mapper;

import ar.com.leo.super_master_backend.dominio.promocion.dto.PromocionCreateDTO;
import ar.com.leo.super_master_backend.dominio.promocion.dto.PromocionDTO;
import ar.com.leo.super_master_backend.dominio.promocion.dto.PromocionUpdateDTO;
import ar.com.leo.super_master_backend.dominio.promocion.entity.Promocion;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PromocionMapper {

    PromocionDTO toDTO(Promocion entity);

    Promocion toEntity(PromocionCreateDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDTO(PromocionUpdateDTO dto, @MappingTarget Promocion entity);
}
