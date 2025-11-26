package ar.com.leo.super_master_backend.dominio.producto.service;

import ar.com.leo.super_master_backend.dominio.cliente.entity.Cliente;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoClienteDTO;
import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCliente;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoClienteId;
import ar.com.leo.super_master_backend.dominio.producto.mapper.ProductoClienteMapper;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoClienteServiceImpl implements ProductoClienteService {

    private final ProductoClienteRepository repo;
    private final ProductoClienteMapper mapper;

    @Override
    public List<ProductoClienteDTO> listar(Integer productoId) {
        return repo.findByProductoId(productoId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public ProductoClienteDTO agregar(Integer productoId, Integer clienteId) {

        ProductoCliente entity = new ProductoCliente();

        entity.setId(new ProductoClienteId(productoId, clienteId));
        entity.setProducto(new Producto(productoId));
        entity.setCliente(new Cliente(clienteId));

        repo.save(entity);

        return mapper.toDTO(entity);
    }

    @Override
    public void eliminar(Integer productoId, Integer clienteId) {
        repo.deleteByProductoIdAndClienteId(productoId, clienteId);
    }

}