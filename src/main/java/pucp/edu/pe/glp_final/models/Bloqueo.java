package pucp.edu.pe.glp_final.models;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Pattern;

import jakarta.persistence.*;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Component
@Table(name = "bloqueo")
@Entity
public class Bloqueo {
    private static int contadorId = 1; // Contador estático para IDs
    @Id
    private int id;
    private int anio;
    private int mes;
    private int diaInicio;
    private int horaInicio;
    private int minutoInicio;
    private int diaFin;
    private int horaFin;
    private int minutoFin;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;

    @Transient
    private List<NodoBloqueado> tramo;

    @ManyToMany
    @JoinTable(
            name = "bloqueo_nodo",
            joinColumns = @JoinColumn(name = "bloqueo_id"),
            inverseJoinColumns = @JoinColumn(name = "nodo_id")
    )
    public static Bloqueo leerBloqueo(String registro, int anio, int mes) {
        Bloqueo bloqueo = new Bloqueo();

        List<NodoBloqueado> tramoBloqueos = new ArrayList<>();
        bloqueo.anio = anio;
        bloqueo.mes = mes;
        bloqueo.id = contadorId++;

        // Dividir el registro en dos partes usando ":" como separador
        String[] partes = registro.split(":");
        if (partes.length != 2) {
            throw new IllegalArgumentException("Formato de entrada incorrecto");
        }

        // Dividir la primera parte en "inicio" y "fin" usando "-"
        String[] momentoInicioFin = partes[0].split("-");
        if (momentoInicioFin.length != 2) {
            throw new IllegalArgumentException("Formato de inicio/fin incorrecto");
        }

        // Atributos de inicio
        String[] partesInicio = momentoInicioFin[0].split("[dhm]");
        if (partesInicio.length != 3) {
            throw new IllegalArgumentException("Formato de momento incorrecto");
        }

        bloqueo.setDiaInicio(Integer.parseInt(partesInicio[0].replace("d", "")));
        bloqueo.setHoraInicio(Integer.parseInt(partesInicio[1].replace("h", "")));
        bloqueo.setMinutoInicio(Integer.parseInt(partesInicio[2].replace("m", "")));

        // Atributos de fin
        String[] partesFin = momentoInicioFin[1].split("[dhm]");
        if (partesFin.length != 3) {
            throw new IllegalArgumentException("Formato de momento incorrecto");
        }

        bloqueo.setDiaFin(Integer.parseInt(partesFin[0].replace("d", "")));
        bloqueo.setHoraFin(Integer.parseInt(partesFin[1].replace("h", "")));
        bloqueo.setMinutoFin(Integer.parseInt(partesFin[2].replace("m", "")));

        // Dividir la segunda parte en coordenadas usando ","
        String[] coordenadas = partes[1].split(",");
        if (coordenadas.length % 2 != 0) {
            throw new IllegalArgumentException("Formato de coordenadas incorrecto");
        }

        LocalDateTime fechaInicio = LocalDateTime.of(anio,mes,bloqueo.getDiaInicio(),bloqueo.getHoraInicio(),bloqueo.getMinutoInicio());
        LocalDateTime fechaFin = LocalDateTime.of(anio,mes,bloqueo.getDiaFin(),bloqueo.getHoraFin(),bloqueo.getMinutoFin());
        bloqueo.setFechaInicio(fechaInicio);
        bloqueo.setFechaFin(fechaFin);

        for (int i = 0; i < coordenadas.length - 2; i += 2) {
            NodoBloqueado nodo = new NodoBloqueado();

            nodo.setX_ini(Integer.parseInt(coordenadas[i]));
            nodo.setY_ini(Integer.parseInt(coordenadas[i + 1]));

            nodo.setX_fin(Integer.parseInt(coordenadas[i + 2]));
            nodo.setY_fin(Integer.parseInt(coordenadas[i + 3]));

            tramoBloqueos.add(nodo);
        }

        bloqueo.setTramo(tramoBloqueos);

        return bloqueo;
    }

