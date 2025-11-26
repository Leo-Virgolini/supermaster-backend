package ar.com.leo.super_master_backend.dominio.catalogo.controller;


import ar.com.leo.super_master_backend.dominio.catalogo.dto.CatalogoCreateDTO;
import ar.com.leo.super_master_backend.dominio.catalogo.dto.CatalogoDTO;
import ar.com.leo.super_master_backend.dominio.catalogo.dto.CatalogoUpdateDTO;
import ar.com.leo.super_master_backend.dominio.catalogo.service.CatalogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/catalogos")
public class CatalogoController {

    private final CatalogoService service;

    // ===============================
    // LISTAR
    // ===============================
    @GetMapping
    public ResponseEntity<List<CatalogoDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    // ===============================
    // OBTENER UNO
    // ===============================
    @GetMapping("/{id}")
    public ResponseEntity<CatalogoDTO> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    // ===============================
    // CREAR
    // ===============================
    @PostMapping
    public ResponseEntity<CatalogoDTO> crear(@RequestBody CatalogoCreateDTO dto) {
        return ResponseEntity.ok(service.crear(dto));
    }

    // ===============================
    // ACTUALIZAR
    // ===============================
    @PutMapping("/{id}")
    public ResponseEntity<CatalogoDTO> actualizar(
            @PathVariable Integer id,
            @RequestBody CatalogoUpdateDTO dto
    ) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    // ===============================
    // ELIMINAR
    // ===============================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

}