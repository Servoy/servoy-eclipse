package com.servoy.eclipse.cypress.headless;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.app.IApplicationContext;
import org.json.JSONException;
import org.sablo.websocket.GetHttpSessionConfigurator;
import org.sablo.websocket.IWebsocketSession;
import org.sablo.websocket.IWebsocketSessionFactory;
import org.sablo.websocket.WebsocketSessionKey;
import org.sablo.websocket.WebsocketSessionManager;

import com.servoy.base.persistence.IBaseColumn;
import com.servoy.eclipse.exporter.apps.common.AbstractWorkspaceExporter;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.war.exporter.AbstractWarExportModel.License;
import com.servoy.eclipse.ngclient.startup.FormPreviewNGClient;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.DummyValidator;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.server.extensions.ServoyServiceLoader;
import com.servoy.j2db.server.ngclient.NGClientWebsocketSession;
import com.servoy.j2db.server.ngclient.WebsocketSessionFactory;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServerSingleton;
import com.servoy.j2db.server.starter.IWebServerStarter;
import com.servoy.j2db.util.DatabaseUtils;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.xmlxport.ColumnInfoDef;
import com.servoy.j2db.util.xmlxport.TableDef;

/**
 * Headless Eclipse application that runs Cypress form tests in CI without the
 * Servoy Developer IDE UI.
 * <p>
 * Extends {@link AbstractWorkspaceExporter}, which performs the proven Servoy
 * headless bootstrap: disables the UI, loads the settings file, starts the
 * embedded application server (including Tomcat), imports the workspace
 * projects, initializes the Servoy model and activates the requested solution.
 * Instead of exporting a WAR, this runner discovers and executes Cypress form
 * tests, then writes a JUnit XML report.
 * <p>
 * Register in {@code plugin.xml} as an
 * {@code org.eclipse.core.runtime.applications} extension and invoke via:
 *
 * <pre>
 * ./servoy_developer -application com.servoy.eclipse.cypress.cypressFormTestRunner \
 *     -data /workspace -s mySolution -o /path/to/test-results
 * </pre>
 *
 * Exit codes: {@code 0} = all tests passed (or none discovered), {@code 1} =
 * one or more tests failed, {@code 2} = infrastructure/build error.
 */
public class CypressFormTestRunner extends AbstractWorkspaceExporter<CypressFormTestArgumentChest> {
	/**
	 * Exit code when one or more form tests failed (as opposed to an infrastructure
	 * error).
	 */
	private static final Integer EXIT_TESTS_FAILED = Integer.valueOf(1);

	private static final long SERVER_POLL_INTERVAL_MS = 500;
	private static final long SERVER_TIMEOUT_MS = 120_000;

	@Override
	protected CypressFormTestArgumentChest createArgumentChest(IApplicationContext context) {
		return new CypressFormTestArgumentChest(
				(String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS));
	}

	/**
	 * Installs a database-driver-aware classloader for the JDK
	 * {@link java.util.ServiceLoader} lookups Servoy performs during the solution
	 * build, then delegates to the base implementation.
	 * <p>
	 * This runs after {@code AbstractWorkspaceExporter} has started the application
	 * server (so the driver classloader from
	 * {@code IServerManagerInternal.getClassLoader()} exists) but before
	 * {@code buildActiveProjects()} opens live database connections. Servoy
	 * Developer wires the same bridge in
	 * {@code com.servoy.eclipse.core.Activator.startAppServer} via a base
	 * classloader that falls back to the driver classloader; the headless exporter
	 * base ({@code AbstractWorkspaceExporter.initializeApplicationServer}) sets
	 * only the plugin-base classloader, which cannot see
	 * {@code application_server/drivers/*.jar}. Without this bridge the PostgreSQL
	 * connection initializer, loaded via {@link ServoyServiceLoader}, fails with
	 * {@code NoClassDefFoundError: org/postgresql/util/PGBinaryObject}, which
	 * aborts the build and prevents the web server from starting.
	 */
	@Override
	protected void checkAndExportSolutions(CypressFormTestArgumentChest configuration) {
		installDriverAwareServiceClassLoader();
		super.checkAndExportSolutions(configuration);
	}

