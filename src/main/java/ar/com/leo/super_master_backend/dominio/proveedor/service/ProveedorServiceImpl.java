package ar.com.leo.super_master_backend.dominio.proveedor.service;

import ar.com.leo.super_master_backend.dominio.proveedor.dto.ProveedorDTO;
import ar.com.leo.super_master_backend.dominio.proveedor.mapper.ProveedorMapper;
import ar.com.leo.super_master_backend.dominio.proveedor.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProveedorServiceImpl implements ProveedorService {

    private final ProveedorRepository repo;
    private final ProveedorMapper mapper;

    @Override
    public List<ProveedorDTO> listar() {
        return repo.findAll().stream().map(mapper::toDTO).toList();
    }

}