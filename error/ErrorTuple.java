package error;

public class ErrorTuple {
    private int line_number;
    private String message;

    public ErrorTuple(int line_number, String message) {
        this.line_number = line_number;
        this.message = message;
    }

    public int getLine_number() {
        return line_number;
    }

    public String getMessage() {
        return message;
    }

    public void setLine_number(int line_number) {
        this.line_number = line_number;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return line_number + " " + message;
    }
}