	/**
	 * Auto-creates missing in-memory test tables before the base class's build/marker
	 * check runs, so that forms bound to those tables don't fail the build with "table
	 * is not accessible" (see {@link #autoCreateInMemoryTestTables()}).
	 */
	@Override
	protected void afterSolutionActivated(CypressFormTestArgumentChest configuration, String solutionName) {
		autoCreateInMemoryTestTables();
	}

	private void installDriverAwareServiceClassLoader() {
		try {
			IApplicationServerSingleton as = ApplicationServerRegistry.get();
			if (as == null) {
				outputError("Cannot install driver-aware classloader: application server not started.");
				return;
			}
			ClassLoader pluginBaseClassLoader = as.getBaseClassLoader();
			ClassLoader driverClassLoader = as.getServerManager().getClassLoader();
			if (driverClassLoader == null) {
				outputExtra("No driver classloader available; leaving default service classloader in place.");
				return;
			}
			// Mirror com.servoy.eclipse.core.Activator.startAppServer: a classloader whose
			// parent is the
			// plugin base classloader and that falls back to the driver classloader (which
			// loads
			// application_server/drivers/*.jar) for classes the parent cannot find (e.g.
			// org.postgresql.*).
			ClassLoader combined = new ClassLoader(pluginBaseClassLoader) {
				@Override
				protected Class<?> findClass(String name) throws ClassNotFoundException {
					return driverClassLoader.loadClass(name);
				}
			};
			ServoyServiceLoader.setClassLoader(combined);
			outputExtra("Installed driver-aware service classloader for headless DB access.");
		} catch (Exception e) {
			ServoyLog.logError("Failed to install driver-aware service classloader.", e);
			outputError("Failed to install driver-aware service classloader: " + e.getMessage());
		}
	}

	/**
	 * Called by {@link AbstractWorkspaceExporter} after the application server is
	 * up, the workspace projects are imported and the requested solution has been
	 * activated. Runs the Cypress form tests instead of exporting a WAR.
	 */
	@Override
	protected void exportActiveSolution(CypressFormTestArgumentChest configuration) {
		try {
			enableTestingMode();

			activateLicenses(configuration);

			activateNgClientBundle();

			activateNgClientSolution(configuration);

			startWebServer();

			int port = waitForWebServer();
			output("Application server ready on port " + port);

			HeadlessFormTestExecutor executor = new HeadlessFormTestExecutor(configuration, this::output,
					this::outputExtra);
			List<FormTestResult> results = executor.execute();

			// always write a report, even for an empty run, so the CI JUnit publisher finds
			// a file
			writeReport(configuration, results);

			if (results.isEmpty()) {
				output("No form tests were discovered.");
				// vacuous pass: nothing to run is not a failure
				return;
			}

			printSummary(results);

			boolean anyError = results.stream().anyMatch(FormTestResult::isError);
			boolean anyFailed = results.stream().anyMatch(r -> !r.isPassed());
			if (anyError) {
				// an errored run indicates an infrastructure problem (e.g. spec missing,
				// timeout)
				outputError("One or more Cypress form tests could not be executed.");
				exitCode = EXIT_EXPORT_FAILED;
			} else if (anyFailed) {
				outputError("One or more Cypress form tests failed.");
				exitCode = EXIT_TESTS_FAILED;
			}
		} catch (Exception e) {
			ServoyLog.logError("Cypress form test run failed.", e);
			outputError("Cypress form test run failed: " + e.getMessage());
			exitCode = EXIT_EXPORT_FAILED;
		}
	}

