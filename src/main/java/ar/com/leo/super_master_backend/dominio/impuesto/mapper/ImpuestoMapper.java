package ar.com.leo.super_master_backend.dominio.impuesto.mapper;

import ar.com.leo.super_master_backend.dominio.impuesto.dto.ImpuestoDTO;
import ar.com.leo.super_master_backend.dominio.impuesto.entity.Impuesto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ImpuestoMapper {

    ImpuestoDTO toDTO(Impuesto impuesto);
}