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

    @GetMapping("/estadisticas/finalizacion")
    @ResponseBody
    public ResponseEntity<Object> estadisticasSimulacion(@RequestParam(required = false) int tipoSimulacion,
                                                         @RequestParam(required = false) int dia,
                                                         @RequestParam(required = false) int mes, @RequestParam(required = false) int anio,
                                                         @RequestParam(required = false) double timer) {

        int diaActual = ((int) timer / 1440);
        int horaActual = (((int) timer % 1440) / 60);
        int minutoActual = ((int) timer % 60);


        LocalDateTime fechaActual = LocalDateTime.of(anio, mes, diaActual, horaActual, minutoActual);
        LocalDateTime fechaSimulacion = LocalDateTime.of(anio, mes, dia, 0, 0);

        if(tipoSimulacion == 2){//Semanal
            fechaSimulacion = fechaSimulacion.plusHours(168);
            if(fechaActual.isEqual(fechaSimulacion) || fechaActual.isAfter(fechaSimulacion)){

                Map<String, Boolean> mapa = new HashMap<String, Boolean>();
                mapa.put("termino", true);
                return ResponseEntity.status(200).body(mapa);

            }else{
                Map<String, Boolean> mapa = new HashMap<String, Boolean>();
                mapa.put("termino", false);
                return ResponseEntity.status(200).body(mapa);
            }
        }else{
            if(tipoSimulacion == 1){

                fechaSimulacion = fechaSimulacion.plusHours(24);
                if(fechaActual.isEqual(fechaSimulacion) || fechaActual.isAfter(fechaSimulacion)){

                    Map<String, Boolean> mapa = new HashMap<String, Boolean>();
                    mapa.put("termino", true);
                    return ResponseEntity.status(200).body(mapa);

                }else{
                    Map<String, Boolean> mapa = new HashMap<String, Boolean>();
                    mapa.put("termino", false);
                    return ResponseEntity.status(200).body(mapa);
                }

            }else{
                if(tipoSimulacion==3){


                    return null;
                    //Por Implementar
                }
            }
        }
        return null;
    }

}
