package pucp.edu.pe.glp_final.service;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.io.IOException;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pucp.edu.pe.glp_final.repository.FileRepository;


@Service
public class FileStorageService implements FileRepository {

    private final Path rootPedidos = Paths.get("src/main/java/com/plg/backend/data/pedidos.20250419/pedidos.20250419");
    private final Path rootBloqueos = Paths.get("src/main/java/com/plg/backend/data/bloqueos");
    private final String fileVenta = "ventas";
    private final String fileBloqueo = "bloqueos";

    @Override
    public void saveFile(MultipartFile file) {
        try {
            // Determinar si es un archivo de bloqueos
            Path targetPath;
            if (file.getOriginalFilename().contains("bloqueos")) {
                targetPath = rootBloqueos;
            } else {
                targetPath = rootPedidos;
            }

            // Crear el directorio si no existe
            Files.createDirectories(targetPath);

            System.out.println("=== DEBUG FILE STORAGE ===");
            System.out.println("Guardando archivo: " + file.getOriginalFilename());
            System.out.println("En la ruta: " + targetPath.toAbsolutePath());

            if(doesFileExist(file)){
                System.out.println("El archivo ya existe, reemplazando...");
                Files.delete(targetPath.resolve(file.getOriginalFilename()));
            }

            Files.copy(file.getInputStream(), targetPath.resolve(file.getOriginalFilename()));
            System.out.println("Archivo guardado exitosamente en: " + targetPath.resolve(file.getOriginalFilename()).toAbsolutePath());
            System.out.println("=== FIN DEBUG FILE STORAGE ===");

        } catch (Exception e) {
            System.out.println("ERROR al guardar archivo: " + e.getMessage());
            e.printStackTrace();
            if (e instanceof FileAlreadyExistsException) {
                throw new RuntimeException("Ya existe un archivo con ese nombre.");
            }
            throw new RuntimeException(e.getMessage());
        }
    }


    public boolean doesFileExist(MultipartFile file) {
        Path targetPath = file.getOriginalFilename().contains("bloqueos") ? rootBloqueos : rootPedidos;
        return Files.exists(targetPath.resolve(file.getOriginalFilename()));
    }


    public Path getFileVenta(int anio, int mes){
        String anioString = Integer.toString(anio);
        String mesString = String.format("%02d", mes);
        return this.rootPedidos.resolve(fileVenta + anioString + mesString + ".txt");
    }


    public Path getFileBloqueo(int anio, int mes){
        String mesString = String.format("%02d", mes);
        String anioString = Integer.toString(anio);
        return this.rootBloqueos.resolve(anioString+mesString+"."+fileBloqueo+".txt");
    }


    public List<String> obtenerNombrePedidos(){
        try {
            if (!Files.exists(rootPedidos)) {
                System.out.println("El directorio de pedidos no existe.");
                return List.of();
            }

            return Files.list(rootPedidos)
                    .filter(Files::isRegularFile) // Asegura que sea archivo y no subdirectorio
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(nombre -> nombre.contains("ventas"))
                    .collect(Collectors.toList());

        } catch (IOException e) {
            System.err.println("Error al listar archivos de pedidos con 'ventas': " + e.getMessage());
            return List.of();
        }
    }
    public List<String> obtenerNombreBloqueos(){
        try {
            if (!Files.exists(rootBloqueos)) {
                System.out.println("El directorio de bloqueos no existe.");
                return List.of();
            }

            return Files.list(rootBloqueos)
                    .filter(Files::isRegularFile) // Asegura que sea archivo y no subdirectorio
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(nombre -> nombre.contains("bloqueos"))
                    .collect(Collectors.toList());

        } catch (IOException e) {
            System.err.println("Error al listar archivos de pedidos con 'bloqueos': " + e.getMessage());
            return List.of();
        }
    }







}
