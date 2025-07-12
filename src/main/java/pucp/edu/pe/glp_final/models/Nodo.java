package pucp.edu.pe.glp_final.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "nodo")
public class Nodo {
    @Id
    private int id;

    private int x;
    private int y;

    @OneToOne(mappedBy = "ubicacion")
    private Almacen almacen;

    public Nodo(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
