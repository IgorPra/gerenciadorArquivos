import java.io.Serializable;
import java.time.LocalDateTime;

public class SimFile implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SimFile(String name, String content) {
        this.name = name;
        this.content = content;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void rename(String newName) {
        this.name = newName;
        this.updatedAt = LocalDateTime.now();
    }

    public SimFile copyWithName(String newName) {
        SimFile copied = new SimFile(newName, this.content);
        copied.createdAt = this.createdAt;
        copied.updatedAt = LocalDateTime.now();
        return copied;
    }
}

