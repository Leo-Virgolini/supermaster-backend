package ar.com.leo.super_master_backend.dominio.usuario.service;

import ar.com.leo.super_master_backend.dominio.common.exception.ConflictException;
import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import ar.com.leo.super_master_backend.dominio.usuario.dto.*;
import ar.com.leo.super_master_backend.dominio.usuario.entity.Rol;
import ar.com.leo.super_master_backend.dominio.usuario.entity.Usuario;
import ar.com.leo.super_master_backend.dominio.usuario.mapper.UsuarioMapper;
import ar.com.leo.super_master_backend.dominio.usuario.repository.RolRepository;
import ar.com.leo.super_master_backend.dominio.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioDTO> listar(String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return usuarioRepository
                    .findByUsernameContainingIgnoreCaseOrNombreCompletoContainingIgnoreCase(search, search, pageable)
                    .map(usuarioMapper::toDTO);
        }
        return usuarioRepository.findAll(pageable).map(usuarioMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioDTO obtener(Integer id) {
        return usuarioMapper.toDTO(findUsuarioById(id));
    }

    @Override
    @Transactional
    public UsuarioDTO crear(UsuarioCreateDTO dto) {
        if (usuarioRepository.existsByUsername(dto.username())) {
            throw new ConflictException("Ya existe un usuario con username: " + dto.username());
        }

        Rol rol = rolRepository.findById(dto.rolId())
                .orElseThrow(() -> new NotFoundException("Rol no encontrado con id: " + dto.rolId()));

        Usuario usuario = new Usuario();
        usuario.setUsername(dto.username());
        usuario.setPasswordHash(passwordEncoder.encode(dto.password()));
        usuario.setNombreCompleto(dto.nombreCompleto());
        usuario.setActivo(true);
        usuario.setRol(rol);

        return usuarioMapper.toDTO(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public UsuarioDTO actualizar(Integer id, UsuarioUpdateDTO dto) {
        Usuario usuario = findUsuarioById(id);

        if (dto.nombreCompleto() != null) {
            usuario.setNombreCompleto(dto.nombreCompleto());
        }
        if (dto.activo() != null) {
            usuario.setActivo(dto.activo());
        }
        if (dto.rolId() != null) {
            Rol rol = rolRepository.findById(dto.rolId())
                    .orElseThrow(() -> new NotFoundException("Rol no encontrado con id: " + dto.rolId()));
            usuario.setRol(rol);
        }

        return usuarioMapper.toDTO(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public void cambiarPassword(Integer id, CambioPasswordDTO dto) {
        Usuario usuario = findUsuarioById(id);
        usuario.setPasswordHash(passwordEncoder.encode(dto.nuevaPassword()));
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        Usuario usuario = findUsuarioById(id);
        usuarioRepository.delete(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RolDTO> listarRoles() {
        return rolRepository.findAll().stream()
                .map(usuarioMapper::toRolDTO)
                .toList();
    }

    private Usuario findUsuarioById(Integer id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado con id: " + id));
    }
}
