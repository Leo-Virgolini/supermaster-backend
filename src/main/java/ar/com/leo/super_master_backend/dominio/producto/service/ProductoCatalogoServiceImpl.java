package ar.com.leo.super_master_backend.dominio.producto.service;

import ar.com.leo.super_master_backend.dominio.catalogo.entity.Catalogo;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCatalogoDTO;
import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCatalogo;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCatalogoId;
import ar.com.leo.super_master_backend.dominio.producto.mapper.ProductoCatalogoMapper;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCatalogoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoCatalogoServiceImpl implements ProductoCatalogoService {

    private final ProductoCatalogoRepository repo;
    private final ProductoCatalogoMapper mapper;

    @Override
    public List<ProductoCatalogoDTO> listar(Integer productoId) {
        return repo.findByProductoId(productoId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public ProductoCatalogoDTO agregar(Integer productoId, Integer catalogoId) {

        ProductoCatalogo entidad = new ProductoCatalogo();
        entidad.setId(new ProductoCatalogoId(productoId, catalogoId));
        entidad.setProducto(new Producto(productoId));
        entidad.setCatalogo(new Catalogo(catalogoId));

        repo.save(entidad);

        return mapper.toDTO(entidad);
    }

    @Override
    public void eliminar(Integer productoId, Integer catalogoId) {
        repo.deleteByProductoIdAndCatalogoId(productoId, catalogoId);
    }

}