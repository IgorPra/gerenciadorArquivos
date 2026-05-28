import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class FileSystemSimulator {
    private static final String DATA_FILE = "filesystem.dat";
    private static final String JOURNAL_FILE = "journal.log";

    private SimDirectory root;
    private final Journal journal;

    public FileSystemSimulator() {
        this.journal = new Journal(JOURNAL_FILE);
        loadFileSystem();
        showJournalPreview();
    }

    public static void main(String[] args) {
        FileSystemSimulator simulator = new FileSystemSimulator();
        simulator.runShell();
    }

    private void loadFileSystem() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(DATA_FILE))) {
            root = (SimDirectory) in.readObject();
            System.out.println("Sistema de arquivos carregado com sucesso.");
        } catch (IOException | ClassNotFoundException e) {
            root = new SimDirectory("/");
            System.out.println("Nenhum sistema salvo encontrado. Novo sistema iniciado.");
            saveFileSystem();
        }
    }

    private void saveFileSystem() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            out.writeObject(root);
        } catch (IOException e) {
            System.out.println("Erro ao salvar sistema de arquivos: " + e.getMessage());
        }
    }

    private void showJournalPreview() {
        List<String> entries = journal.readRecentEntries(5);
        if (entries.isEmpty()) {
            System.out.println("Journal vazio no momento.");
            return;
        }

        System.out.println("Ultimas operacoes do journal:");
        for (String entry : entries) {
            System.out.println(entry);
        }
    }

    private void runShell() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\nSimulador de Sistema de Arquivos");
        System.out.println("Digite um comando ou 'exit' para sair.");

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            if ("exit".equalsIgnoreCase(input)) {
                System.out.println("Encerrando simulador.");
                break;
            }

            executeCommand(input);
        }

        scanner.close();
    }

    private void executeCommand(String input) {
        try {
            if (input.startsWith("touch ")) {
                handleTouchCommand(input);
                return;
            }

            String[] parts = input.split("\\s+");
            String command = parts[0].toLowerCase();

            switch (command) {
                case "mkdir":
                    validateLength(parts, 2);
                    createDirectory(parts[1]);
                    break;
                case "rmdir":
                    validateLength(parts, 2);
                    deleteDirectory(parts[1]);
                    break;
                case "rename":
                    validateLength(parts, 3);
                    renamePath(parts[1], parts[2]);
                    break;
                case "cp":
                    validateLength(parts, 3);
                    copyFile(parts[1], parts[2]);
                    break;
                case "rm":
                    validateLength(parts, 2);
                    deleteFile(parts[1]);
                    break;
                case "ls":
                    validateLength(parts, 2);
                    listDirectory(parts[1]);
                    break;
                case "help":
                    printHelp();
                    break;
                default:
                    System.out.println("Comando invalido. Digite 'help' para ver os comandos disponiveis.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }

    private void handleTouchCommand(String input) {
        int firstQuote = input.indexOf('"');
        int lastQuote = input.lastIndexOf('"');

        if (firstQuote == -1 || lastQuote == firstQuote) {
            throw new IllegalArgumentException("Uso: touch /caminho/arquivo.txt \"conteudo\"");
        }

        String beforeContent = input.substring(0, firstQuote).trim();
        String content = input.substring(firstQuote + 1, lastQuote);
        String[] tokens = beforeContent.split("\\s+");

        if (tokens.length != 2) {
            throw new IllegalArgumentException("Uso: touch /caminho/arquivo.txt \"conteudo\"");
        }

        createFile(tokens[1], content);
    }

    private void printHelp() {
        System.out.println("Comandos disponiveis:");
        System.out.println("mkdir /diretorio");
        System.out.println("rmdir /diretorio");
        System.out.println("touch /diretorio/arquivo.txt \"conteudo\"");
        System.out.println("cp /origem/arquivo.txt /destino/arquivo.txt");
        System.out.println("rename /caminho/atual novo_nome");
        System.out.println("rm /diretorio/arquivo.txt");
        System.out.println("ls /diretorio");
        System.out.println("exit");
    }

    private void validateLength(String[] parts, int expectedLength) {
        if (parts.length < expectedLength) {
            throw new IllegalArgumentException("Comando incompleto. Digite 'help' para ver o formato correto.");
        }
    }

    public void createDirectory(String path) {
        try {
            PathInfo info = getParentAndName(path);
            if (info.parent.getDirectories().containsKey(info.name)) {
                throw new IllegalArgumentException("Diretorio ja existe.");
            }

            if (info.parent.getFiles().containsKey(info.name)) {
                throw new IllegalArgumentException("Ja existe um arquivo com esse nome.");
            }

            info.parent.getDirectories().put(info.name, new SimDirectory(info.name));
            saveFileSystem();
            journal.log("CREATE_DIRECTORY", path, "SUCCESS");
            System.out.println("Diretorio criado com sucesso.");
        } catch (IllegalArgumentException e) {
            journal.log("CREATE_DIRECTORY", path, "FAIL");
            System.out.println(e.getMessage());
        }
    }

    public void deleteDirectory(String path) {
        try {
            if ("/".equals(path)) {
                throw new IllegalArgumentException("Nao e permitido remover o diretorio raiz.");
            }

            PathInfo info = getParentAndName(path);
            SimDirectory target = info.parent.getDirectories().get(info.name);

            if (target == null) {
                throw new IllegalArgumentException("Diretorio nao encontrado.");
            }

            if (!target.getDirectories().isEmpty() || !target.getFiles().isEmpty()) {
                throw new IllegalArgumentException("Diretorio nao vazio. Remova o conteudo antes.");
            }

            info.parent.getDirectories().remove(info.name);
            saveFileSystem();
            journal.log("DELETE_DIRECTORY", path, "SUCCESS");
            System.out.println("Diretorio removido com sucesso.");
        } catch (IllegalArgumentException e) {
            journal.log("DELETE_DIRECTORY", path, "FAIL");
            System.out.println(e.getMessage());
        }
    }

    public void renameDirectory(String path, String newName) {
        try {
            validateName(newName);
            PathInfo info = getParentAndName(path);
            SimDirectory dir = info.parent.getDirectories().get(info.name);

            if (dir == null) {
                throw new IllegalArgumentException("Diretorio nao encontrado.");
            }

            if (info.parent.getDirectories().containsKey(newName) || info.parent.getFiles().containsKey(newName)) {
                throw new IllegalArgumentException("Ja existe item com o novo nome.");
            }

            info.parent.getDirectories().remove(info.name);
            dir.setName(newName);
            info.parent.getDirectories().put(newName, dir);

            saveFileSystem();
            journal.log("RENAME_DIRECTORY", path + " -> " + newName, "SUCCESS");
            System.out.println("Diretorio renomeado com sucesso.");
        } catch (IllegalArgumentException e) {
            journal.log("RENAME_DIRECTORY", path + " -> " + newName, "FAIL");
            System.out.println(e.getMessage());
        }
    }

    public void createFile(String path, String content) {
        try {
            PathInfo info = getParentAndName(path);

            if (info.parent.getFiles().containsKey(info.name)) {
                throw new IllegalArgumentException("Arquivo ja existe.");
            }

            if (info.parent.getDirectories().containsKey(info.name)) {
                throw new IllegalArgumentException("Ja existe um diretorio com esse nome.");
            }

            info.parent.getFiles().put(info.name, new SimFile(info.name, content));
            saveFileSystem();
            journal.log("CREATE_FILE", path, "SUCCESS");
            System.out.println("Arquivo criado com sucesso.");
        } catch (IllegalArgumentException e) {
            journal.log("CREATE_FILE", path, "FAIL");
            System.out.println(e.getMessage());
        }
    }

    public void copyFile(String sourcePath, String destinationPath) {
        try {
            PathInfo sourceInfo = getParentAndName(sourcePath);
            SimFile sourceFile = sourceInfo.parent.getFiles().get(sourceInfo.name);

            if (sourceFile == null) {
                throw new IllegalArgumentException("Arquivo de origem nao encontrado.");
            }

            PathInfo destInfo = getParentAndName(destinationPath);

            if (destInfo.parent.getFiles().containsKey(destInfo.name) || destInfo.parent.getDirectories().containsKey(destInfo.name)) {
                throw new IllegalArgumentException("Ja existe item no destino com esse nome.");
            }

            SimFile copiedFile = sourceFile.copyWithName(destInfo.name);
            destInfo.parent.getFiles().put(destInfo.name, copiedFile);

            saveFileSystem();
            journal.log("COPY_FILE", sourcePath + " -> " + destinationPath, "SUCCESS");
            System.out.println("Arquivo copiado com sucesso.");
        } catch (IllegalArgumentException e) {
            journal.log("COPY_FILE", sourcePath + " -> " + destinationPath, "FAIL");
            System.out.println(e.getMessage());
        }
    }

    public void deleteFile(String path) {
        try {
            PathInfo info = getParentAndName(path);
            SimFile removed = info.parent.getFiles().remove(info.name);

            if (removed == null) {
                throw new IllegalArgumentException("Arquivo nao encontrado.");
            }

            saveFileSystem();
            journal.log("DELETE_FILE", path, "SUCCESS");
            System.out.println("Arquivo removido com sucesso.");
        } catch (IllegalArgumentException e) {
            journal.log("DELETE_FILE", path, "FAIL");
            System.out.println(e.getMessage());
        }
    }

    public void renameFile(String path, String newName) {
        try {
            validateName(newName);
            PathInfo info = getParentAndName(path);
            SimFile file = info.parent.getFiles().get(info.name);

            if (file == null) {
                throw new IllegalArgumentException("Arquivo nao encontrado.");
            }

            if (info.parent.getFiles().containsKey(newName) || info.parent.getDirectories().containsKey(newName)) {
                throw new IllegalArgumentException("Ja existe item com o novo nome.");
            }

            info.parent.getFiles().remove(info.name);
            file.rename(newName);
            info.parent.getFiles().put(newName, file);

            saveFileSystem();
            journal.log("RENAME_FILE", path + " -> " + newName, "SUCCESS");
            System.out.println("Arquivo renomeado com sucesso.");
        } catch (IllegalArgumentException e) {
            journal.log("RENAME_FILE", path + " -> " + newName, "FAIL");
            System.out.println(e.getMessage());
        }
    }

    public void listDirectory(String path) {
        try {
            SimDirectory dir = getDirectory(path);
            System.out.println("Conteudo de " + path + ":");

            if (dir.getDirectories().isEmpty() && dir.getFiles().isEmpty()) {
                System.out.println("(vazio)");
                return;
            }

            for (Map.Entry<String, SimDirectory> entry : dir.getDirectories().entrySet()) {
                System.out.println("- [DIR] " + entry.getKey());
            }

            for (Map.Entry<String, SimFile> entry : dir.getFiles().entrySet()) {
                System.out.println("- " + entry.getKey());
            }
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }

    private void renamePath(String path, String newName) {
        SimDirectory parent = getParentDirectory(path);
        String currentName = getLastPart(path);

        if (parent.getFiles().containsKey(currentName)) {
            renameFile(path, newName);
            return;
        }

        if (parent.getDirectories().containsKey(currentName)) {
            renameDirectory(path, newName);
            return;
        }

        System.out.println("Arquivo ou diretorio nao encontrado.");
    }

    private SimDirectory getDirectory(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Caminho invalido.");
        }

        if ("/".equals(path)) {
            return root;
        }

        String[] parts = splitPath(path);
        SimDirectory current = root;

        for (String part : parts) {
            SimDirectory next = current.getDirectories().get(part);
            if (next == null) {
                throw new IllegalArgumentException("Diretorio nao encontrado: " + path);
            }
            current = next;
        }

        return current;
    }

    private SimDirectory getParentDirectory(String fullPath) {
        String[] parts = splitPath(fullPath);

        if (parts.length == 0) {
            throw new IllegalArgumentException("Caminho invalido.");
        }

        if (parts.length == 1) {
            return root;
        }

        StringBuilder parentPath = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            parentPath.append('/').append(parts[i]);
        }

        return getDirectory(parentPath.toString());
    }

    private PathInfo getParentAndName(String fullPath) {
        validateAbsolutePath(fullPath);
        SimDirectory parent = getParentDirectory(fullPath);
        String name = getLastPart(fullPath);
        validateName(name);
        return new PathInfo(parent, name);
    }

    private void validateAbsolutePath(String path) {
        if (path == null || path.isBlank() || !path.startsWith("/")) {
            throw new IllegalArgumentException("Caminho invalido. Use caminho absoluto, por exemplo: /documentos/arquivo.txt");
        }

        if ("/".equals(path)) {
            throw new IllegalArgumentException("Operacao invalida para caminho raiz '/'.");
        }
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Nome invalido.");
        }

        if (name.contains("/")) {
            throw new IllegalArgumentException("Nome invalido. Nao use '/'.");
        }
    }

    private String getLastPart(String path) {
        String[] parts = splitPath(path);
        if (parts.length == 0) {
            throw new IllegalArgumentException("Caminho invalido.");
        }
        return parts[parts.length - 1];
    }

    private String[] splitPath(String path) {
        if (path == null) {
            return new String[0];
        }

        String normalized = path.trim();
        if (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        if ("/".equals(normalized)) {
            return new String[0];
        }

        String[] raw = normalized.split("/");
        List<String> parts = new ArrayList<>();
        for (String item : raw) {
            if (!item.isBlank()) {
                parts.add(item);
            }
        }
        return parts.toArray(new String[0]);
    }

    private static class PathInfo {
        private final SimDirectory parent;
        private final String name;

        private PathInfo(SimDirectory parent, String name) {
            this.parent = parent;
            this.name = name;
        }
    }
}

