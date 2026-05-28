import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Journal {
    private final String journalPath;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Journal(String journalPath) {
        this.journalPath = journalPath;
    }

    public void log(String operation, String path, String status) {
        String time = LocalDateTime.now().format(formatter);
        String line = time + " | " + operation + " | " + path + " | " + status;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(journalPath, true))) {
            writer.write(line);
            writer.newLine();
        } catch (IOException e) {
            System.out.println("Falha ao escrever no journal: " + e.getMessage());
        }
    }

    public List<String> readRecentEntries(int maxLines) {
        List<String> entries = new ArrayList<>();
        File file = new File(journalPath);

        if (!file.exists()) {
            return entries;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                entries.add(line);
            }
        } catch (IOException e) {
            System.out.println("Falha ao ler journal: " + e.getMessage());
        }

        if (entries.size() <= maxLines) {
            return entries;
        }

        return entries.subList(entries.size() - maxLines, entries.size());
    }
}

