package pucp.edu.pe.glp_final.service;

import pucp.edu.pe.glp_final.models.SimulacionBloqueo;
import org.springframework.stereotype.Service;
import lombok.Getter;

@Getter
@Service
public class SimulacionBloqueoService {

    private SimulacionBloqueo simulacion;

    public void empezar(
            int horaInicial,
            int minutoInicial,
            int anio,
            int mes,
            int dia,
            int minutosPorIteracion,
            int momento
    ) {
        simulacion = new SimulacionBloqueo(
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

    public int obtenerVelocidad() {
        return simulacion.getMinutosPorIteracion();
    }

    public void configurarVelocidad(int minutosPorIteracion) {
        if (simulacion != null) simulacion.setMinutosPorIteracion(minutosPorIteracion);
    }
}
