package ar.com.leo.super_master_backend.dominio.clasif_gastro.entity;

import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import ar.com.leo.super_master_backend.dominio.regla_descuento.entity.ReglaDescuento;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "clasif_gastro", schema = "supermaster")
public class ClasifGastro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_clasif_gastro", nullable = false)
    private Integer id;

    @Size(max = 45)
    @NotNull
    @Column(name = "nombre", nullable = false, length = 45)
    private String nombre;

    // -------------------------------------
    // RELACIÓN JERÁRQUICA (PADRE - HIJOS)
    // -------------------------------------
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "id_padre")
    private ClasifGastro padre;

    @OneToMany(mappedBy = "padre")
    private Set<ClasifGastro> subclasificaciones = new LinkedHashSet<>();

    // -------------------------------------
    // RELACIÓN CON PRODUCTOS
    // -------------------------------------
    @OneToMany(mappedBy = "clasifGastro")
    private Set<Producto> productos = new LinkedHashSet<>();

    // -------------------------------------
    // RELACIÓN CON REGLAS DESCUENTO
    // -------------------------------------
    @OneToMany(mappedBy = "clasifGastro")
    private Set<ReglaDescuento> reglasDescuentos = new LinkedHashSet<>();

    public ClasifGastro(Integer id) {
        this.id = id;
    }

}