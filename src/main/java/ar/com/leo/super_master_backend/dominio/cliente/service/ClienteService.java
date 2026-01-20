package ar.com.leo.super_master_backend.dominio.cliente.service;

import ar.com.leo.super_master_backend.dominio.cliente.dto.ClienteCreateDTO;
import ar.com.leo.super_master_backend.dominio.cliente.dto.ClienteDTO;
import ar.com.leo.super_master_backend.dominio.cliente.dto.ClienteUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClienteService {

    Page<ClienteDTO> listar(String search, Pageable pageable);

    ClienteDTO obtener(Integer id);

    ClienteDTO crear(ClienteCreateDTO dto);

    ClienteDTO actualizar(Integer id, ClienteUpdateDTO dto);

    void eliminar(Integer id);
}