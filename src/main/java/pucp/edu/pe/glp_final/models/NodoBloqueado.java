package pucp.edu.pe.glp_final.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Builder
@AllArgsConstructor
@Setter
@Getter
@NoArgsConstructor

public class NodoBloqueado {
    private int id;
    private int x_ini;
    private int y_ini;
    private int x_fin;
    private int y_fin;

}
