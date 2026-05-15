package dev.yanianz.sourbyanticheat.logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ViolationLogger {

    private final Path logDir;
    private final boolean useJson;
    private Path currentFile;
    private String currentDate;

    public ViolationLogger(Path dataFolder, boolean useJson) {
        this.logDir = dataFolder.resolve("logs").resolve("violations");
        this.useJson = useJson;
        try {
            Files.createDirectories(logDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create violation log directory", e);
        }
        rotateLog();
    }

    public void log(ViolationEntry entry) {
        rotateLog();
        String line = useJson ? entry.toJson() : entry.toString();
        try {
            Files.writeString(currentFile, line + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("[SAC] Failed to write violation log: " + e.getMessage());
        }
    }

    private void rotateLog() {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        if (today.equals(currentDate) && currentFile != null) return;
        currentDate = today;
        String ext = useJson ? "json" : "log";
        currentFile = logDir.resolve("violations-" + today + "." + ext);
    }
}
