package ar.com.leo.super_master_backend.dominio.cliente.service;

import ar.com.leo.super_master_backend.dominio.cliente.dto.ClienteDTO;

import java.util.List;

public interface ClienteService {
    List<ClienteDTO> listar();
}