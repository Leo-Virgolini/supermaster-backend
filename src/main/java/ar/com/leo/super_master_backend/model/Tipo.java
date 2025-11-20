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
@Table(name = "tipos", schema = "supermaster")
public class Tipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo", nullable = false)
    private Integer id;

    @Size(max = 45)
    @NotNull
    @Column(name = "nombre", nullable = false, length = 45)
    private String nombre;

    // -------------------------------------
    // RELACIÓN JERÁRQUICA (PADRE - HIJOS)
    // -------------------------------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_padre")
    private Tipo padre;

    @OneToMany(mappedBy = "padre")
    private Set<Tipo> subTipos = new LinkedHashSet<>();

    // -------------------------------------
    // RELACIÓN CON PRODUCTOS
    // -------------------------------------
    @OneToMany(mappedBy = "tipo")
    private Set<Producto> productos = new LinkedHashSet<>();

}