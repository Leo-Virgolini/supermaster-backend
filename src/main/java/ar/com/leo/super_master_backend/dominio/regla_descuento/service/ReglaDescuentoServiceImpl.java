package ar.com.leo.super_master_backend.dominio.regla_descuento.service;

import ar.com.leo.super_master_backend.dominio.regla_descuento.dto.ReglaDescuentoCreateDTO;
import ar.com.leo.super_master_backend.dominio.regla_descuento.dto.ReglaDescuentoDTO;
import ar.com.leo.super_master_backend.dominio.regla_descuento.dto.ReglaDescuentoUpdateDTO;
import ar.com.leo.super_master_backend.dominio.regla_descuento.entity.ReglaDescuento;
import ar.com.leo.super_master_backend.dominio.regla_descuento.mapper.ReglaDescuentoMapper;
import ar.com.leo.super_master_backend.dominio.regla_descuento.repository.ReglaDescuentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReglaDescuentoServiceImpl implements ReglaDescuentoService {

    private final ReglaDescuentoRepository repo;
    private final ReglaDescuentoMapper mapper;

    @Override
    public List<ReglaDescuentoDTO> listar() {
        return repo.findAll()
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public List<ReglaDescuentoDTO> listarPorCanal(Integer canalId) {
        return repo.findByCanalId(canalId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public ReglaDescuentoDTO obtener(Integer id) {
        return repo.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new RuntimeException("Regla de descuento no encontrada"));
    }

    @Override
    public ReglaDescuentoDTO crear(ReglaDescuentoCreateDTO dto) {
        ReglaDescuento entity = mapper.toEntity(dto);
        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    public ReglaDescuentoDTO actualizar(Integer id, ReglaDescuentoUpdateDTO dto) {
        ReglaDescuento entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Regla de descuento no encontrada"));

        mapper.updateEntityFromDTO(dto, entity);

        repo.save(entity);

        return mapper.toDTO(entity);
    }

    @Override
    public void eliminar(Integer id) {
        repo.deleteById(id);
    }

}