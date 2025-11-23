package ar.com.leo.super_master_backend.dominio.concepto_gasto.service;

import ar.com.leo.super_master_backend.dominio.concepto_gasto.dto.ConceptoGastoDTO;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.mapper.ConceptoGastoMapper;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.repository.ConceptoGastoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConceptoGastoServiceImpl implements ConceptoGastoService {

    private final ConceptoGastoRepository repo;
    private final ConceptoGastoMapper mapper;

    @Override
    public List<ConceptoGastoDTO> listar() {
        return repo.findAll().stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public ConceptoGastoDTO obtener(Integer id) {
        return mapper.toDTO(
                repo.findById(id).orElseThrow(() ->
                        new RuntimeException("Concepto gasto no encontrado"))
        );
    }

}
