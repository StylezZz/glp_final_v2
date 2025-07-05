package pucp.edu.pe.glp_final.service;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import lombok.Getter;
import pucp.edu.pe.glp_final.models.Bloqueo;
import pucp.edu.pe.glp_final.models.SimulacionBloqueo;

@Service
@Getter
public class SimulacionBloqueoService {
    private SimulacionBloqueo simulacion;

    // Inicializa la simulación con la lista de bloqueos cargada
    // Se coloca un temporizador que ejecuta la simulacion cada x minutos
    public void iniciarSimulacion(List<Bloqueo> bloqueos, int horaInicial, int minutoInicial,
                                  int anio, int mes, int dia, int minutosPorIteracion, int timer) {
        simulacion = new SimulacionBloqueo(bloqueos, horaInicial, minutoInicial, anio, mes, dia,minutosPorIteracion, timer);
        simulacion.iniciarSimulacion();
    }

    // Detiene la simulación
    public void detenerSimulacion() {
        if (simulacion != null) {
            simulacion.detenerSimulacion();
        }
    }

    // Obtiene los bloqueos activos en la simulación
    public List<Bloqueo> obtenerBloqueosActivos() {
        if (simulacion != null) {
            return simulacion.bloqueosActivos();
        } else {
            return new ArrayList<>();
        }
    }

    public void establecerVelocidad(int minutosPorIteracion) {
        if (simulacion != null) {
            simulacion.setMinutosPorIteracion(minutosPorIteracion);
        }
    }

    // Obtiene la velocidad actual de la simulación.
    public int obtenerVelocidad() {
        return simulacion.getMinutosPorIteracion();
    }
}
