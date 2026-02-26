/// Proof: private-interface-methods
/// Source: content/language/private-interface-methods.yaml
interface Logger {
    private String format(String lvl, String msg) {
        return "[" + lvl + "] " + timestamp() + msg;
    }
    default void logInfo(String msg) {
        System.out.println(format("INFO", msg));
    }
    default void logWarn(String msg) {
        System.out.println(format("WARN", msg));
    }
    String timestamp();
}

void main() {
    Logger l = () -> "now";
    l.logInfo("started");
    l.logWarn("careful");
}
