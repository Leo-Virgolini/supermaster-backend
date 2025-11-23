package ar.com.leo.super_master_backend.dominio.cliente.entity;

import ar.com.leo.super_master_backend.dominio.entity.ProductoCliente;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "clientes", schema = "supermaster")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente", nullable = false)
    private Integer id;

    @Size(max = 45)
    @NotNull
    @Column(name = "cliente", nullable = false, length = 45)
    private String cliente;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProductoCliente> productoClientes = new LinkedHashSet<>();

    public Cliente(Integer idCliente) {
        this.id = idCliente;
    }

}