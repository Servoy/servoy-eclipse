# Spec: SVY-21204 â Server blockage caused by RabbitMQ error

## 1. Goal

Prevent a RabbitMQ connection failure from cascading into a full server blockage. When the RabbitMQ connection drops (network issue, broker restart, etc.), the server should handle the error gracefully without blocking HTTP request threads, client executor threads, or other critical paths.

## 2. Background

### 2.1 Observed behaviour

A production pod (IPS, version 2025.3.2.4044 LTS) experienced memory spikes to 8 GB (max heap) and thread blockage. Some pods crashed outright. Thread dumps taken during the incident reveal the following cascade:

1. A RabbitMQ I/O error occurs on the AMQP connection to `10.245.134.111:5671`.
2. The RabbitMQ client library's `AutorecoveringConnection` error handler spawns "RabbitMQ Error On Write Thread" instances to shut down channels via `ChannelManager.handleSignal` â `ChannelManager.scheduleShutdownProcessing`.
3. During channel shutdown, the thread loads a class via `WebappClassLoaderBase`. The Datadog ByteBuddy agent intercepts class loading, triggering bytecode transformation that reads from a JAR file.
4. The `JarFile.checkForSpecialAttributes` call acquires a lock on the `JarFile` instance. The `readAllBytes` I/O operation holds this lock while reading.
5. Multiple "RabbitMQ Error On Write Thread" instances block on the same `JarFile` lock, creating a deadlock-like chain among themselves.
6. Client executor threads (e.g. `Executor,uuid:4968645:2`, `Executor,uuid:8916D8F:2`) that need to load ICU resources (`com.ibm.icu` via `WebappClassLoaderBase`) also block on the same `JarFile` lock.
7. HTTP request threads (`http-nio-8080-exec-*`) block on the `WebappClassLoaderBase` class-loading lock held by another HTTP thread doing Log4j `ThrowableProxy` class resolution.
8. The result: 28 threads BLOCKED simultaneously, the server becomes unresponsive.

### 2.2 The DataNotifyBroadCaster plugin

The `com.servoy.extensions.plugins.broadcaster.DataNotifyBroadCaster` is a Servoy server plugin that uses RabbitMQ to distribute data-change notifications across cluster nodes. Its consumer (`DataNotifyBroadCaster$2.handleDelivery`) deserializes Java objects from RabbitMQ messages. When the AMQP connection fails, the RabbitMQ client's internal error-handling threads trigger class loading that cascades into the blockage.

### 2.3 Contributing factors

- **Datadog ByteBuddy agent**: The Java agent instruments classes at load time. During RabbitMQ shutdown, it tries to resolve class hierarchies via `WebappClassLoaderBase.findResource`, which accesses JAR files.
- **JarFile intrinsic lock**: `JarFile.checkForSpecialAttributes` uses an internal `synchronized` block. When the I/O is slow (network-attached storage, or filesystem contention under memory pressure), this lock is held for extended periods.
- **WebappClassLoaderBase lock**: Tomcat's classloader uses `synchronized(this)` during `loadClass`, causing all class-loading operations to serialize and pile up behind the blocked thread.
- **Log4j ThrowableProxy**: When an exception is logged, Log4j resolves every frame's class via `Class.forName` â classloader. Under contention, this becomes a secondary blockage vector.

### 2.4 Server version context

- Servoy version: 2025.3.2.4044 LTS
- Java: 17.0.9
- RabbitMQ client: uses `AutorecoveringConnection` (version embedded in broadcaster plugin)
- Tomcat: embedded (WAR deployment)
- Datadog Java agent: active (bytecode instrumentation)

## 3. Design

### 3.1 Configure RabbitMQ connection factory for resilience

Configure the `ConnectionFactory` with a bounded shared executor and appropriate timeouts to prevent thread explosion during error handling:

