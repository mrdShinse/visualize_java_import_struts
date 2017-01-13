package mrd.shinse.visualize;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.JavaType;
import org.jboss.forge.roaster.model.source.Import;
import org.jboss.forge.roaster.model.source.JavaClassSource;

public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            new Main().execute(args);
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void execute(String[] args) throws IOException {
        Path targetSourceDir = Paths.get("./").resolve("tmp").toAbsolutePath().normalize();
        List<Path> javaSourceFileList = getJavaSourceFileList(targetSourceDir);
        List<ResultDto> results = new ArrayList<>();
        for (Path javaSource : javaSourceFileList) {
            JavaType<?> source = Roaster.parse(javaSource.toFile());
            if (!source.isClass()) {
                continue;
            }
            JavaClassSource classSource = (JavaClassSource) source;
            List<Import> imports = classSource.getImports().stream().filter(i -> {
                Pattern p = Pattern.compile("(Service)|(Action)|(Dao)]$");
                Matcher m = p.matcher(i.toString());
                return m.find();
            }).collect(Collectors.toList());
            results.add(new ResultDto(javaSource, imports));
        }
        System.out.println("size is " + results.size());
        Path resultPath = targetSourceDir.resolve("../result.txt");
        Writer.createNewFile(resultPath);
        Writer.write(resultPath, results);
    }

    private List<Path> getJavaSourceFileList(Path targetSourceDir) throws IOException {
        return Files.walk(targetSourceDir).filter((Path path) -> {
            return !Files.isDirectory(path)
                    && path.getFileName().toString().length() > 4
                    && path.getFileName().toString().substring(path.getFileName().toString().length() - 4, path.getFileName().toString().length()).equals("java");
        }).filter((Path path) -> {
            Pattern p = Pattern.compile("(ServiceImpl.java)|(Action.java)|(Dao.java)$");
            Matcher m = p.matcher(path.toString());
            return m.find();
        }).collect(Collectors.toList());
    }
}

@Data
@AllArgsConstructor
class ResultDto {

    Path file;
    List<Import> imports;
}

class Writer {

    public static void createNewFile(Path resultPath) {
        File resultFile = resultPath.toFile();
        if (!resultFile.exists()) {
            try {
                resultFile.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(Writer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void write(Path path, List<ResultDto> results) throws IOException {
        try (PrintWriter pw = new PrintWriter(path.toFile(), "UTF-8")) {
            results.stream().forEach(actionInfo -> {
                pw.write(actionInfo.toString() + "\r\n");
            });
            pw.flush();
        }
    }
}
