import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;


public class Main {
    static Path vvmPath;
    static Path langPath = Path.of("./lang");
    static Path backupPath = Path.of("./backup");
    static Path vvmAppDataPath;
    static String version;
    private static final String VISUALVM_DEFAULT_USERDIR = "visualvm_default_userdir";

    public static void main(String[] args) throws IOException {
        setVvmPath(Path.of("D:\\Program Files\\visualvm\\visualvm_217"));


        //createBackup();
        //rollbackFromBackup();
        //langProperties.store(Files.newOutputStream(p));
        //readAllLanguages(vvmPath);
        //readAllLanguages(vvmAppDataPath);
        useAllLanguages(vvmPath);
        useAllLanguages(vvmAppDataPath);
        //useAllLanguages(vvmPath);
        //readAllLanguages(pluginPath);
        //findRemainingLang();
    }

    public static void setVvmPath(Path path) throws IOException {
        vvmPath = path;
        try (InputStream inputStream = Files.newInputStream(vvmPath.resolve("etc/visualvm.conf"))) {
            Properties conf = new Properties();
            conf.load(inputStream);
            String userDir = conf.getProperty(VISUALVM_DEFAULT_USERDIR);
            Path appDataDir = Paths.get(System.getProperty("user.home"), "AppData", "Roaming", "VisualVM");
            version = userDir.replace("${DEFAULT_USERDIR_ROOT}", "").replaceAll("[\"\\\\/]", "");
            vvmAppDataPath = appDataDir.resolve(version);
        }
    }


    public static void findRemainingLang() throws IOException {
        Properties allLang = new Properties();
        allLang.load(Files.newBufferedReader(langPath.resolve("allLang.properties")));
        Properties allLangZH = new Properties();
        allLangZH.load(Files.newBufferedReader(langPath.resolve("allLangZH.properties")));
        for (Map.Entry<Object, Object> map : allLangZH.entrySet()) {
            allLang.remove(map.getKey());
        }
        allLang.store(Files.newBufferedWriter(langPath.resolve("remainingLang.properties")), "");
    }

    private static void useLanguage(Path jarPath, Properties langProperties) throws IOException {

        Path sourceJarFile = jarPath.getParent().resolve("soc_" + jarPath.getFileName());
        Files.move(jarPath, sourceJarFile);

        JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarPath));
        JarFile srcJarFile = new JarFile(sourceJarFile.toFile());
        Enumeration<JarEntry> entries = srcJarFile.entries();

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();

            if (entry.getName().endsWith("/Bundle.properties")) {

                Properties properties = new Properties();
                properties.load(srcJarFile.getInputStream(entry));
                for (Map.Entry<Object, Object> map : properties.entrySet()) {
                    Object langValue = langProperties.get(map.getKey());
                    if (langValue != null)
                        properties.put(map.getKey(), langValue);
                }
                JarEntry newEntry = new JarEntry(entry.getName());
                jarOutputStream.putNextEntry(newEntry);
                properties.store(jarOutputStream, "");
            } else {
                jarOutputStream.putNextEntry(entry);
                jarOutputStream.write(srcJarFile.getInputStream(entry).readAllBytes());
            }
            jarOutputStream.closeEntry();
        }
        jarOutputStream.close();
        srcJarFile.close();
        Files.delete(sourceJarFile);
    }

    public static void useAllLanguages(Path path) throws IOException {
        Properties langProperties = new Properties();
        langProperties.load(Files.newBufferedReader(langPath.resolve("allLangZH.properties"), StandardCharsets.UTF_8));

        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().contains(".jar"))
                    useLanguage(file, langProperties);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void readAllLanguages(Path path) throws IOException {
        Properties langProperties = new Properties();
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
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

    public static void createBackup() throws IOException {
        Path backupVvmPath = backupPath.resolve(version);
        copyDirectory(vvmPath, backupVvmPath.resolve("software"));
        copyDirectory(vvmAppDataPath, backupVvmPath.resolve("appData"));
    }

    public static void rollbackFromBackup() throws IOException {
        Path backupVvmPath = backupPath.resolve(version);
        copyDirectory(backupVvmPath.resolve("software"), vvmPath);
        copyDirectory(backupVvmPath.resolve("appData"), vvmAppDataPath);
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
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