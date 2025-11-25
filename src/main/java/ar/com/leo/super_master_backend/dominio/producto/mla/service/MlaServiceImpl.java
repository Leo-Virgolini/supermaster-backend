package ar.com.leo.super_master_backend.dominio.producto.mla.service;

import ar.com.leo.super_master_backend.dominio.producto.mla.dto.MlaDTO;
import ar.com.leo.super_master_backend.dominio.producto.mla.entity.Mla;
import ar.com.leo.super_master_backend.dominio.producto.mla.mapper.MlaMapper;
import ar.com.leo.super_master_backend.dominio.producto.mla.repository.MlaRepository;
import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MlaServiceImpl implements MlaService {

    private final MlaRepository repo;
    private final MlaMapper mapper;

    @Override
    public MlaDTO obtener(Integer id) {
        Mla entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("MLA no encontrado"));
        return mapper.toDTO(entity);
    }

    @Override
    public List<MlaDTO> listarPorProducto(Integer productoId) {
        return repo.findByProductoId(productoId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public MlaDTO crear(MlaDTO dto) {
        Mla entity = mapper.toEntity(dto);
        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    public MlaDTO actualizar(Integer id, MlaDTO dto) {
        Mla entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("MLA no encontrado"));

        // Actualizar campos
        entity.setMla(dto.mla());
        entity.setPrecioEnvio(dto.precioEnvio());
        entity.setProducto(new Producto(dto.productoId()));

        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    public void eliminar(Integer id) {
        repo.deleteById(id);
    }

}