package pucp.edu.pe.glp_final.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "nodo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Nodo {
    @Id
    private int id;

    private int x;
    private int y;
    private Boolean estaBloqueado;

    @OneToOne(mappedBy = "ubicacion")
    private Almacen almacen;

    public Nodo(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public boolean estaDisponible() {
        return !this.estaBloqueado;
    }

}
