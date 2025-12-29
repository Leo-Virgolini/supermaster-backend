package ar.com.leo.super_master_backend.dominio.promocion.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "promociones", schema = "supermaster", uniqueConstraints = @UniqueConstraint(name = "uq_promociones_codigo", columnNames = {
                "codigo" }), indexes = @Index(name = "idx_promociones_tipo", columnList = "tipo"))
public class Promocion {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "id", nullable = false)
        private Integer id;

        @Size(max = 20)
        @NotNull
        @Column(name = "codigo", nullable = false, length = 20, unique = true)
        private String codigo;

        @NotNull
        @Enumerated(EnumType.STRING)
        @Column(name = "tipo", nullable = false, columnDefinition = "ENUM('MULTIPLICADOR','DESCUENTO_PORC','DIVISOR','PRECIO_FIJO')")
        private TipoPromocionTabla tipo;

        @NotNull
        @Column(name = "valor", nullable = false, precision = 6, scale = 3)
        private BigDecimal valor;

        public Promocion(Integer id) {
                this.id = id;
        }
}