	/**
	 * Auto-creates tables in in-memory-driver test servers (e.g. {@code servoy_test})
	 * from cached {@code .dbi} metadata, mirroring the Developer-only convenience in
	 * {@code com.servoy.eclipse.core.ServoyModel.updateResources()}.
	 * <p>
	 * The headless exporter family normally has no need for this: WAR/solution/mobile
	 * export reads the repository/model, not live table data. This runner is
	 * different - it renders forms live against a real (even if throwaway, in-memory)
	 * database, so a fresh in-memory HSQLDB server with no auto-create step means any
	 * form whose table hasn't been pre-provisioned by hand fails with "table is not
	 * accessible". This is intentionally scoped to {@code CypressFormTestRunner} only
	 * (not the shared {@code AbstractWorkspaceExporter}/{@code ExportServoyModel}), so
	 * WAR/solution/mobile export behavior is unaffected.
	 * <p>
	 * Must run after {@code ExportServoyModel}'s resources project (and therefore its
	 * {@code DataModelManager}) is set, but before the base class's build/problem-marker
	 * check runs - otherwise a form bound to a not-yet-created in-memory table fails the
	 * build with "table is not accessible" and the run is aborted before
	 * {@code exportActiveSolution()} is ever reached. Called from
	 * {@link #afterSolutionActivated(CypressFormTestArgumentChest, String)}, which
	 * {@link AbstractWorkspaceExporter} invokes at exactly that point.
	 * <p>
	 * <b>Why this does not call {@code EclipseDatabaseUtils.createNewTableFromColumnInfo}:</b>
	 * that method (and every other class in {@code com.servoy.eclipse.core}) throws
	 * {@code NoClassDefFoundError}/{@code RuntimeException} the instant OSGi lazily
	 * activates the bundle, because {@code com.servoy.eclipse.core.Activator.start()}
	 * unconditionally calls {@code ModelUtils.assertUINotDisabled(...)}, which refuses to
	 * start while {@code ModelUtils.setUIDisabled(true)} is in effect (set by
	 * {@code AbstractWorkspaceExporter.start()} for every headless exporter). So this
	 * runner creates tables itself via {@link #createTableFromColumnInfo}, using only
	 * {@code IServerInternal}/{@code ITable}/{@code Column} APIs from {@code servoy_shared}
	 * (no {@code com.servoy.eclipse.core} dependency) and skipping the Developer-only
	 * {@code DataModelManager} write-back bookkeeping, which is irrelevant for a throwaway
	 * in-memory table.
	 */
	private void autoCreateInMemoryTestTables() {
		try {
			IServerManagerInternal serverManager = ApplicationServerRegistry.get().getServerManager();
			DataModelManager dataModelManager = ServoyModelFinder.getServoyModel().getDataModelManager();
			if (dataModelManager == null) {
				outputExtra("No active resources project's DataModelManager available; skipping in-memory test table auto-create.");
				return;
			}

			String[] serverNames = serverManager.getServerNames(true, true, true, true);
			for (String serverName : serverNames) {
				try {
					IServerInternal server = (IServerInternal) serverManager.getServer(serverName, false, false);
					if (server.getConfig().isInMemDriver() && !IServer.INMEM_SERVER.equals(server.getConfig().getServerName())
							&& server.getTableNames(true).size() == 0) {
						IFolder serverInformationFolder = dataModelManager.getServerInformationFolder(server.getName());
						if (serverInformationFolder.exists()) {
							serverInformationFolder.accept((IResource resource) -> {
								String extension = resource.getFileExtension();
								if (extension != null && extension.equalsIgnoreCase(DataModelManager.COLUMN_INFO_FILE_EXTENSION)) {
									String tableName = resource.getName().substring(0,
											resource.getName().length() - DataModelManager.COLUMN_INFO_FILE_EXTENSION_WITH_DOT.length());
									IFile file = dataModelManager.getDBIFile(server.getName(), tableName);
									if (file.exists()) {
										try {
											InputStream is = file.getContents(true);
											String dbiFileContent = Utils.getTXTFileContent(is, Charset.forName("UTF8"));
											Utils.closeInputStream(is);
											String problems = createTableFromColumnInfo(server, tableName, dbiFileContent);
											if (problems != null) {
												outputExtra("Auto-created in-memory test table '" + tableName + "' on server '"
														+ server.getName() + "' with warnings: " + problems);
											} else {
												outputExtra("Auto-created in-memory test table '" + tableName + "' on server '" + server.getName()
														+ "'.");
											}
										} catch (CoreException e) {
											ServoyLog.logError(e);
										}
									}
								}
								return true;
							});
						}
					}
				} catch (Exception e) {
					ServoyLog.logError("Failed to auto-create in-memory test tables for server '" + serverName + "'.", e);
					outputExtra("Failed to auto-create in-memory test tables for server '" + serverName + "': " + e.getMessage());
				}
			}
		} catch (Exception e) {
			ServoyLog.logError("Failed to auto-create in-memory test tables.", e);
			outputExtra("Failed to auto-create in-memory test tables: " + e.getMessage());
		}
	}

