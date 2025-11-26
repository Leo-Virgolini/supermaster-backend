package ar.com.leo.super_master_backend.dominio.origen.controller;

import ar.com.leo.super_master_backend.dominio.origen.dto.OrigenCreateDTO;
import ar.com.leo.super_master_backend.dominio.origen.dto.OrigenDTO;
import ar.com.leo.super_master_backend.dominio.origen.dto.OrigenUpdateDTO;
import ar.com.leo.super_master_backend.dominio.origen.service.OrigenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/origenes")
public class OrigenController {

    private final OrigenService service;

    @GetMapping
    public ResponseEntity<List<OrigenDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrigenDTO> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @PostMapping
    public ResponseEntity<OrigenDTO> crear(@RequestBody OrigenCreateDTO dto) {
        return ResponseEntity.ok(service.crear(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrigenDTO> actualizar(
            @PathVariable Integer id,
            @RequestBody OrigenUpdateDTO dto
    ) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

}