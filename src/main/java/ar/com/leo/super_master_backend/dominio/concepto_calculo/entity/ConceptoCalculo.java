package ar.com.leo.super_master_backend.dominio.concepto_calculo.entity;

import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConcepto;
import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConceptoRegla;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "conceptos_calculo", schema = "supermaster")
public class ConceptoCalculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_concepto", nullable = false)
    private Integer id;

    @Size(max = 45)
    @NotNull
    @Column(name = "concepto", nullable = false, length = 45)
    private String concepto;

    @NotNull
    @Column(name = "porcentaje", nullable = false, precision = 6, scale = 3)
    private BigDecimal porcentaje;

    @ColumnDefault("'COMISION_SOBRE_PVP'")
    @Enumerated(EnumType.STRING)
    @Column(name = "aplica_sobre", columnDefinition = "ENUM('GASTO_SOBRE_COSTO','FLAG_FINANCIACION_PROVEEDOR','AJUSTE_MARGEN_PUNTOS','AJUSTE_MARGEN_PROPORCIONAL','FLAG_USAR_MARGEN_MINORISTA','FLAG_USAR_MARGEN_MAYORISTA','GASTO_POST_GANANCIA','FLAG_APLICAR_IVA','IMPUESTO_ADICIONAL','GASTO_POST_IMPUESTOS','FLAG_INCLUIR_ENVIO','COMISION_SOBRE_PVP','CALCULO_SOBRE_CANAL_BASE','RECARGO_CUPON','DESCUENTO_PORCENTUAL','INFLACION_DIVISOR','FLAG_APLICAR_PROMOCIONES') DEFAULT 'COMISION_SOBRE_PVP'")
    private AplicaSobre aplicaSobre;

    @Size(max = 255)
    @Column(name = "descripcion")
    private String descripcion;

    // ----------------------------------------
    // RELACIÓN CON CANALES
    // ----------------------------------------
    // Los conceptos se asocian a canales a través de la tabla canal_concepto
    // Un concepto puede estar asociado a múltiples canales

    @OneToMany(mappedBy = "concepto", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CanalConcepto> canalConceptos = new LinkedHashSet<>();

    // ----------------------------------------
    // RELACIÓN CON REGLAS DE CANAL-CONCEPTO
    // ----------------------------------------
    @OneToMany(mappedBy = "concepto")
    private Set<CanalConceptoRegla> canalConceptoReglas = new LinkedHashSet<>();

    public ConceptoCalculo(Integer id) {
        this.id = id;
    }

}
