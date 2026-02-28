///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
/// Proof: switch-expressions
/// Source: content/language/switch-expressions.yaml
enum Day { MONDAY, TUESDAY, WEDNESDAY,
           THURSDAY, FRIDAY, SATURDAY, SUNDAY }

void main() {
    Day day = Day.MONDAY;
    String msg = switch (day) {
        case MONDAY  -> "Start";
        case FRIDAY  -> "End";
        default      -> "Mid";
    };
}
