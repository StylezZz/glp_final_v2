package pucp.edu.pe.glp_final.repository;

import org.springframework.web.multipart.MultipartFile;

public interface FileRepository {
    public void saveFile(MultipartFile file);
}
