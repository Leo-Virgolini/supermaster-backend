package ar.com.leo.super_master_backend.dominio.catalogo.service;

import ar.com.leo.super_master_backend.dominio.catalogo.dto.CatalogoDTO;
import ar.com.leo.super_master_backend.dominio.catalogo.entity.Catalogo;
import ar.com.leo.super_master_backend.dominio.catalogo.repository.CatalogoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogoServiceImpl implements CatalogoService {

    private final CatalogoRepository repo;

    @Override
    public List<CatalogoDTO> listar() {
        return repo.findAll().stream()
                .map(c -> new CatalogoDTO(c.getId(), c.getCatalogo()))
                .toList();
    }

    @Override
    public CatalogoDTO crear(String nombre) {
        Catalogo c = new Catalogo();
        c.setCatalogo(nombre);
        repo.save(c);
        return new CatalogoDTO(c.getId(), c.getCatalogo());
    }
}