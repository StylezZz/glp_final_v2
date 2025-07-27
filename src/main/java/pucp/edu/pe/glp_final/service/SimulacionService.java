package pucp.edu.pe.glp_final.service;

import pucp.edu.pe.glp_final.models.Simulacion;
import org.springframework.stereotype.Service;
import lombok.Getter;

@Getter
@Service
public class SimulacionService {

    private Simulacion simulacion;

    public void empezar(
            int horaInicial,
            int minutoInicial,
            int anio,
            int mes,
            int dia,
            int minutosPorIteracion,
            int momento
    ) {
        simulacion = new Simulacion(
                horaInicial,
                minutoInicial,
                anio,
                mes,
                dia,
                minutosPorIteracion,
                momento
        );
        simulacion.empezar();
    }

    public void parar() {
        if (simulacion != null) simulacion.parar();
    }

}
