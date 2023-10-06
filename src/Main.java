import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


public class Main {
    static Path vvmPath = Path.of("D:\\Program Files\\visualvm\\visualvm_217\\visualvm\\modules");
    static Path workPath = Path.of("./work");
    static Path langPath = workPath.resolve("lang");
    static Path backupPath = workPath.resolve("backup");

    static {
        try {
            Files.createDirectories(backupPath);
            Files.createDirectories(langPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException {

        //langProperties.store(Files.newOutputStream(p));
        readAllLanguages();
    }

    private static void readAllLanguages() throws IOException {
        Properties langProperties = new Properties();
        Files.walkFileTree(vvmPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().contains(".jar"))
                    readLanguages(file, langProperties);
                return FileVisitResult.CONTINUE;
            }
        });
        langProperties.store(Files.newOutputStream(langPath.resolve("allLang.properties")), "");
    }


    private static void readLanguages(Path jarPath, Properties langProperties) throws IOException {
        JarFile jarFile = new JarFile(jarPath.toFile());
        Enumeration<JarEntry> entries = jarFile.entries();

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.getName().endsWith("/Bundle.properties")) {
                Properties properties = new Properties();
                properties.load(jarFile.getInputStream(entry));
                for (Map.Entry<Object, Object> map : properties.entrySet()) {
                    if (!map.getKey().toString().contains("OpenIDE-Module"))
                        langProperties.put(map.getKey(), map.getValue());
                }
            }
        }
        jarFile.close();
    }

    private static void createBackup() throws IOException {
        copyDirectory(vvmPath, backupPath);
    }

    public static void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}