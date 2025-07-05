package pucp.edu.pe.glp_final.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table(name = "almacen")
@Getter
@Setter
@Entity
@NoArgsConstructor
public class Almacen {

    @Id
    private int id;
    private String nombre;
    private int capacidad;

    @OneToOne
    @JoinColumn(name = "nodo_id")
    private Nodo ubicacion;

    private int capacidadDisponible;
    private int capacidadFicticia;

    public Almacen(int id, String nombre, int capacidad, Nodo ubicacion) {
        this.id = id;
        this.nombre = nombre;
        this.capacidad = capacidad;
        this.ubicacion = ubicacion;
        this.capacidadDisponible = capacidad;
        this.capacidadFicticia = Integer.MAX_VALUE;
    }
}
