import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class SimDirectory implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private Map<String, SimFile> files;
    private Map<String, SimDirectory> directories;
    private LocalDateTime createdAt;

    public SimDirectory(String name) {
        this.name = name;
        this.files = new LinkedHashMap<>();
        this.directories = new LinkedHashMap<>();
        this.createdAt = LocalDateTime.now();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, SimFile> getFiles() {
        return files;
    }

    public Map<String, SimDirectory> getDirectories() {
        return directories;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

