package ar.com.leo.super_master_backend.dominio.concepto_gasto.entity;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.annotations.ColumnDefault;

import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConcepto;
import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConceptoRegla;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "conceptos_gastos", schema = "supermaster")
public class ConceptoGasto {

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

    @ColumnDefault("'PVP'")
    @Enumerated(EnumType.STRING)
    @Column(name = "aplica_sobre", columnDefinition = "ENUM('COSTO','PVP','COSTO_IVA','AUMENTA_MARGEN','REDUCE_MARGEN','IMP','RECARGO_CUPON','DESCUENTO','ENVIO','INFLACION','PROVEEDOR_FIN','COSTO_GANANCIA') DEFAULT 'PVP'")
    private AplicaSobre aplicaSobre;

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

    public ConceptoGasto(Integer id) {
        this.id = id;
    }

}