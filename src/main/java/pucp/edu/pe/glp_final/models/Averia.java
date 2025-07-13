package pucp.edu.pe.glp_final.models;

import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pucp.edu.pe.glp_final.models.enums.TipoIncidente;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Component
public class Averia {
    private int id;
    private int turnoAveria;
    private String codigoCamion;
    private TipoIncidente tipoAveria;
    private String descripcion;

    public static Averia leerRegistro(String registro) {
        Averia averia = new Averia();
        String[] partes = registro.split("_");
        if (partes.length != 3)
            throw new IllegalArgumentException("Formato de registro incorrecto: " + registro);

        String[] turno = partes[0].split("t");
        if (turno.length != 2)
            throw new IllegalArgumentException("Formato de turno incorrecto: " + partes[0]);

        averia.setTurnoAveria(Integer.parseInt(turno[1]));
        averia.setCodigoCamion(partes[1]);
        String[] tipoIncidente = partes[2].split("ti");

        averia.setTipoAveria(TipoIncidente.values()[Integer.parseInt(tipoIncidente[1])]);

        return averia;
    }

}