    public List<Bloqueo> leerArchivoBloqueo(String nombreArchivo) {
        // Reiniciar el contador de IDs al leer un nuevo archivo
        contadorId = 1;

        System.out.println("=== DEBUG LECTURA DE BLOQUEOS ===");
        System.out.println("Nombre del archivo recibido: " + nombreArchivo);

        // Actualizar la ruta para que coincida con FileStorageService
        String path = "src/main/java/com/plg/backend/data/bloqueos/" + nombreArchivo;
        System.out.println("Ruta completa del archivo: " + path);

        List<Bloqueo> bloqueos = new ArrayList<>();

        try {
            // Verificar si el archivo existe
            File file = new File(path);
            if (!file.exists()) {
                System.out.println("ERROR: El archivo no existe en la ruta especificada");
                System.out.println("Ruta absoluta intentada: " + file.getAbsolutePath());
                return bloqueos;
            }
            System.out.println("El archivo existe: " + file.getAbsolutePath());

            // Intentar extraer año y mes
            try {
                int anio = Integer.parseInt(nombreArchivo.substring(0, 4));
                int mes = Integer.parseInt(nombreArchivo.substring(4, 6));
                System.out.println("Año extraído: " + anio);
                System.out.println("Mes extraído: " + mes);
            } catch (Exception e) {
                System.out.println("ERROR: No se pudo extraer año y mes del nombre del archivo");
                System.out.println("Error específico: " + e.getMessage());
                return bloqueos;
            }

            // Leer el contenido del archivo
            try (BufferedReader br = new BufferedReader(new FileReader(path))) {
                String linea;
                int numeroLinea = 0;
                while ((linea = br.readLine()) != null) {
                    numeroLinea++;
                    System.out.println("Leyendo línea " + numeroLinea + ": " + linea);

                    try {
                        Bloqueo bloqueo = leerBloqueo(linea,
                                Integer.parseInt(nombreArchivo.substring(0, 4)),
                                Integer.parseInt(nombreArchivo.substring(4, 6)));
                        bloqueos.add(bloqueo);
                        System.out.println("Bloqueo procesado correctamente");
                    } catch (Exception e) {
                        System.out.println("ERROR procesando línea " + numeroLinea + ": " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("ERROR al leer el archivo: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("Total de bloqueos procesados: " + bloqueos.size());
        System.out.println("=== FIN DEBUG LECTURA DE BLOQUEOS ===");
        return bloqueos;
    }

    // Si una calle o nodo esta bloqueado en cierto momento, devolver true o false para replanificar
    public boolean estaActivo(Calendar ahora) {

        // Obtiene la fecha y hora actual
        int anioActual = ahora.get(Calendar.YEAR);
        int mesActual = ahora.get(Calendar.MONTH) + 1; // Suma 1 porque en Calendar, enero es 0
        int diaActual = ahora.get(Calendar.DAY_OF_MONTH);
        int horaActual = ahora.get(Calendar.HOUR_OF_DAY);
        int minutoActual = ahora.get(Calendar.MINUTE);

        // Compara con las fechas y horas de inicio y fin del bloqueo
        if (anioActual > anio || (anioActual == anio && mesActual > mes)) {
            return false; // Bloqueo en el pasado
        }
        if (anioActual < anio || mesActual < mes || diaActual < diaInicio) {
            return false; // Bloqueo en el futuro
        }
        if (diaActual == diaInicio && horaActual < horaInicio) {
            return false; // Bloqueo en el futuro (mismo día)
        }
        if (diaActual == diaInicio && horaActual == horaInicio && minutoActual < minutoInicio) {
            return false; // Bloqueo en el futuro (mismo día y misma hora)
        }
        if (anioActual == anio && mesActual == mes && diaActual == diaInicio &&
                horaActual == horaInicio && minutoActual >= minutoInicio &&
                (diaActual > diaFin || horaActual > horaFin || (horaActual == horaFin && minutoActual >= minutoFin))) {
            return false; // Bloqueo en el pasado
        }

        return true; // Bloqueo activo
    }

    // Devuelve los bloqueos activos en cierto momento
    public static List<Bloqueo> bloqueosActivos(List<Bloqueo> bloqueos, Calendar ahora) {

        List<Bloqueo> bloqueosActivos = new ArrayList<>();

        for (Bloqueo bloqueo : bloqueos) {
            if (bloqueo.estaActivo(ahora)) {
                bloqueosActivos.add(bloqueo);
            }
        }
        return bloqueosActivos;
    }

    /**
     * Devuelve la lista de nodos de la grilla que están bloqueados según los bloqueos activos.
     * @param bloqueos Lista de bloqueos a considerar.
     * @return Lista de nodos bloqueados.
     */
    public static List<Nodo> NodosBloqueados(List<Bloqueo>bloqueos){
        List<Nodo> nodosBloqueados =new ArrayList<>();

        for(Bloqueo bloqueo: bloqueos){
            for(NodoBloqueado tramoBloqueo: bloqueo.getTramo()){
                //bloqueo horizontal
                if(tramoBloqueo.getY_ini()==tramoBloqueo.getY_fin()){
                    // bloqueo derecho
                    if(tramoBloqueo.getX_ini()<tramoBloqueo.getX_fin()){
                        for(int i =tramoBloqueo.getX_ini();i<=tramoBloqueo.getX_fin();i++){
                            Nodo nodo=new Nodo(i,tramoBloqueo.getY_ini());
                            nodosBloqueados.add(nodo);

                        }
                    }else{
                        // bloqueo lado izquierdo
                        for(int i =tramoBloqueo.getX_fin();i<=tramoBloqueo.getX_ini();i++){
                            Nodo nodo=new Nodo(i,tramoBloqueo.getY_ini());
                            nodosBloqueados.add(nodo);
                        }
                    }
                }else{
                    //bloqueo vertical arriba
                    if(tramoBloqueo.getY_ini()<tramoBloqueo.getY_fin()){
                        for(int i =tramoBloqueo.getY_ini();i<=tramoBloqueo.getY_fin();i++){
                            Nodo nodo=new Nodo(tramoBloqueo.getX_ini(),i);
                            nodosBloqueados.add(nodo);
                        }
                    }else{
                        // bloqueo abajo
                        for(int i =tramoBloqueo.getY_fin();i<=tramoBloqueo.getY_ini();i++){
                            Nodo nodo=new Nodo(tramoBloqueo.getX_ini(),i);
                            nodosBloqueados.add(nodo);
                        }
                    }
                }

            }

        }

        return nodosBloqueados;
    }

    /**
     * Devuelve los nombres de los archivos .txt en la carpeta 'data/bloqueos'
     * que cumplen con el patrón aaaamm.bloqueos.txt (ejemplo: 202501.bloqueos.txt).
     * @return Lista de nombres de archivos válidos.
     */
    public List<String> ObtenerNombresDeArchivosDeBloqueos() {
        String resourcesDirectoryPath = "src/main/java/com/plg/backend/data/bloqueos";
        System.out.println("=== DEBUG OBTENER NOMBRES DE ARCHIVOS ===");
        System.out.println("Buscando archivos en: " + resourcesDirectoryPath);

        List<String> nombresArchivos = new ArrayList<>();
        File directorio = new File(resourcesDirectoryPath);

        if (directorio.exists() && directorio.isDirectory()) {
            System.out.println("Directorio encontrado");
            File[] files = directorio.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        String nombreArchivo = file.getName();
                        System.out.println("Archivo encontrado: " + nombreArchivo);
                        if (Pattern.matches("\\d{6}\\.bloqueos\\.txt", nombreArchivo)) {
                            System.out.println("Archivo válido encontrado: " + nombreArchivo);
                            nombresArchivos.add(nombreArchivo);
                        }
                    }
                }
            }
        } else {
            System.err.println("El directorio no existe o no es una carpeta: " + directorio.getAbsolutePath());
        }

        System.out.println("Total de archivos encontrados: " + nombresArchivos.size());
        System.out.println("=== FIN DEBUG OBTENER NOMBRES DE ARCHIVOS ===");
        return nombresArchivos;
    }

}
