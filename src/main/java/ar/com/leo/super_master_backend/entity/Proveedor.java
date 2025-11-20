package ar.com.leo.super_master_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "proveedores", schema = "supermaster")
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proveedor", nullable = false)
    private Integer id;

    @Size(max = 100)
    @NotNull
    @Column(name = "proveedor", nullable = false, length = 100)
    private String proveedor;

    @Size(max = 50)
    @NotNull
    @Column(name = "apodo", nullable = false, length = 50)
    private String apodo;

    @Size(max = 45)
    @Column(name = "plazo_pago", length = 45)
    private String plazoPago;

    @Column(name = "entrega")
    private Boolean entrega;

    @OneToMany(mappedBy = "proveedor")
    private Set<Producto> productos = new LinkedHashSet<>();

}