	/**
	 * Minimal, headless-safe re-implementation of
	 * {@code com.servoy.eclipse.core.util.EclipseDatabaseUtils.createNewTableFromColumnInfo}
	 * that avoids any dependency on {@code com.servoy.eclipse.core} (see
	 * {@link #autoCreateInMemoryTestTables()} for why that bundle cannot be touched here).
	 * Creates the table and its columns from the given {@code .dbi} JSON content and syncs
	 * it with the live database; deliberately does not update/write back {@code .dbi} file
	 * column info (the {@code NO_UPDATE} behavior), since a throwaway in-memory test table
	 * has no persisted metadata to keep in sync.
	 *
	 * @return null if all is OK; otherwise a newline-separated description of problems
	 *         encountered (mirrors the original method's contract).
	 */
	private String createTableFromColumnInfo(IServerInternal server, String tableName, String dbiFileContent) {
		TableDef tableInfo;
		try {
			tableInfo = DatabaseUtils.deserializeTableInfo(dbiFileContent);
		} catch (JSONException e) {
			return "Corrupt .dbi file: " + e.getMessage();
		}
		if (!tableName.equals(tableInfo.name)) {
			return "Table name does not match dbi file name for " + tableName;
		}
		if (tableInfo.columnInfoDefSet.size() == 0) {
			return "Table " + tableName + " does not have any columns";
		}

		StringBuilder problems = new StringBuilder();
		try {
			ITable table = server.createNewTable(DummyValidator.INSTANCE, tableName, false, true);
			table.setMarkedAsMetaData(Boolean.TRUE.equals(tableInfo.isMetaData));
			server.setTableMarkedAsHiddenInDeveloper(table, tableInfo.hiddenInDeveloper, true);

			List<ColumnInfoDef> columns = new ArrayList<>(tableInfo.columnInfoDefSet);
			columns.sort((o1, o2) -> {
				if (o1.creationOrderIndex != o2.creationOrderIndex) {
					return Integer.compare(o1.creationOrderIndex, o2.creationOrderIndex);
				}
				return o1.name.compareTo(o2.name);
			});
			for (ColumnInfoDef columnInfoDef : columns) {
				Column column = table.createNewColumn(DummyValidator.INSTANCE, columnInfoDef.name, columnInfoDef.columnType);
				column.setDatabasePK((columnInfoDef.flags & IBaseColumn.PK_COLUMN) != 0);
				column.setFlags(columnInfoDef.flags);
				column.setAllowNull(columnInfoDef.allowNull);

				if (columnInfoDef.autoEnterType == ColumnInfo.SEQUENCE_AUTO_ENTER) {
					int sequenceType = columnInfoDef.autoEnterSubType;
					boolean supported;
					try {
						supported = server.supportsSequenceType(sequenceType, null);
					} catch (Exception e) {
						throw new RepositoryException(e);
					}
					if (!supported) {
						problems.append("The import version of the column '" + columnInfoDef.name + "' of table '" + tableInfo.name
								+ "' in server '" + server.getName() + "' has sequence type " + sequenceType
								+ " which is not supported by the database, using the Servoy sequence type instead.\n");
						sequenceType = ColumnInfo.SERVOY_SEQUENCE;
					}
					column.setSequenceType(sequenceType);
				}
			}

			try {
				server.syncTableObjWithDB(table, false, false);
			} catch (Exception e) {
				try {
					if (!table.getExistInDB()) {
						server.removeTable(table);
					}
				} catch (Exception fatal) {
					problems.append("Fatal error: " + fatal.getMessage() + "\n");
					ServoyLog.logError(fatal);
				}
				throw (e instanceof RepositoryException) ? (RepositoryException) e : new RepositoryException(e);
			}

			try {
				server.createMissingDBSequences(table);
			} catch (SQLException e) {
				throw new RepositoryException("Could not create db sequences for table " + table, e);
			}
		} catch (RepositoryException e) {
			ServoyLog.logError(e);
			problems.append(e.getMessage() + "\n");
		}

		return problems.length() > 0 ? problems.toString() : null;
	}

