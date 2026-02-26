import java.math.*;

/// Proof: manual-transaction-vs-declarative
/// Source: content/enterprise/manual-transaction-vs-declarative.yaml
@interface ApplicationScoped {}
@interface PersistenceContext {}
@interface Transactional {}

record Account(Long id, BigDecimal balance) {
    Account debit(BigDecimal amount) { return this; }
    Account credit(BigDecimal amount) { return this; }
}

interface EntityManager {
    <T> T find(Class<T> cls, Object id);
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
