///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
//DEPS jakarta.enterprise:jakarta.enterprise.cdi-api:4.1.0
//DEPS jakarta.persistence:jakarta.persistence-api:3.2.0
//DEPS jakarta.transaction:jakarta.transaction-api:2.0.1

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import java.math.*;

/// Proof: manual-transaction-vs-declarative
/// Source: content/enterprise/manual-transaction-vs-declarative.yaml
class Account {
    Long id; BigDecimal balance;
    Account debit(BigDecimal amount) { return this; }
    Account credit(BigDecimal amount) { return this; }
}

@ApplicationScoped
class AccountService {
    @PersistenceContext
    EntityManager em;

    @Transactional
    public void transferFunds(Long from, Long to,
                              BigDecimal amount) {
        var src = em.find(Account.class, from);
        var dst = em.find(Account.class, to);
        src.debit(amount);
        dst.credit(amount);
    }
}

void main() {}