```java
ConnectionFactory factory = new ConnectionFactory();
factory.setAutomaticRecoveryEnabled(true);
factory.setNetworkRecoveryInterval(5000); // 5s between recovery attempts
factory.setRequestedHeartbeat(30);        // detect dead connections faster
factory.setConnectionTimeout(10000);      // 10s connection timeout
factory.setHandshakeTimeout(10000);
// Use a bounded executor for consumer work to prevent thread explosion
factory.setSharedExecutor(Executors.newFixedThreadPool(4));
```

### 3.2 Wrap broadcaster RabbitMQ operations with connection-state guard

Before any publish or consume operation, check `connection.isOpen()`. During shutdown, avoid operations that would trigger further class loading or lock acquisition. Do not log exceptions with full stack traces during known-shutdown scenarios, to avoid ThrowableProxy class-loading contention.

### 3.3 Limit concurrent shutdown threads

The `ChannelManager.scheduleShutdownProcessing` creates multiple threads that can pile up. The broadcaster plugin should:
- Use `setTopologyRecoveryExecutor` with a bounded thread pool to limit the number of concurrent recovery/shutdown threads
- Or wrap `ShutdownListener` callbacks to execute on a dedicated single-thread executor
- Configure `setRequestedChannelMax` to limit the number of channels

## 4. Implementation plan

1. **DataNotifyBroadCaster plugin** â Configure ConnectionFactory with bounded executor:
   - Set `factory.setSharedExecutor(Executors.newFixedThreadPool(N))` to bound the thread pool used for consumer dispatch and error handling
   - Set appropriate timeouts (heartbeat, connection, handshake)

2. **DataNotifyBroadCaster plugin** â Add connection-state guard:
   - Before calling `channel.basicPublish(...)`, check `connection.isOpen()` and `channel.isOpen()`
   - If not open, skip the operation and log at WARN level (do not log exceptions with full stack traces during known-shutdown scenarios, to avoid ThrowableProxy class-loading contention)

3. **DataNotifyBroadCaster plugin** â Limit concurrent shutdown threads:
   - Use `factory.setTopologyRecoveryExecutor(boundedExecutor)` if supported by the RabbitMQ client version
   - Or wrap `ShutdownListener` callbacks to execute on a dedicated single-thread executor

## 5. Acceptance criteria

- [ ] A RabbitMQ connection failure does not cause HTTP request threads to become BLOCKED
- [ ] A RabbitMQ connection failure does not cause client executor threads to become BLOCKED on JarFile locks
- [ ] The number of "RabbitMQ Error On Write Thread" instances is bounded (no unbounded thread spawning)
- [ ] The broadcaster plugin recovers its RabbitMQ connection automatically after a transient network failure
- [ ] Server remains responsive to HTTP requests during RabbitMQ outage (degraded functionality is acceptable, full blockage is not)
- [ ] Memory does not spike to max heap as a result of thread pile-up and object accumulation during RabbitMQ error handling

## 6. Out of scope

- Root-cause investigation of why the RabbitMQ network connection fails (infrastructure/network issue on customer side)
- Redesign of the broadcaster plugin's messaging architecture
- Replacement of RabbitMQ with a different messaging system
- Fixing the Datadog ByteBuddy agent's JarFile locking behaviour (third-party)
- Fixing the JDK's `JarFile.checkForSpecialAttributes` synchronization (JDK bug)

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| What version of the RabbitMQ client library is bundled with the broadcaster plugin? Does it support `setTopologyRecoveryExecutor`? | Dev team | open |
| Is the broadcaster plugin source in a separate repository? If so, which one? | Dev team | open |
| Can we reproduce this by simulating RabbitMQ network failure in a test environment with Datadog agent enabled? | QA/DevOps | open |
| Is the customer using a specific RabbitMQ version that has known issues with connection recovery? | Paolo Aronne | open |
| What is the expected behaviour for in-flight data notifications during RabbitMQ outage â drop, queue locally, or retry? | Product | open |
