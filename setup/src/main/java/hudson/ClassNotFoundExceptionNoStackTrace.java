package hudson;

public class ClassNotFoundExceptionNoStackTrace extends ClassNotFoundException {

    public ClassNotFoundExceptionNoStackTrace(String className) {
        super(className);
    }

    public Throwable fillInStackTrace() {
        return this;
    }

}
