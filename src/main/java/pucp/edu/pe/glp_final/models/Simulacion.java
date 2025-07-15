package pucp.edu.pe.glp_final.models;

import java.util.Timer;
import java.util.TimerTask;
import java.time.LocalDateTime;

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

    private void progresarMomento(int minutos) {
        LocalDateTime tiempo = LocalDateTime.of(anioActual, mesActual, diaActual, horaActual, minutoActual);
        tiempo = tiempo.plusMinutes(minutos);

        this.anioActual = tiempo.getYear();
        this.mesActual = tiempo.getMonthValue();
        this.diaActual = tiempo.getDayOfMonth();
        this.horaActual = tiempo.getHour();
        this.minutoActual = tiempo.getMinute();
    }
}
