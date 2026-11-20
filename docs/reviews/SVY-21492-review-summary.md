# Review Summary: SVY-21492 Postgres vector columns goes wrong in DBI sync

**Risk: MODERATE.** The Jira log shows type #1111 for the vector column, but the fixed code should produce 10002. If the column is not detected as a vector, the new dimension lookup never runs and the sync difference stays.

Reviewed scope:
- servoy-client `20b68b7c2c` (master)
- server `0756d067` (master)

## Release target
Fix Version 2026.12: MATCH. servoy-client master is 2026.12.0 rc and carries the commit. The server master version was not read from source and is assumed to match.

## Manual test plan

**Verifying the fix**
1. On Postgres with pgvector, create `CREATE TABLE t (id int primary key, embedding vector(1024))` in the server's configured schema.
2. In Developer, add the table to a solution and run DBI sync. Expected: no column difference for `embedding`.
3. Insert a record with a 1024-float vector from a script. Expected: no "column size too small" error.
4. Repeat with a table in a non-default schema, and with the server schema unset.

**Regression checks**
1. A `.dbi` with `length: 2147483647` for the vector column: expect a difference once, gone after updating the `.dbi`.
2. Same table name with different vector dimensions in two schemas, with the server schema set and unset.
3. Unconstrained `vector` column (no dimension): behaviour unchanged.
4. Non-vector Postgres columns (uuid, json, jsonb, text): DBI sync unchanged.
5. SQL Server, MySQL, MariaDB, Oracle and DB2 vector columns: no behaviour change except the client-side size validation skip.

Automated checks worth running (in the `test` repo, project `j2db_test`): `HibernateIT` (~line 3398) and `DB2DialectTest`. Neither was run during the review.

## Follow-ups and open questions
- Confirm which type id the vector column has in the failing case (1111 vs 10002), including build, driver and whether `TYPE_NAME` was exactly `vector`.
- Use `table.getSchema()` for the catalog lookup when it is set, as other column lookups in `Server.askTableForColumns` do. With a null server schema the query takes the first matching row from any schema.
- Existing `.dbi` files holding `length: 2147483647` will show a one-time difference after upgrade. Consider a release note.
- The hard-coded `10002` in `Column.getObjectSize` must stay equal to Hibernate `SqlTypes.VECTOR_FLOAT32`.
- Unconstrained `vector` (`atttypmod` = -1) still reads back as MAX_VALUE. This is unchanged from before.
- Add a test in the `test` repo for a `vector(N)` read-back, including the null-schema case.
