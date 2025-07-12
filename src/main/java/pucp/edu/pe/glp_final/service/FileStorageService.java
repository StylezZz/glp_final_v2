package pucp.edu.pe.glp_final.service;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pucp.edu.pe.glp_final.repository.FileRepository;


@Service
public class FileStorageService implements FileRepository {

    private final Path rootPedidos = Paths.get("src/main/java/com/plg/backend/data/pedidos.20250419/pedidos.20250419");
    private final Path rootBloqueos = Paths.get("src/main/java/com/plg/backend/data/bloqueos");

    @Override
    public void saveFile(MultipartFile file) {
        try {
            Path targetPath;
            if (file.getOriginalFilename().contains("bloqueos")) {
                targetPath = rootBloqueos;
            } else {
                targetPath = rootPedidos;
            }

            Files.createDirectories(targetPath);

            System.out.println("=== DEBUG FILE STORAGE ===");
            System.out.println("Guardando archivo: " + file.getOriginalFilename());
            System.out.println("En la ruta: " + targetPath.toAbsolutePath());

            if (doesFileExist(file)) {
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
}
