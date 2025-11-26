package ar.com.leo.super_master_backend.dominio.producto.mapper;

import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoClienteDTO;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCliente;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductoClienteMapper {

    @Mapping(source = "producto.id", target = "productoId")
    @Mapping(source = "cliente.id", target = "clienteId")
    ProductoClienteDTO toDTO(ProductoCliente entity);

    @Mapping(target = "id.idProducto", source = "productoId")
    @Mapping(target = "id.idCliente", source = "clienteId")
    @Mapping(target = "producto", expression = "java(new Producto(dto.productoId()))")
    @Mapping(target = "cliente", expression = "java(new Cliente(dto.clienteId()))")
    ProductoCliente toEntity(ProductoClienteDTO dto);
}