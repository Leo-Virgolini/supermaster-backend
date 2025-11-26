package ar.com.leo.super_master_backend.dominio.producto.service;

import ar.com.leo.super_master_backend.dominio.canal.entity.Canal;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCanalDTO;
import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanal;
import ar.com.leo.super_master_backend.dominio.producto.mapper.ProductoCanalMapper;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoCanalServiceImpl implements ProductoCanalService {

    private final ProductoCanalRepository repo;
    private final ProductoCanalMapper mapper;

    @Override
    public List<ProductoCanalDTO> listar(Integer productoId) {
        return repo.findByProductoId(productoId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public ProductoCanalDTO agregar(Integer productoId, Integer canalId) {

        // Si ya existe, devolverlo
        var existente = repo.findByProductoIdAndCanalId(productoId, canalId)
                .map(mapper::toDTO)
                .orElse(null);

        if (existente != null) return existente;

        ProductoCanal pc = new ProductoCanal();
        pc.setProducto(new Producto(productoId));
        pc.setCanal(new Canal(canalId));

        // valores por defecto
        pc.setUsaCanalBase(false);
        pc.setAplicaComision(true);
        pc.setAplicaCuotas(true);

        pc = repo.save(pc);

        return mapper.toDTO(pc);
    }

    @Override
    public ProductoCanalDTO actualizar(Integer productoId, Integer canalId, ProductoCanalDTO dto) {

        ProductoCanal pc = repo.findByProductoIdAndCanalId(productoId, canalId)
                .orElseThrow(() -> new RuntimeException("Relación Producto-Canal no existe."));

        // MapStruct NO actualiza entidades existentes, así que asignamos a mano
        pc.setMargenPorcentaje(dto.margenPorcentaje());
        pc.setMargenFijo(dto.margenFijo());
        pc.setMargenPromocion(dto.margenPromocion());
        pc.setMargenOferta(dto.margenOferta());
        pc.setUsaCanalBase(dto.usaCanalBase());
        pc.setAplicaCuotas(dto.aplicaCuotas());
        pc.setAplicaComision(dto.aplicaComision());
        pc.setNotas(dto.notas());

        repo.save(pc);

        return mapper.toDTO(pc);
    }

    @Override
    public void eliminar(Integer productoId, Integer canalId) {
        repo.deleteByProductoIdAndCanalId(productoId, canalId);
    }

}