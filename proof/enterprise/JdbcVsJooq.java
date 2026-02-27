///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
//DEPS org.jooq:jooq:3.20.11

import org.jooq.*;
import org.jooq.impl.DSL;
import javax.sql.DataSource;
import java.util.*;

/// Proof: jdbc-vs-jooq
/// Source: content/enterprise/jdbc-vs-jooq.yaml
class User {
    Long id; String name; String email;
}

// Simulating the generated jOOQ table fields (normally produced by jOOQ codegen)
class USERS {
    static final Field<String> DEPARTMENT = DSL.field(DSL.name("department"), String.class);
    static final Field<Integer> SALARY = DSL.field(DSL.name("salary"), Integer.class);
    static final Field<Long> ID = DSL.field(DSL.name("id"), Long.class);
    static final Field<String> NAME = DSL.field(DSL.name("name"), String.class);
    static final Field<String> EMAIL = DSL.field(DSL.name("email"), String.class);
    static final Table<?> TABLE = DSL.table(DSL.name("users"));
}

List<User> findByDept(DataSource ds, String department, int minSalary) {
    DSLContext dsl = DSL.using(ds, SQLDialect.POSTGRES);

    return dsl
        .select(USERS.ID, USERS.NAME, USERS.EMAIL)
        .from(USERS.TABLE)
        .where(USERS.DEPARTMENT.eq(department)
            .and(USERS.SALARY.gt(minSalary)))
        .fetchInto(User.class);
}

void main() {}
