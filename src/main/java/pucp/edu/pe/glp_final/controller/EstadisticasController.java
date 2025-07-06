package pucp.edu.pe.glp_final.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api")
@RestController
public class EstadisticasController {

    // Endpoint para verificar si la simulación ha finalizado
    @GetMapping("/estadisticas/finalizacion")
    @ResponseBody
    public ResponseEntity<Object> estadisticasSimulacion(@RequestParam(required = false) int tipoSimulacion,
                                                         @RequestParam(required = false) int dia,
                                                         @RequestParam(required = false) int mes, @RequestParam(required = false) int anio,
                                                         @RequestParam(required = false) double timer) {
        // Conversión del timer (minutos) a componentes temporales para fecha actual
        int diaActual = ((int) timer / 1440);
        int horaActual = (((int) timer % 1440) / 60);
        int minutoActual = ((int) timer % 60);

        // Construcción de fechas para comparación temporal
        LocalDateTime fechaActual = LocalDateTime.of(anio, mes, diaActual, horaActual, minutoActual);
        LocalDateTime fechaSimulacion = LocalDateTime.of(anio, mes, dia, 0, 0);

        if(tipoSimulacion == 2){//Semanal
            // SIMULACIÓN SEMANAL - 168 horas de operación continua
            // Evaluación para operaciones de distribución de larga duración
            // con múltiples ciclos de entrega y reabastecimiento
            fechaSimulacion = fechaSimulacion.plusHours(168);
            if(fechaActual.isEqual(fechaSimulacion) || fechaActual.isAfter(fechaSimulacion)){
                // Simulación semanal completada
                Map<String, Boolean> mapa = new HashMap<String, Boolean>();
                mapa.put("termino", true);
                return ResponseEntity.status(200).body(mapa);

            }else{
                // Simulación semanal en progreso
                Map<String, Boolean> mapa = new HashMap<String, Boolean>();
                mapa.put("termino", false);
                return ResponseEntity.status(200).body(mapa);
            }
        }else{
            if(tipoSimulacion == 1){
                // SIMULACIÓN DIARIA - 24 horas de operación estándar
                // Evaluación para operaciones diarias típicas de distribución urbana
                // con ciclo completo de planificación, ejecución y retorno a almacén
                fechaSimulacion = fechaSimulacion.plusHours(24);
                if(fechaActual.isEqual(fechaSimulacion) || fechaActual.isAfter(fechaSimulacion)){
                    // Simulación diaria completada
                    Map<String, Boolean> mapa = new HashMap<String, Boolean>();
                    mapa.put("termino", true);
                    return ResponseEntity.status(200).body(mapa);
                }else{
                    // Simulación diaria en progreso
                    Map<String, Boolean> mapa = new HashMap<String, Boolean>();
                    mapa.put("termino", false);
                    return ResponseEntity.status(200).body(mapa);
                }

            }else{
                if(tipoSimulacion==3){
                    // SIMULACIÓN PERSONALIZADA - Duración configurable
                    // Para casos especiales como eventos masivos, análisis específicos
                    // o validación de escenarios particulares de distribución

                    return null;
                }
            }
        }
        // Retorno por defecto para casos no manejados
        return null;
    }

}
