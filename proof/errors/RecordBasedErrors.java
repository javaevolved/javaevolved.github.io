import java.time.*;

/// Proof: record-based-errors
/// Source: content/errors/record-based-errors.yaml
public record ApiError(
    int code,
    String message,
    Instant timestamp
) {
    public ApiError(int code, String msg) {
        this(code, msg, Instant.now());
    }
}

void main() {
    var err = new ApiError(404, "not found");
}
