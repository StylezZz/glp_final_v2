package pucp.edu.pe.glp_final.models;

import java.time.LocalDateTime;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Setter
@Getter
@Table(name = "pedido")

public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private int dia;
    private int hora;
    private int minuto;

    private int posX;
    private int posY;
    private String idCliente;

    private int cantidadGLP; // En m3
    private int horasLimite; // En horas
    private boolean entregado; // Si el pedido ha sido entregado o no
    private int cantidadGLPAsignada;
    private boolean asignado; // Si el pedido ha sido asignado a un camion o no
    private int horaDeInicio;
    private int anio;
    private int mesPedido;
    private int tiempoLlegada;
    private String idCamion;
    private boolean entregadoCompleto;
    private LocalDateTime fechaDeRegistro;
    private LocalDateTime fechaEntrega;
    private boolean isbloqueo;
    private double priodidad;
    private LocalDateTime fecDia;
    private String tiempoRegistroStr;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    public void setCantidadGLPAsignada(int cantidadGLPAsignada) {
        this.cantidadGLPAsignada += cantidadGLPAsignada;
    }

    public Pedido(int dia, int hora, int minuto, int anio, int mes, int posX, int posY, String idCliente,
                  int cantidadGLP, int horasLimite, boolean entregado) {
        this.dia = dia;
        this.hora = hora;
        this.minuto = minuto;
        this.posX = posX;
        this.posY = posY;
        this.idCliente = idCliente;
        this.cantidadGLP = cantidadGLP;
        this.horasLimite = horasLimite;
        this.asignado = false;
        this.entregado = entregado;
        this.cantidadGLPAsignada = 0;
        this.horaDeInicio = hora * 60 + minuto + dia * 1440;
        this.anio = anio;
        this.mesPedido = mes;
        this.tiempoLlegada = this.horaDeInicio + this.horasLimite * 60;
        this.fechaDeRegistro = LocalDateTime.of(anio, mes, dia, hora, minuto);
        this.fechaEntrega = fechaDeRegistro.plusHours(horasLimite);
        this.isbloqueo = false;
        this.fecDia = LocalDateTime.now();
    }

    public static Pedido leerRegistro(String registro,int anio,int mes, int id){

        Pedido pedido=new Pedido();
        pedido.setAnio(anio);
        pedido.setMesPedido(mes);
        // Dividir el registro en partes usando ":" como separador
        String[] partes = registro.split(":");

        // Verificar si el registro tiene el formato esperado
        if (partes.length != 2) {
            throw new IllegalArgumentException("Formato de registro incorrecto: " + registro);
        }

        // Formato de tiempo (##d##h##m)
        String[] tiempo = partes[0].split("[dhm]");
        if (tiempo.length != 3) {
            throw new IllegalArgumentException("Formato de tiempo incorrecto: " + partes[0]);
        }

        pedido.setDia(Integer.parseInt(tiempo[0]));
        pedido.setHora( Integer.parseInt(tiempo[1]));
        pedido.setMinuto(Integer.parseInt(tiempo[2]));

        // Formato de ubicación (posX,posY)
        String[] ubicacion = partes[1].split(",");
        if (ubicacion.length != 5) {
            throw new IllegalArgumentException("Formato de ubicación incorrecto: " + partes[1]);
        }

        pedido.setPosX(Integer.parseInt(ubicacion[0]));
        pedido.setPosY(Integer.parseInt(ubicacion[1]));

        // Formato de cliente (c-idCliente)
        pedido.setIdCliente(ubicacion[2]);

        //no solo está le número sino también m3
        String[] cantidad = ubicacion[3].split("m3");
        pedido.setCantidadGLP(Integer.parseInt(cantidad[0]));
        pedido.setHorasLimite(Integer.parseInt(ubicacion[4].replaceAll("[^0-9]", "")));
        pedido.setEntregado(false);
        pedido.setAsignado(false);
        pedido.setHoraDeInicio(pedido.getHora()*60 + pedido.getMinuto() + pedido.getDia() * 1440);
        pedido.setTiempoLlegada(pedido.getHoraDeInicio() + pedido.getHorasLimite()*60);

        LocalDateTime fechaDeRegistro = LocalDateTime.of(anio,mes,pedido.getDia(),pedido.getHora(),pedido.getMinuto());
        LocalDateTime fechaEntrega = fechaDeRegistro.plusHours(pedido.getHorasLimite());
        pedido.setFechaDeRegistro(fechaDeRegistro);
        pedido.setFechaEntrega(fechaEntrega);

        // Guardar el string original del tiempo de registro
        pedido.setTiempoRegistroStr(partes[0]);

        return pedido;
    }
}
