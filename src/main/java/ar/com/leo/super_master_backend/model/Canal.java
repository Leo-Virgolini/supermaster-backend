package ar.com.leo.super_master_backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "canales", schema = "supermaster")
public class Canal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_canal", nullable = false)
    private Integer id;

    @Size(max = 45)
    @NotNull
    @Column(name = "canal", nullable = false, length = 45)
    private String canal;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "id_canal_base")
    private Canal canalBase;

    @OneToMany(mappedBy = "canalBase")
    private Set<Canal> subcanales = new LinkedHashSet<>();

    @OneToMany(mappedBy = "canal", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CanalConcepto> canalConceptos = new LinkedHashSet<>();

    @OneToMany(mappedBy = "canal", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProductoCanal> productoCanales = new LinkedHashSet<>();

    @OneToMany(mappedBy = "canal", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProductoCanalPrecio> productoCanalPrecios = new LinkedHashSet<>();

    @OneToMany(mappedBy = "canal", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ReglaDescuento> reglasDescuentos = new LinkedHashSet<>();

}