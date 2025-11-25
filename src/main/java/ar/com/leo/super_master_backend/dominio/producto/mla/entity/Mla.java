package ar.com.leo.super_master_backend.dominio.producto.mla.entity;

import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "mlas", schema = "supermaster")
public class Mla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mla", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @Size(max = 20)
    @NotNull
    @Column(name = "mla", nullable = false, length = 20)
    private String mla;

    @Column(name = "precio_envio", precision = 10, scale = 2)
    private BigDecimal precioEnvio;

}