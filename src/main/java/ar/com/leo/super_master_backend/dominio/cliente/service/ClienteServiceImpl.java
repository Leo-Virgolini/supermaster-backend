package ar.com.leo.super_master_backend.dominio.cliente.service;

import ar.com.leo.super_master_backend.dominio.cliente.dto.ClienteDTO;
import ar.com.leo.super_master_backend.dominio.cliente.mapper.ClienteMapper;
import ar.com.leo.super_master_backend.dominio.cliente.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository repo;
    private final ClienteMapper mapper;

    @Override
    public List<ClienteDTO> listar() {
        return repo.findAll().stream().map(mapper::toDTO).toList();
    }
}