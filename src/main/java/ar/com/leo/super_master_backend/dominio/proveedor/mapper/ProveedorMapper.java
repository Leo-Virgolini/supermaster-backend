package ar.com.leo.super_master_backend.dominio.proveedor.mapper;

import ar.com.leo.super_master_backend.dominio.proveedor.dto.ProveedorDTO;
import ar.com.leo.super_master_backend.dominio.proveedor.entity.Proveedor;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProveedorMapper {
    ProveedorDTO toDTO(Proveedor p);
}