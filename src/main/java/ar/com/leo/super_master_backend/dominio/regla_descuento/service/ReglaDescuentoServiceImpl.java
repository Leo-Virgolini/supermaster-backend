package ar.com.leo.super_master_backend.dominio.regla_descuento.service;

import ar.com.leo.super_master_backend.dominio.canal.entity.Canal;
import ar.com.leo.super_master_backend.dominio.catalogo.entity.Catalogo;
import ar.com.leo.super_master_backend.dominio.clasif_gastro.entity.ClasifGastro;
import ar.com.leo.super_master_backend.dominio.clasif_gral.entity.ClasifGral;
import ar.com.leo.super_master_backend.dominio.regla_descuento.dto.ReglaDescuentoDTO;
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
    public ReglaDescuentoDTO obtener(Integer id) {
        ReglaDescuento e = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Regla de descuento no encontrada"));
        return mapper.toDTO(e);
    }

    @Override
    public List<ReglaDescuentoDTO> listar() {
        return repo.findAll().stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public ReglaDescuentoDTO crear(ReglaDescuentoDTO dto) {
        ReglaDescuento entity = mapper.toEntity(dto);
        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    public ReglaDescuentoDTO actualizar(Integer id, ReglaDescuentoDTO dto) {
        ReglaDescuento entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Regla de descuento no encontrada"));

        // Actualizar datos
        entity.setMontoMinimo(dto.montoMinimo());
        entity.setDescuentoPorcentaje(dto.descuentoPorcentaje());
        entity.setPrioridad(dto.prioridad());
        entity.setActivo(dto.activo());
        entity.setDescripcion(dto.descripcion());

        if (dto.canalId() != null)
            entity.setCanal(new Canal(dto.canalId()));

        if (dto.catalogoId() != null)
            entity.setCatalogo(new Catalogo(dto.catalogoId()));

        if (dto.clasifGralId() != null)
            entity.setClasifGral(new ClasifGral(dto.clasifGralId()));

        if (dto.clasifGastroId() != null)
            entity.setClasifGastro(new ClasifGastro(dto.clasifGastroId()));

        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    public void eliminar(Integer id) {
        repo.deleteById(id);
    }

    @Override
    public List<ReglaDescuentoDTO> listarPorCanal(Integer canalId) {
        return repo.findByIdCanalId(canalId).stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public List<ReglaDescuentoDTO> listarActivas(Integer canalId) {
        return repo.findByIdCanalIdAndActivoTrueOrderByPrioridadAsc(canalId).stream()
                .map(mapper::toDTO)
                .toList();
    }

}