	/**
	 * Activates any client licenses supplied on the command line, using the same
	 * {@code checkClientLicense} call the WAR exporter and the setup pipeline
	 * wizard use. Each NG form-preview client needs a client license to register;
	 * without one the embedded server uses the non-commercial user limit, which the
	 * concurrent clients exhaust ("No more licenses available"). When no license
	 * argument is passed, the server's own {@code servoy.properties} license (if
	 * any) applies and this is a no-op.
	 */
	private void activateLicenses(CypressFormTestArgumentChest configuration) {
		for (License license : configuration.getLicenses()) {
			boolean ok = ApplicationServerRegistry.get().checkClientLicense(license.getCompanyKey(), license.getCode(),
					license.getNumberOfLicenses());
			if (ok) {
				output("Activated client license for '" + license.getCompanyKey() + "'.");
			} else {
				outputError("Invalid client license for '" + license.getCompanyKey() + "' (code "
						+ mask(license.getCode()) + "). NG clients may fail to register with 'No more licenses'.");
			}
		}
	}

	private static String mask(String code) {
		if (code == null || code.length() < 4)
			return "****";
		return code.substring(0, 4) + "-****-****-****";
	}

	private void enableTestingMode() {
		// enables data-cy selectors in the NG client, matching the IDE testForm flow
		Settings.getInstance().setProperty("servoy.ngclient.testingMode", "true");

		// Mark this as a developer startup so that NGClientEntryFilter.init() skips
		// loading components.properties/services.properties (which don't exist in the
		// embedded Tomcat). Without this, the filter's init() throws NPE, filterStart()
		// fails, the StandardContext is marked as broken, and WebSocket upgrades are
		// rejected â€” preventing the NG client from connecting.
		try {
			java.lang.reflect.Method m = ApplicationServerRegistry.get().getClass().getMethod("setDeveloperStartup",
					boolean.class);
			m.invoke(ApplicationServerRegistry.get(), true);
		} catch (Exception e) {
			ServoyLog.logError("Failed to set developer startup mode.", e);
		}

		// The websocket SecurityFilter that normally sets the origin/host check is only
		// configured in a deployed WAR, not in the developer/headless embedded server.
		// Servoy
		// Developer disables the origin check for exactly this reason (see
		// com.servoy.eclipse.core.Activator.startAppServer:
		// GetHttpSessionConfigurator.setOriginCheck(DISABLE_ORIGIN_CHECK)). In
		// UI-disabled headless
		// mode that core code is skipped, so the runner must disable it here -
		// otherwise the NG
		// client websocket upgrade is rejected with "No Host header set for this
		// request" and every
		// form fails to load.
		GetHttpSessionConfigurator.setOriginCheck(GetHttpSessionConfigurator.DISABLE_ORIGIN_CHECK);
	}

