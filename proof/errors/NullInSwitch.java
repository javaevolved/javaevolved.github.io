/// Proof: null-in-switch
/// Source: content/errors/null-in-switch.yaml
enum Status { ACTIVE, PAUSED, STOPPED }

String describe(Status status) {
    return switch (status) {
        case null    -> "unknown";
        case ACTIVE  -> "active";
        case PAUSED  -> "paused";
        default      -> "other";
    };
}

void main() {
    describe(null);
    describe(Status.ACTIVE);
}
