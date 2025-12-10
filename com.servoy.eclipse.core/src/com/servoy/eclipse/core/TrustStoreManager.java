package com.servoy.eclipse.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

public class TrustStoreManager
{
	// Default system cacerts path and password
	private static final String DEFAULT_CACERTS_PATH = System.getProperty("java.home") + "/lib/security/cacerts";

	private static final String DEFAULT_CACERTS_PASSWORD = "changeit";

	private static final String certAlias = "ServoyDeveloper2025"; // this should be updated when the certificate is updated

	public static void initializeTrustStore(File customTrustStore, String certificateName) throws Exception
	{
		if (!customTrustStore.exists())
		{
			File defaultCacerts = new File(DEFAULT_CACERTS_PATH);

			// 1. Copy the default cacerts file
			Files.copy(defaultCacerts.toPath(), customTrustStore.toPath());
		}

		// 2. Load the copied trust store
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		try (FileInputStream fis = new FileInputStream(customTrustStore))
		{
			ks.load(fis, DEFAULT_CACERTS_PASSWORD.toCharArray());
		}

		// 3. Add the custom certificate
		if (!ks.containsAlias(certAlias))
		{
			// 4. Load the custom certificate from file
			try (InputStream certIn = TrustStoreManager.class.getResourceAsStream(certificateName))
			{
				CertificateFactory cf = CertificateFactory.getInstance("X.509");
				Certificate customCert = cf.generateCertificate(certIn);
				ks.setCertificateEntry(certAlias, customCert);
			}

			// 5. Save the modified trust store
			try (FileOutputStream fos = new FileOutputStream(customTrustStore))
			{
				ks.store(fos, DEFAULT_CACERTS_PASSWORD.toCharArray());
			}
		}

		// 6. Set the system properties for the current JVM
		System.setProperty("javax.net.ssl.trustStore", customTrustStore.getAbsolutePath());
		System.setProperty("javax.net.ssl.trustStorePassword", DEFAULT_CACERTS_PASSWORD);
	}
}
