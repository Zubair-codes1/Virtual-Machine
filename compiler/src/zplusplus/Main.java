package zplusplus;

import zplusplus.code_gen.CodeGenerator;
import zplusplus.exceptions.CompilerException;
import zplusplus.parser.Parser;
import zplusplus.ast.Statement;
import zplusplus.sem_analysis.Analyser;
import zplusplus.sem_analysis.Environment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        if (
                args.length != 2 || !args[0].endsWith(".zpp") || !args[1].endsWith(".asm")
        ) {
            System.err.println("Error Usage: zcc <zpp file path> <asm file path>");
            return;
        }

        try {
            String code = Files.readString(Paths.get(args[1]));

            Parser parser = new Parser();
            List<Statement> statements = parser.parse(code);

            Analyser analyser = new Analyser(statements);
            Environment environment = analyser.analyse();

            CodeGenerator codeGenerator = new CodeGenerator();
            String assembly = codeGenerator.generate(statements, environment);

            Files.write(Paths.get(args[2]), assembly.getBytes());


        } catch (IOException e) {
            throw new CompilerException("Compiler Error: Could not read file", 0);
        } catch (CompilerException e) {
            System.err.println(e.getMessage());
        }
    }
}
