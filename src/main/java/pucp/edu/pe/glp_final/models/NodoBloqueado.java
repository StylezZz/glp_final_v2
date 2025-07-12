package pucp.edu.pe.glp_final.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "tramo_bloqueo")
public class NodoBloqueado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private int x_ini;
    private int y_ini;
    private int x_fin;
    private int y_fin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bloqueo_id")
    @JsonBackReference
    private Bloqueo bloqueo;
}
