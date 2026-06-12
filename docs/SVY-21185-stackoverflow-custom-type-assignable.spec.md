# Spec: SVY-21185 — StackOverflow when building (CustomType isAssignableFrom cycle)

## 1. Goal

Fix infinite recursion in `CustomTypeRecordType.isAssignableFrom()` that causes a
`StackOverflowError` when building solutions containing self-referential custom
types (e.g., a custom type with a member of type `Array<itself>`).

## 2. Background

### 2.1 CustomTypeRecordType

Commit `25dd1e4024` (SVY-20992, April 17 2026) introduced `CustomTypeRecordType`
— a hybrid class extending `RSimpleType` and implementing `IRRecordType`. It allows
plain object literals (`var x = {}`) to be validly assigned to custom-type variables.

Its `isAssignableFrom(IRType)` override performs structural member-by-member type
compatibility checks when the incoming type is also an `IRRecordType`.

### 2.2 The init separation

Commit `491a5b05` (April 20 2026) separated construction from member initialization
to address a related StackOverflow in `init()` for self-referential types. It uses
a static `ConcurrentHashMap<Type, CustomTypeRecordType> instances` to cache
instances and a `members.isEmpty()` guard in `init()`.

### 2.3 The remaining cycle

Even after the init fix, a StackOverflow persists in `isAssignableFrom` when:
- A custom type has a member whose type is `Array<itself>` (or directly itself)
- Two instances of the same custom type are compared for assignment compatibility
- The check descends into `RArrayType.isAssignableFrom` (line 91), which compares
  item types, calling back into `CustomTypeRecordType.isAssignableFrom` — ad infinitum

Stack trace pattern:
```
CustomTypeRecordType.isAssignableFrom → RArrayType.isAssignableFrom → CustomTypeRecordType.isAssignableFrom → ...
```

### 2.4 Git history

| Commit | Author | Purpose |
|--------|--------|---------|
| `25dd1e4024` | Diana Bunaciu | SVY-20992: introduced CustomTypeRecordType with isAssignableFrom |
| `491a5b05` | Johan Compagner | Partial fix: separated init from construction to break init recursion |

## 3. Design

### 3.1 Recursion guard in isAssignableFrom (THE FIX)

Add a `ThreadLocal<Set<Long>> ASSIGNABLE_GUARD` that tracks `(this, type)` pairs
currently being evaluated on the current thread. If the same pair is encountered
again (cycle detected), short-circuit with `TypeCompatibility.TRUE`.

- **Key computation:** `System.identityHashCode(this) * 31L + System.identityHashCode(type)`
- **Lifecycle:** pair is added on entry, removed in a `finally` block
- **Cycle return value:** `TypeCompatibility.TRUE` (safe default — means "compatible",
  avoids false type errors on valid code)

### 3.2 Hardening init() against first-member self-reference (SUGGESTION — not implemented)

> **Note:** This is a potential future improvement, not part of the current fix.

The `init()` method's `members.isEmpty()` guard fails when the **first** member
(in iteration order) is self-referential — no members have been added yet, so the
recursive `init` call re-enters the loop. This could cause a StackOverflow in
`init()` (separate from the `isAssignableFrom` cycle fixed above).

Suggested fix: add a boolean `initializing` field. Check it alongside `members.isEmpty()`:

```java
if (members.isEmpty() && !initializing)
```

Set it to `true` before iterating, reset in `finally`.

### 3.3 instances.clear() → instances.remove(type) (SUGGESTION — not implemented)

> **Note:** This is a potential future improvement, not part of the current fix.

Replace `instances.clear()` (line 99) with `instances.remove(type)` to avoid
destroying cache entries needed by concurrent or recursive `toRType` calls for
other custom types.

## 4. Implementation plan

1. Add `ASSIGNABLE_GUARD` ThreadLocal to `CustomTypeRecordType.isAssignableFrom` — **DONE**
2. Verify no compilation errors — **DONE**

## 5. Acceptance criteria

- [ ] Building a solution with a self-referential custom type (member of type `Array<CustomType>`) does not throw StackOverflowError
- [ ] Building a solution with a directly self-referential custom type (member of same type) does not throw StackOverflowError
- [ ] Building a solution with mutually-referential custom types (A→B, B→A) does not throw StackOverflowError
- [ ] Type checking still reports genuine assignment errors (e.g., assigning String to Number)
- [ ] No regressions in existing DLTK type validation tests

## 6. Out of scope

- Refactoring the broader DLTK type system to use a proper graph-based cycle detection
- Performance optimization of type checking (the ThreadLocal guard is adequate)
- Fixing the `RTypeDeclaration.isAssignableFrom` itself (it already has its own `visited` set)

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should the `instances` map be removed entirely in favour of always creating fresh instances? | architect | open |
| Is `TypeCompatibility.TRUE` the correct return on cycle detection, or should it be `UNPARAMETERIZED`? | architect | open |
