package ar.com.leo.super_master_backend.dominio.producto.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.leo.super_master_backend.dominio.canal.entity.Canal;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalRepository;
import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCanalDTO;
import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanal;
import ar.com.leo.super_master_backend.dominio.producto.mapper.ProductoCanalMapper;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductoCanalServiceImpl implements ProductoCanalService {

    private final ProductoCanalRepository repo;
    private final ProductoCanalMapper mapper;
    private final ProductoRepository productoRepository;
    private final CanalRepository canalRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductoCanalDTO> listar(Integer productoId) {
        return repo.findByProductoId(productoId)
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProductoCanalDTO agregar(Integer productoId, Integer canalId) {
        // Validar que exista el producto
        productoRepository.findById(productoId)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado"));

        // Validar que exista el canal
        canalRepository.findById(canalId)
                .orElseThrow(() -> new NotFoundException("Canal no encontrado"));

        // Si ya existe, devolverlo
        var existente = repo.findByProductoIdAndCanalId(productoId, canalId);
        if (existente.isPresent()) {
            return mapper.toDTO(existente.get());
        }

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
    @Transactional
    public ProductoCanalDTO actualizar(Integer productoId, Integer canalId, ProductoCanalDTO dto) {

        ProductoCanal pc = repo.findByProductoIdAndCanalId(productoId, canalId)
                .orElseThrow(() -> new NotFoundException("Configuración de Producto-Canal no existe."));

        // MapStruct NO actualiza entidades existentes, así que asignamos a mano
        pc.setMargenPorcentaje(dto.margenPorcentaje());
        pc.setMargenFijo(dto.margenFijo());
        pc.setUsaCanalBase(dto.usaCanalBase());
        pc.setAplicaCuotas(dto.aplicaCuotas());
        pc.setAplicaComision(dto.aplicaComision());
        pc.setNotas(dto.notas());

        repo.save(pc);

        return mapper.toDTO(pc);
    }

    @Override
    @Transactional
    public void eliminar(Integer productoId, Integer canalId) {
        repo.deleteByProductoIdAndCanalId(productoId, canalId);
    }

}