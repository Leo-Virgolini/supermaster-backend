package ar.com.leo.super_master_backend.dominio.producto.service;

import ar.com.leo.super_master_backend.dominio.apto.entity.Apto;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoAptoDTO;
import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoApto;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoAptoId;
import ar.com.leo.super_master_backend.dominio.producto.mapper.ProductoAptoMapper;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoAptoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoAptoServiceImpl implements ProductoAptoService {

    private final ProductoAptoRepository repo;
    private final ProductoAptoMapper mapper;

    @Override
    public List<ProductoAptoDTO> listar(Integer productoId) {
        return repo.findByProductoId(productoId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public ProductoAptoDTO agregar(Integer productoId, Integer aptoId) {

        ProductoApto entity = new ProductoApto();

        entity.setId(new ProductoAptoId(productoId, aptoId));
        entity.setProducto(new Producto(productoId));
        entity.setApto(new Apto(aptoId));

        repo.save(entity);

        return mapper.toDTO(entity);
    }

    @Override
    public void eliminar(Integer productoId, Integer aptoId) {
        repo.deleteByProductoIdAndAptoId(productoId, aptoId);
    }

}