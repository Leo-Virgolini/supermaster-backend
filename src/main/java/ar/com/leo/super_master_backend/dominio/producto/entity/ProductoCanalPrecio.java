package ar.com.leo.super_master_backend.dominio.producto.entity;

import ar.com.leo.super_master_backend.dominio.canal.entity.Canal;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "producto_canal_precios", schema = "supermaster")
public class ProductoCanalPrecio {

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
    @Column(name = "pvp", precision = 12, scale = 2)
    private BigDecimal pvp;

    @Column(name = "costo_total", precision = 12, scale = 2)
    private BigDecimal costoTotal;

    @Column(name = "ganancia_abs", precision = 12, scale = 2)
    private BigDecimal gananciaAbs;

    @Column(name = "ganancia_porcentaje", precision = 6, scale = 2)
    private BigDecimal gananciaPorcentaje;

    @Column(name = "gastos_total_porcentaje", precision = 6, scale = 2)
    private BigDecimal gastosTotalPorcentaje;

    @Column(name = "fecha_ultimo_calculo", columnDefinition = "datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Instant fechaUltimoCalculo;

}