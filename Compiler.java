import error.Error;
import frontend.Lexer;
import frontend.Parser;
import backend.CodeGenerator;
import backend.LlvmIRGenerator;
import backend.ir.IrModule;
import opt.llvm.LlvmOptimizer;
import opt.mips.MipsOptimizer;
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

        // 手动开关优化：true 开启优化，false 保持原始输出
        boolean enableOpt = true;

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
            IrModule irModule = llvm.generateModule(parser.getCompUnitNode());
            if (enableOpt) {
                irModule = new LlvmOptimizer().optimize(irModule);
            }
            String ir = irModule.emit();
            Files.writeString(Paths.get("llvm_ir.txt"), ir);
            // LLVM -> MIPS
            backend.LlvmToMipsGenerator llvm2mips = new backend.LlvmToMipsGenerator();
            String mipsOutput = llvm2mips.generateFromModule(irModule);
            if (enableOpt) {
                mipsOutput = new MipsOptimizer().optimize(mipsOutput);
            }
            Files.writeString(Paths.get("mips.txt"), mipsOutput);
        }
    }
}
