package ar.com.leo.super_master_backend.dominio.cliente.service;

import ar.com.leo.super_master_backend.dominio.cliente.dto.ClienteCreateDTO;
import ar.com.leo.super_master_backend.dominio.cliente.dto.ClienteDTO;
import ar.com.leo.super_master_backend.dominio.cliente.dto.ClienteUpdateDTO;

import java.util.List;

public interface ClienteService {

    List<ClienteDTO> listar();

    ClienteDTO obtener(Integer id);

    ClienteDTO crear(ClienteCreateDTO dto);

    ClienteDTO actualizar(Integer id, ClienteUpdateDTO dto);

    void eliminar(Integer id);
}