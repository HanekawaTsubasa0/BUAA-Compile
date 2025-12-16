import error.Error;
import frontend.Lexer;
import frontend.Parser;
import backend.CodeGenerator;
import backend.LlvmIRGenerator;
import semantic.SemanticAnalyzer;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

public class Compiler {
    public static void main(String[] args) throws IOException {
        // === 重定向输入/输出/错误流 ===
        System.setIn(new FileInputStream("testfile.txt"));
        System.setOut(new PrintStream(new FileOutputStream("parser.txt")));
        System.setErr(new PrintStream(new FileOutputStream("error.txt")));

        Error error = Error.getInstance();

        // === 一次性读入整个文件 ===
        String input = Files.readString(Paths.get("testfile.txt")); // 保留换行符

        // 初始化 Lexer
        Lexer lexer = new Lexer(input);

        //lexer.output();
        Parser parser = new Parser(lexer.getTokens());

        parser.print();

        SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
        semanticAnalyzer.analyze(parser.getCompUnitNode());

        error.printAllErrors();

        if (error.getErrorTuples().isEmpty()) {
            // 原 AST->MIPS 暂停，使用 LLVM 再翻译 MIPS 供调试
            Files.write(Paths.get("symbol.txt"), semanticAnalyzer.dumpSymbols());
            LlvmIRGenerator llvm = new LlvmIRGenerator();
            String ir = llvm.generate(parser.getCompUnitNode());
            Files.writeString(Paths.get("llvm_ir.txt"), ir);
            // LLVM -> MIPS
            backend.LlvmToMipsGenerator llvm2mips = new backend.LlvmToMipsGenerator();
            String mipsFromIr = llvm2mips.generateFromFile("llvm_ir.txt");
            Files.writeString(Paths.get("mips.txt"), mipsFromIr);
        }
    }
}