	/**
	 * Installs a formpreview-aware {@code IWebsocketSessionFactory} so that a
	 * {@code FormPreviewNGClient} (instead of a regular {@code NGClient}) is created
	 * when the WebSocket session's {@code requestParams} contain "formpreview". The
	 * {@code FormPreviewNGClient} overrides {@code showDefaultLogin()} to bypass
	 * authentication entirely, and its constructor/{@code shutdownExisting()} keep a
	 * single instance so licenses are released between specs.
	 * <p>
	 * This <b>unconditionally overwrites</b> whatever factory is currently registered
	 * for {@code CLIENT_ENDPOINT}. It must not try to detect an "already formpreview-aware"
	 * factory: the one {@code com.servoy.eclipse.ngclient.startup.Activator} registers is
	 * an anonymous {@code IWebsocketSessionFactory} (not a {@code WebsocketSessionFactory}
	 * instance) whose {@code init} has no "formpreview" branch at all, so any
	 * {@code instanceof}-based guard reads that as "already aware", bails out, and no
	 * {@code FormPreviewNGClient} is ever created - the form then falls through to the
	 * regular auth/debug-client path, never renders, and each spec leaks a client until
	 * the license pool is exhausted ("No more licenses available").
	 * <p>
	 * Overwriting is safe: the factory below delegates to plain
	 * {@code NGClientWebsocketSession} behaviour via {@code super.init(requestParams)}
	 * whenever "formpreview" is absent.
	 */
	private void activateNgClientBundle() {
		try {
			WebsocketSessionManager.setWebsocketSessionFactory(WebsocketSessionFactory.CLIENT_ENDPOINT,
					new IWebsocketSessionFactory() {
						@Override
						public IWebsocketSession createSession(WebsocketSessionKey sessionKey) throws Exception {
							return new NGClientWebsocketSession(sessionKey, null) {
								@Override
								public void init(java.util.Map<String, java.util.List<String>> requestParams)
										throws Exception {
									if (getClient() == null && requestParams.containsKey("formpreview")) {
										String formName = requestParams.get("formpreview").get(0);
										FormPreviewNGClient.setPendingTargetFormName(formName);
										setClient(new FormPreviewNGClient(this, null, formName));
									}
									super.init(requestParams);
								}
							};
						}
					});
			outputExtra("Registered formpreview-aware WebSocket session factory for headless mode.");
		} catch (Exception e) {
			ServoyLog.logError("Failed to register formpreview WebSocket factory.", e);
			outputExtra("Failed to register formpreview WebSocket factory: " + e.getMessage());
		}
	}

	/**
	 * Tells the NG client UI layer which solution is active so that
	 * {@code IndexPageFilter} can locate the {@code dist/app/browser/index.html}
	 * built by the TiNG Angular compiler. Without this call,
	 * {@code Activator.getInstance().getSolutionProjectFolder()} returns null and
	 * the {@code formpreview} auth-bypass path in the filter cannot find the index
	 * page â€” causing the solution's authenticator to appear instead.
	 * <p>
	 * In the full IDE this is done by {@code com.servoy.eclipse.core} during
	 * startup; the headless runner must replicate it explicitly.
	 */
	private void activateNgClientSolution(CypressFormTestArgumentChest configuration) {
		try {
			com.servoy.eclipse.ngclient.ui.Activator ngActivator = com.servoy.eclipse.ngclient.ui.Activator
					.getInstance();
			if (ngActivator != null) {
				String solutionName = configuration.getSolutionNames()[0];
				ngActivator.setActiveSolution(solutionName);
				outputExtra("Activated NG client solution: " + solutionName);
			} else {
				outputExtra("com.servoy.eclipse.ngclient.ui not active - formpreview auth bypass may not work.");
			}
		} catch (Exception e) {
			outputExtra("Failed to activate NG client solution: " + e.getMessage());
		}
	}

