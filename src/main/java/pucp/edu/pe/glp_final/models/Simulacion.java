package pucp.edu.pe.glp_final.models;

import java.time.LocalDateTime;
import java.util.Timer;
import java.util.TimerTask;

import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class Simulacion {
    private Timer momento;
    private int minutosPorIteracion;
    private int horaActual;
    private int minutoActual;
    private int diaActual;
    private int anioActual;
    private int mesActual;
    private int timerSimulacion;

    public Simulacion(
            int horaInicial,
            int minutoInicial,
            int anio,
            int mes,
            int dia,
            int minutosPorIteracion,
            int momento
    ) {
        this.anioActual = anio;
        this.mesActual = mes;
        this.diaActual = dia;
        this.horaActual = horaInicial;
        this.minutoActual = minutoInicial;
        this.minutosPorIteracion = minutosPorIteracion;
        this.timerSimulacion = momento;
    }

    public void empezar() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                progresarMomento(minutosPorIteracion);
            }
        }, 0, timerSimulacion);
    }

    public void parar() {
        if (momento != null) {
            momento.cancel();
            momento.purge();
        }
    }

    /*
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
    */

    private void progresarMomento(int minutos) {
        LocalDateTime tiempo = LocalDateTime.of(anioActual, mesActual, diaActual, horaActual, minutoActual);
        tiempo = tiempo.plusMinutes(minutos);

        this.anioActual = tiempo.getYear();
        this.mesActual = tiempo.getMonthValue();
        this.diaActual = tiempo.getDayOfMonth();
        this.horaActual = tiempo.getHour();
        this.minutoActual = tiempo.getMinute();
    }

    /*
    private void progresarMomento(int minutos) {
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
    */
}
