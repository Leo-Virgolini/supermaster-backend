package ar.com.leo.super_master_backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "catalogos", schema = "supermaster")
public class Catalogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_catalogo", nullable = false)
    private Integer id;

    @Size(max = 45)
    @NotNull
    @Column(name = "catalogo", nullable = false, length = 45)
    private String catalogo;

    // --------------------------
    // RELACIÓN CON PRODUCTOS
    // --------------------------
    @OneToMany(mappedBy = "catalogo", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProductoCatalogo> productoCatalogos = new LinkedHashSet<>();

    // --------------------------
    // RELACIÓN CON REGLAS
    // --------------------------
    @OneToMany(mappedBy = "catalogo", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ReglaDescuento> reglasDescuentos = new LinkedHashSet<>();

}