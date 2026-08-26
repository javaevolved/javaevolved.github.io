///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.awt.event.*;

/// Proof: anonymous-classes-to-lambdas
/// Source: content/language/anonymous-classes-to-lambdas.yaml
void save(ActionEvent event) {}

void main() {
    Button button = new Button();
    button.addActionListener(this::save);
}

class Button {
    void addActionListener(ActionListener listener) {}
}
