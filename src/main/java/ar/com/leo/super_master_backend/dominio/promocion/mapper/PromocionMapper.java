package ar.com.leo.super_master_backend.dominio.promocion.mapper;

import ar.com.leo.super_master_backend.dominio.common.mapper.GlobalMapperConfig;
import ar.com.leo.super_master_backend.dominio.promocion.dto.PromocionCreateDTO;
import ar.com.leo.super_master_backend.dominio.promocion.dto.PromocionDTO;
import ar.com.leo.super_master_backend.dominio.promocion.dto.PromocionUpdateDTO;
import ar.com.leo.super_master_backend.dominio.promocion.entity.Promocion;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = GlobalMapperConfig.class)
public interface PromocionMapper {

    PromocionDTO toDTO(Promocion entity);

    Promocion toEntity(PromocionCreateDTO dto);

    void updateEntityFromDTO(PromocionUpdateDTO dto, @MappingTarget Promocion entity);
}