	/**
	 * Starts the embedded Tomcat web server (the HTTP endpoint Cypress connects
	 * to).
	 * <p>
	 * {@link AbstractWorkspaceExporter} starts the application server (persistence,
	 * model, solution activation) but not the HTTP connector, because exporting a
	 * WAR never needs a live listener. Servoy Developer explicitly calls
	 * {@code startWebServer()} after startup (see
	 * {@code com.servoy.eclipse.core.Activator.startAppServer()}); we do the same
	 * here so {@code http://localhost:<port>/} actually serves.
	 * <p>
	 * <b>Headless note:</b> The {@code org.apache.tomcat} bundle's activator loads
	 * all {@code org.apache.tomcat.serviceprovider} extension-point contributions.
	 * Some providers (e.g. from {@code com.servoy.eclipse.core}) cannot load in a
	 * headless environment. The Tomcat activator tolerates individual provider
	 * failures so the web server still starts successfully.
	 */
	private void startWebServer() {
		IWebServerStarter webStarter = ApplicationServerRegistry.getService(IWebServerStarter.class);
		if (webStarter == null) {
			outputError("No IWebServerStarter registered - cannot start the embedded web server.");
			return;
		}
		outputExtra("Starting embedded web server...");
		webStarter.startWebServer();
	}

	/**
	 * Waits until the embedded Tomcat is actually accepting HTTP connections. It is
	 * not enough for {@link IApplicationServerSingleton#getWebServerPort()} to
	 * return a positive port - that only reports the configured port, not whether
	 * anything is listening. Cypress fails with "could not verify that this server
	 * is running" if it connects before Tomcat is bound, so we poll a real HTTP
	 * request until the connector answers.
	 *
	 * @return the resolved web server port
	 * @throws InterruptedException if interrupted while waiting
	 */
	private int waitForWebServer() throws InterruptedException {
		outputExtra("Waiting for embedded web server...");
		long deadline = System.currentTimeMillis() + SERVER_TIMEOUT_MS;

		int port = ApplicationServerRegistry.get().getWebServerPort();
		while (port <= 0 && System.currentTimeMillis() < deadline) {
			Thread.sleep(SERVER_POLL_INTERVAL_MS);
			port = ApplicationServerRegistry.get().getWebServerPort();
		}
		if (port <= 0) {
			throw new IllegalStateException(
					"Web server port not available within " + (SERVER_TIMEOUT_MS / 1000) + " seconds");
		}

		while (System.currentTimeMillis() < deadline) {
			if (isHttpServing(port)) {
				return port;
			}
			Thread.sleep(SERVER_POLL_INTERVAL_MS);
		}
		throw new IllegalStateException("Web server on port " + port + " did not accept HTTP connections within "
				+ (SERVER_TIMEOUT_MS / 1000) + " seconds");
	}

	/**
	 * Opens a short HTTP request to the embedded server root and reports whether
	 * the connector responded. Any HTTP status code (including 3xx/4xx) proves the
	 * server is listening; only a connection failure means it is not ready yet.
	 *
	 * @param port the web server port to probe
	 * @return {@code true} if the server responded to an HTTP request
	 */
	private boolean isHttpServing(int port) {
		HttpURLConnection connection = null;
		try {
			URL url = URI.create("http://localhost:" + port + "/").toURL();
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(2000);
			connection.setReadTimeout(2000);
			connection.setInstanceFollowRedirects(false);
			// any HTTP response code means the connector is up and answering
			return connection.getResponseCode() > 0;
		} catch (IOException notReadyYet) {
			return false;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	private void writeReport(CypressFormTestArgumentChest configuration, List<FormTestResult> results) {
		try {
			Path outputDir = configuration.getOutputDir();
			Files.createDirectories(outputDir);
			JUnitXmlReporter.writeReport(outputDir, "cypress-form-tests", results);
			output("JUnit XML report written to: " + outputDir.toAbsolutePath());
		} catch (IOException e) {
			outputError("Failed to write JUnit XML report: " + e.getMessage());
		}
	}

	private void printSummary(List<FormTestResult> results) {
		long passed = results.stream().filter(FormTestResult::isPassed).count();
		long failed = results.stream().filter(r -> !r.isPassed() && !r.isError()).count();
		long errors = results.stream().filter(FormTestResult::isError).count();

		output("========================================");
		output("Cypress Form Test Results");
		output("========================================");
		output("Total:   " + results.size());
		output("Passed:  " + passed);
		output("Failed:  " + failed);
		output("Errors:  " + errors);
		output("========================================");
	}
}
