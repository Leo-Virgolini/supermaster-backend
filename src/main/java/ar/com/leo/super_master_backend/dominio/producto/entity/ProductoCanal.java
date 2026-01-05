package ar.com.leo.super_master_backend.dominio.producto.entity;

import java.math.BigDecimal;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import ar.com.leo.super_master_backend.dominio.canal.entity.Canal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "producto_canal", schema = "supermaster", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"id_producto", "id_canal"}))
public class ProductoCanal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    // ---------------------------
    // RELACIÓN CON PRODUCTO
    // ---------------------------
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    // ---------------------------
    // RELACIÓN CON CANAL
    // ---------------------------
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "id_canal", nullable = false)
    private Canal canal;

    // ---------------------------
    // CAMPOS DE LA ENTIDAD
    // ---------------------------
    @NotNull
    @Column(name = "margen_porcentaje", nullable = false, precision = 6, scale = 3)
    private BigDecimal margenPorcentaje;

    @Column(name = "margen_fijo", precision = 10, scale = 2)
    private BigDecimal margenFijo;

    @ColumnDefault("0")
    @Column(name = "usa_canal_base")
    private Boolean usaCanalBase;

    @ColumnDefault("1")
    @Column(name = "aplica_cuotas")
    private Boolean aplicaCuotas;

    @ColumnDefault("1")
    @Column(name = "aplica_comision")
    private Boolean aplicaComision;

    @Size(max = 300)
    @Column(name = "notas", length = 300)
    private String notas;

}