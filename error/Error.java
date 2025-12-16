package error;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Error {
    private ArrayList<ErrorTuple> errorTuples;
    private static final Error instance = new Error();

    private Error() {
        errorTuples = new ArrayList<>();
    }

    public static Error getInstance() {
        return instance;
    }

    public List<ErrorTuple> getErrorTuples() {
        return errorTuples;
    }

    public void addErrorTuple(ErrorTuple errorTuple) {
        errorTuples.add(errorTuple);
    }

    public void addError(int line_number, String message) {
        errorTuples.add(new ErrorTuple(line_number, message));
    }

    // ✅ 按行号排序（从小到大）
    public void sortByLineNumber() {
        errorTuples.sort(Comparator.comparingInt(ErrorTuple::getLine_number));
    }

    // ✅ 逐行输出（自动排序后打印）
    public void printAllErrors() {
        sortByLineNumber();
        for (ErrorTuple e : errorTuples) {
            System.err.println(e);
        }
    }

    // ✅ 静态单条输出（不存入列表）
    public static void printError(int line_number, String message) {
        System.err.println(line_number + " " + message);
    }
}
