package ar.com.leo.super_master_backend.dominio.producto.mla.service;

import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import ar.com.leo.super_master_backend.dominio.common.exception.BadRequestException;
import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoRepository;
import ar.com.leo.super_master_backend.dominio.producto.mla.dto.MlaDTO;
import ar.com.leo.super_master_backend.dominio.producto.mla.entity.Mla;
import ar.com.leo.super_master_backend.dominio.producto.mla.mapper.MlaMapper;
import ar.com.leo.super_master_backend.dominio.producto.mla.repository.MlaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MlaServiceImpl implements MlaService {

    private final MlaRepository repo;
    private final MlaMapper mapper;
    private final ProductoRepository productoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MlaDTO> listarPorProducto(Integer productoId) {
        return repo.findByProductoId(productoId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public MlaDTO crear(Integer productoId, MlaDTO dto) {
        // Validar que el producto exista
        productoRepository.findById(productoId)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado"));

        Mla entity = new Mla();
        entity.setProducto(new Producto(productoId));
        entity.setMla(dto.mla());
        entity.setPrecioEnvio(dto.precioEnvio());

        repo.save(entity);

        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public MlaDTO actualizar(Integer productoId, Integer mlaId, MlaDTO dto) {

        Mla entity = repo.findById(mlaId)
                .orElseThrow(() -> new NotFoundException("MLA no encontrado"));

        if (!entity.getProducto().getId().equals(productoId)) {
            throw new BadRequestException("El MLA no pertenece a este producto");
        }

        entity.setMla(dto.mla());
        entity.setPrecioEnvio(dto.precioEnvio());

        repo.save(entity);

        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public void eliminar(Integer productoId, Integer mlaId) {

        Mla entity = repo.findById(mlaId)
                .orElseThrow(() -> new NotFoundException("MLA no encontrado"));

        if (!entity.getProducto().getId().equals(productoId)) {
            throw new BadRequestException("El MLA no pertenece a este producto");
        }

        repo.delete(entity);
    }

}