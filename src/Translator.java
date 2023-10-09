import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;


public class Translator {
    static Path vvmPath;
    static Path langPath = Path.of("./lang");
    static Path backupPath = Path.of("./backup");
    static Path translatedPath = langPath.resolve("translated");
    static Path vvmAppDataPath;
    static String version;
    private static final String VISUALVM_DEFAULT_USERDIR = "visualvm_default_userdir";

    public static void main(String[] args) throws IOException, InterruptedException {
        setVvmPath(Path.of("D:\\Program Files\\visualvm\\visualvm_207"));//设置路径
        readAllLanguages();
        //createBackup();//创建备份
        //useAllLanguages(vvmPath);//汉化本体
        //useAllLanguages(vvmAppDataPath);//汉化插件
    }


    public static void GetAllExistingLanguagesOfVVM(Path vvmPath) throws IOException{
        setVvmPath(vvmPath);
        readAllLanguages();
        //findRemainingLang();
    }


    /**
     * 设置vvm安装目录的路径 如 "D:\Program Files\visualvm\visualvm_217"
     *
     * @param path 路径
     */
    public static void setVvmPath(Path path) throws IOException {
        vvmPath = path;
        try (InputStream inputStream = Files.newInputStream(vvmPath.resolve("etc/visualvm.conf"))) {
            Properties conf = new Properties();
            conf.load(inputStream);//通过读取配置文件来
            String userDir = conf.getProperty(VISUALVM_DEFAULT_USERDIR);
            Path appDataDir = Paths.get(System.getProperty("user.home"), "AppData", "Roaming", "VisualVM");
            version = userDir.replace("${DEFAULT_USERDIR_ROOT}", "").replaceAll("[\"\\\\/]", "");
            vvmAppDataPath = appDataDir.resolve(version);
        }
    }

    /**
     * 识别尚未本地化的任何剩余语言
     */
    public static void findRemainingLang(String lang) throws IOException {
        Properties allLang = new Properties();
        allLang.load(Files.newBufferedReader(langPath.resolve("allLang.properties")));
        Properties allLangZH = new Properties();
        allLangZH.load(Files.newBufferedReader(translatedPath.resolve(lang + ".properties")));
        for (Map.Entry<Object, Object> map : allLangZH.entrySet()) {
            allLang.remove(map.getKey());
        }
        allLang.store(Files.newBufferedWriter(langPath.resolve("remainingLang.properties")), "");
    }

    /**
     * 指定的语言属性应用于一个JAR文件
     */
    private static void useLanguage(Path jarPath, Properties langProperties) throws IOException {
        Path sourceJarFile = jarPath.getParent().resolve("soc_" + jarPath.getFileName());
        Files.move(jarPath, sourceJarFile);

        try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarPath));
             JarFile srcJarFile = new JarFile(sourceJarFile.toFile())) {
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
        }
        Files.delete(sourceJarFile);
    }


    /**
     * 获取所有翻译过的语言文件
     */
    public static List<String> getAllLanguageFiles() throws IOException {
        ArrayList<String> langFiles = new ArrayList<String>();
        try (Stream<Path> files = Files.list(translatedPath)) {
            files.forEach(path -> {
                String fileName = path.getFileName().toString();
                fileName = fileName.substring(0, fileName.indexOf("."));
                langFiles.add(fileName);
            });
        }
        return langFiles;
    }

    /**
     * 将中文本地化应用于vvm目录中的所有JAR文件
     *
     * @param lang 语言代码 如 "zh_cn"
     */
    public static void useAllLanguages(String lang) throws IOException {
        Properties langProperties = new Properties();
        langProperties.load(Files.newBufferedReader(translatedPath.resolve(lang + ".properties"), StandardCharsets.UTF_8));

        Files.walkFileTree(vvmPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().contains(".jar"))
                    useLanguage(file, langProperties);
                return FileVisitResult.CONTINUE;
            }
        });

        Files.walkFileTree(vvmAppDataPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().contains(".jar"))
                    useLanguage(file, langProperties);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * 从vvm目录中的所有JAR文件中读取所有可用的语言
     */
    public static void readAllLanguages() throws IOException {
        Properties langProperties = new Properties();
        Files.walkFileTree(vvmPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().contains(".jar"))
                    readLanguages(file, langProperties);
                return FileVisitResult.CONTINUE;
            }
        });
        Files.walkFileTree(vvmAppDataPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().contains(".jar"))
                    readLanguages(file, langProperties);
                return FileVisitResult.CONTINUE;
            }
        });
        langProperties.store(Files.newOutputStream(langPath.resolve("allLang.properties")), "");
    }

    /**
     * 从JAR文件中读取所有可用的语言
     */

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

    /**
     * 创建vvm安装和应用数据的备份
     */
    public static void createBackup() throws IOException {
        Path backupVvmPath = backupPath.resolve(version);
        copyDirectory(vvmPath, backupVvmPath.resolve("software"));
        copyDirectory(vvmAppDataPath, backupVvmPath.resolve("appData"));
    }

    /**
     * 从备份中恢复vvm
     */
    public static void rollbackFromBackup() throws IOException {
        Path backupVvmPath = backupPath.resolve(version);
        copyDirectory(backupVvmPath.resolve("software"), vvmPath);
        copyDirectory(backupVvmPath.resolve("appData"), vvmAppDataPath);
    }

    /**
     * 将文件和目录从源位置复制到目标位置
     */
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