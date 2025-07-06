package pucp.edu.pe.glp_final.models;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class SimulacionBloqueo {
    private Timer timer;                        // Timer para la simulacion
    private int minutosPorIteracion;
    private int horaActual;
    private int minutoActual;
    private int diaActual;
    private int anioActual;
    private int mesActual;
    private int timerSimulacion;
    private MetricasSimulacion metricas = new MetricasSimulacion();

    public SimulacionBloqueo(
            int horaInicial,
            int minutoInicial,
            int anio,
            int mes,
            int dia,
            int minutosPorIteracion,
            int timer
    ) {
        this.anioActual = anio;
        this.mesActual = mes;
        this.diaActual = dia;
        this.horaActual = horaInicial;
        this.minutoActual = minutoInicial;
        this.minutosPorIteracion = minutosPorIteracion;
        this.timerSimulacion = timer;
    }

    public void empezar() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                avanzarTiempo(minutosPorIteracion); // Avanza el tiempo en 1 minuto
            }
        }, 0, timerSimulacion); //Inicia la tarea inmediatamente y repítela cada segundo (1000 ms).
    }

    public void parar() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
        }
    }

    // Función para obtener el número de días en un mes para el avance de tiempo
    private int obtenerDiasEnMes(int anio, int mes) {
        if (mes == 2) {
            // Febrero: 28 o 29 días dependiendo de si es un año bisiesto.
            if ((anio % 4 == 0 && anio % 100 != 0) || (anio % 400 == 0)) {
                return 29; // Año bisiesto.
            } else {
                return 28; // Año no bisiesto.
            }
        } else if (mes == 4 || mes == 6 || mes == 9 || mes == 11) {
            return 30; // Meses con 30 días.
        } else {
            return 31; // Meses con 31 días.
        }
    }

    // Avanza el tiempo simulado (puedes ajustar la velocidad de la simulación
    // aquí).
    private void avanzarTiempo(int minutos) {
        minutoActual += minutos;

        // Verifica si es necesario ajustar la hora si los minutos superan 59.
        if (minutoActual >= 60) {
            horaActual++;
            minutoActual = minutoActual % 60;
        }

        // Verifica si es necesario ajustar el día si las horas superan 23.
        if (horaActual >= 24) {
            diaActual++;
            horaActual = horaActual % 24;
        }

        // Verifica si es necesario ajustar el mes si se supera el último día del mes.
        int diasEnEsteMes = obtenerDiasEnMes(anioActual, mesActual);
        if (diaActual > diasEnEsteMes) {
            mesActual++;
            diaActual = 1;
        }

        // Verifica si es necesario ajustar el año si se supera el último mes del año.
        if (mesActual > 12) {
            anioActual++;
            mesActual = 1;
        }
    }
}
