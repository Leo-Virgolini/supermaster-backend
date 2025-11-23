package ar.com.leo.super_master_backend.dominio.cliente.mapper;

import ar.com.leo.super_master_backend.dominio.cliente.dto.ClienteDTO;
import ar.com.leo.super_master_backend.dominio.cliente.entity.Cliente;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ClienteMapper {
    ClienteDTO toDTO(Cliente c);
}