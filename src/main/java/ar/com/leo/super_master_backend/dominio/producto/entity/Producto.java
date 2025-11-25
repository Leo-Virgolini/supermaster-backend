package ar.com.leo.super_master_backend.dominio.producto.entity;

import ar.com.leo.super_master_backend.dominio.clasif_gastro.entity.ClasifGastro;
import ar.com.leo.super_master_backend.dominio.clasif_gral.entity.ClasifGral;
import ar.com.leo.super_master_backend.dominio.marca.entity.Marca;
import ar.com.leo.super_master_backend.dominio.material.entity.Material;
import ar.com.leo.super_master_backend.dominio.producto.mla.entity.Mla;
import ar.com.leo.super_master_backend.dominio.origen.entity.Origen;
import ar.com.leo.super_master_backend.dominio.proveedor.entity.Proveedor;
import ar.com.leo.super_master_backend.dominio.tipo.entity.Tipo;
import ar.com.leo.super_master_backend.entity.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "productos", schema = "supermaster")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto", nullable = false)
    private Integer id;

    @Size(max = 45)
    @NotNull
    @Column(name = "sku", nullable = false, length = 45)
    private String sku;

    @Size(max = 45)
    @Column(name = "cod_ext", length = 45)
    private String codExt;

    @Size(max = 100)
    @NotNull
    @Column(name = "descripcion", nullable = false, length = 100)
    private String descripcion;

    @Size(max = 100)
    @NotNull
    @Column(name = "titulo_web", nullable = false, length = 100)
    private String tituloWeb;

    @Column(name = "es_combo")
    private Boolean esCombo;

    // ---------------------------
    // RELACIONES MANY TO ONE
    // ---------------------------

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_marca")
    private Marca marca;

    @Column(name = "uxb")
    private Integer uxb;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_origen", nullable = false)
    private Origen origen;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_clasif_gral", nullable = false)
    private ClasifGral clasifGral;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_clasif_gastro")
    private ClasifGastro clasifGastro;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tipo", nullable = false)
    private Tipo tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proveedor")
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_material")
    private Material material;

    // ---------------------------
    // ATRIBUTOS NUMÉRICOS Y OTROS
    // ---------------------------

    @Size(max = 45)
    @Column(name = "capacidad", length = 45)
    private String capacidad;

    @Column(name = "largo", precision = 10, scale = 2)
    private BigDecimal largo;

    @Column(name = "ancho", precision = 10, scale = 2)
    private BigDecimal ancho;

    @Column(name = "alto", precision = 10, scale = 2)
    private BigDecimal alto;

    @Size(max = 45)
    @Column(name = "diamboca", length = 45)
    private String diamboca;

    @Size(max = 45)
    @Column(name = "diambase", length = 45)
    private String diambase;

    @Size(max = 45)
    @Column(name = "espesor", length = 45)
    private String espesor;

    @Column(name = "costo", precision = 10, scale = 2)
    private BigDecimal costo;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "fecha_ult_costo")
    private Instant fechaUltCosto;

    @NotNull
    @Column(name = "iva", nullable = false, precision = 5, scale = 2)
    private BigDecimal iva;

    // ---------------------------
    // RELACIONES ONE TO MANY
    // ---------------------------

    @OneToMany(mappedBy = "producto")
    private Set<Mla> mlas = new LinkedHashSet<>();

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProductoApto> productosApto = new LinkedHashSet<>();

    @OneToMany(mappedBy = "producto")
    private Set<ProductoCanal> productoCanales = new LinkedHashSet<>();

    @OneToMany(mappedBy = "producto")
    private Set<ProductoCanalPrecio> productoCanalPrecios = new LinkedHashSet<>();

    @OneToMany(mappedBy = "producto")
    private Set<ProductoCatalogo> productoCatalogos = new LinkedHashSet<>();

    @OneToMany(mappedBy = "producto")
    private Set<ProductoCliente> productoClientes = new LinkedHashSet<>();

    public Producto(Integer id) {
        this.id = id;
    }

}
