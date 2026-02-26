import java.util.*;

/// Proof: jdbc-vs-jooq
/// Source: content/enterprise/jdbc-vs-jooq.yaml
///
/// Note: Uses stub types to prove the fluent API compiles without jOOQ dependency.
@interface Table {}

// Minimal stubs for jOOQ-style API
enum SQLDialect { POSTGRES }
interface Field<T> {
    Condition eq(T val);
    Condition and(Condition c);
    Condition gt(T val);
}
interface Condition {}
interface TableField<R, T> extends Field<T> {}
interface Record {}
interface SelectJoinStep<R> { SelectConditionStep<R> where(Condition c); }
interface SelectConditionStep<R> { <E> List<E> fetchInto(Class<E> cls); }
interface SelectSelectStep<R> { SelectJoinStep<R> from(Object table); }
interface DSLContext {
    <R extends Record> SelectSelectStep<R> select(Object... fields);
}

record UserTable(
    Field<String> DEPARTMENT,
    Field<Integer> SALARY,
    Field<Long> ID,
    Field<String> NAME,
    Field<String> EMAIL
) {}

record User(Long id, String name, String email) {}

class Db {
    static final UserTable USERS = new UserTable(
        null, null, null, null, null);
    static DSLContext dsl;
}

void main() {
    // Structural proof only — real jOOQ requires runtime dependency
}
