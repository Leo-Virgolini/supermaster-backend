package ar.com.leo.super_master_backend.dominio.clasif_gastro.service;

import ar.com.leo.super_master_backend.dominio.clasif_gastro.dto.ClasifGastroCreateDTO;
import ar.com.leo.super_master_backend.dominio.clasif_gastro.dto.ClasifGastroDTO;
import ar.com.leo.super_master_backend.dominio.clasif_gastro.dto.ClasifGastroUpdateDTO;
import ar.com.leo.super_master_backend.dominio.clasif_gastro.entity.ClasifGastro;
import ar.com.leo.super_master_backend.dominio.clasif_gastro.mapper.ClasifGastroMapper;
import ar.com.leo.super_master_backend.dominio.clasif_gastro.repository.ClasifGastroRepository;
import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import ar.com.leo.super_master_backend.dominio.producto.calculo.service.RecalculoPrecioFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ClasifGastroServiceImpl implements ClasifGastroService {

    private final ClasifGastroRepository repo;
    private final ClasifGastroMapper mapper;
    private final RecalculoPrecioFacade recalculoFacade;

    @Override
    @Transactional(readOnly = true)
    public Page<ClasifGastroDTO> listar(String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return repo.findByNombreContainingIgnoreCase(search, pageable)
                    .map(mapper::toDTO);
        }
        return repo.findAll(pageable)
                .map(mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public ClasifGastroDTO obtener(Integer id) {
        return repo.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new NotFoundException("Clasificación Gastro no encontrada"));
    }

    @Override
    @Transactional
    public ClasifGastroDTO crear(ClasifGastroCreateDTO dto) {
        ClasifGastro entity = mapper.toEntity(dto);
        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public ClasifGastroDTO actualizar(Integer id, ClasifGastroUpdateDTO dto) {
        ClasifGastro entity = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Clasificación Gastro no encontrada"));

        Boolean esMaquinaAnterior = entity.getEsMaquina();

        mapper.updateEntityFromDTO(dto, entity);
        repo.save(entity);

        // Si cambió esMaquina, recalcular todos los productos de esta clasificación
        if (dto.esMaquina() != null && !Objects.equals(dto.esMaquina(), esMaquinaAnterior)) {
            recalculoFacade.recalcularPorCambioClasifGastro(id);
        }

        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Clasificación Gastro no encontrada");
        }
        repo.deleteById(id);
    }
}