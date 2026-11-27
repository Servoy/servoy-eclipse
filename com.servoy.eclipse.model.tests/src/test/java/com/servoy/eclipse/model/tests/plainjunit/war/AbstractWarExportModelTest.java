package com.servoy.eclipse.model.tests.plainjunit.war;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.model.war.exporter.AbstractWarExportModel;
import com.servoy.eclipse.model.war.exporter.AbstractWarExportModel.License;
import com.servoy.eclipse.model.war.exporter.TestWarExportModel;
import com.servoy.eclipse.model.war.exporter.IWarExportModel;


class AbstractWarExportModelTest
{
	@Test
	@DisplayName("decryptPassword returns empty for null; decrypts an encrypted value; decodes a plain base64 value")
	void decrypt() throws Exception
	{
		AbstractWarExportModel model = new TestWarExportModel();

		assertEquals("", model.decryptPassword(null, null));
		assertEquals("", model.decryptPassword(null, ""));

		// plain base64 (no enc prefix)
		String plain = "hello";
		String b64 = java.util.Base64.getEncoder().encodeToString(plain.getBytes(java.nio.charset.StandardCharsets.UTF_8));
		assertEquals(plain, model.decryptPassword(null, b64));

		// encrypted value
		KeyGenerator kg = KeyGenerator.getInstance("DES");
		SecretKey key = kg.generateKey();
		Cipher cipher = Cipher.getInstance("DES");
		cipher.init(Cipher.ENCRYPT_MODE, key);
		String encrypted = IWarExportModel.enc_prefix + java.util.Base64.getEncoder().encodeToString(cipher.doFinal(plain.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

		Cipher dec = Cipher.getInstance("DES");
		dec.init(Cipher.DECRYPT_MODE, key);
		assertEquals(plain, model.decryptPassword(dec, encrypted));
	}

	@Test
	@DisplayName("decryptPassword returns empty when decryption fails")
	void decryptFailure()
	{
		AbstractWarExportModel model = new TestWarExportModel();
		// malformed payload -> exception caught -> empty
		assertEquals("", model.decryptPassword(null, model.enc_prefix + "!!!!"));
	}

	@Test
	@DisplayName("License equals/hashCode/containsLicense by code")
	void license()
	{
		License a = new License("c", "code1", "5");
		License b = new License("c", "code1", "5");
		License c = new License("c", "code2", "5");

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertFalse(a.equals(c));
		assertFalse(a.equals("x"));
		assertEquals("c", a.getCompanyKey());
		assertEquals("code1", a.getCode());
		assertEquals("5", a.getNumberOfLicenses());

		AbstractWarExportModel model = new TestWarExportModel();
		model.addLicense(a);
		assertTrue(model.containsLicense("code1"));
		assertFalse(model.containsLicense("nope"));
		assertEquals(1, model.getLicenses().size());
	}

	@Test
	@DisplayName("replaceLicenseCode moves the license to the new code")
	void replaceCode()
	{
		AbstractWarExportModel model = new TestWarExportModel();
		License l = new License("c", "old", "1");
		model.addLicense(l);
		model.replaceLicenseCode(l, "new");
		assertTrue(model.containsLicense("new"));
		assertFalse(model.containsLicense("old"));
	}

	@Test
	@DisplayName("writeLicenses writes one entry per license with an encrypted code")
	void writeLicenses() throws Exception
	{
		License l = new License("Acme", "SECRET", "3");
		Collection<License> licenses = new ArrayList<>();
		licenses.add(l);

		Properties p = new Properties();
		AbstractWarExportModel.writeLicenses(p, licenses);

		assertEquals("1", p.getProperty("licenseManager.numberOfLicenses"));
		assertEquals("Acme", p.getProperty("license.0.company_name"));
		assertEquals("3", p.getProperty("license.0.licenses"));
		assertEquals("0", p.getProperty("license.0.product"));
		assertTrue(p.getProperty("license.0.code").startsWith(IWarExportModel.enc_prefix));
	}
}
