package ar.com.leo.super_master_backend.dominio.regla_descuento.mapper;

import ar.com.leo.super_master_backend.dominio.regla_descuento.dto.ReglaDescuentoDTO;
import ar.com.leo.super_master_backend.dominio.regla_descuento.entity.ReglaDescuento;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReglaDescuentoMapper {

    @Mapping(source = "idCanal.id", target = "canalId")
    @Mapping(source = "idCatalogo.id", target = "catalogoId")
    @Mapping(source = "idClasifGral.id", target = "clasifGralId")
    @Mapping(source = "idClasifGastro.id", target = "clasifGastroId")
    ReglaDescuentoDTO toDTO(ReglaDescuento e);

    @Mapping(source = "canalId", target = "idCanal.id")
    @Mapping(source = "catalogoId", target = "idCatalogo.id")
    @Mapping(source = "clasifGralId", target = "idClasifGral.id")
    @Mapping(source = "clasifGastroId", target = "idClasifGastro.id")
    ReglaDescuento toEntity(ReglaDescuentoDTO dto);
}