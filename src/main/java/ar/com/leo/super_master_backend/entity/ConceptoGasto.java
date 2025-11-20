package ar.com.leo.super_master_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
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
    @Column(name = "porcentaje", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentaje;

    @ColumnDefault("'PVP'")
    @Lob
    @Column(name = "aplica_sobre")
    private String aplicaSobre;

    @Size(max = 2)
    @Column(name = "cuotas", length = 2)
    private String cuotas;

    @OneToMany(mappedBy = "concepto", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CanalConcepto> canalConceptos = new LinkedHashSet<>();

}