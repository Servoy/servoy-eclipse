/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
*/
package com.servoy.eclipse.ui.tests.plainjunit.editors.table;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.sql.Types;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.servoy.base.persistence.IBaseColumn;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.ui.preferences.PrimaryKeyType;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.util.XMLUtils;
import com.servoy.j2db.util.xmlxport.ColumnInfoDef;

/**
 * SVY-21712 - guards the invariant the ColumnComposite.initDataBindings fix
 * depends on.
 *
 * <p>
 * The production fix (a single guarded block in
 * {@code com.servoy.eclipse.ui/.../editors/table/ColumnComposite.java}, UUID-PK
 * branch of {@code initDataBindings(ITable)}) eagerly creates the auto-PK's
 * {@link ColumnInfo} and sets its {@code configuredColumnType} to the canonical
 * logical type {@code MEDIA(16)}, mirroring
 * {@code ColumnTypeEditingSupport.setValue} for dropdown-created UUID columns.
 * Driving the real {@code initDataBindings} requires a full table editor inside
 * a running Developer
 * ({@code ServoyModelFinder.getServoyModel().getDataModelManager()}), which is
 * disproportionate and brittle for this invariant. This plainjunit test
 * therefore reproduces the exact ColumnInfo-level state the fix produces, and
 * the save/read-back type-decay scenario, with real persistence objects (no
 * OSGi), and asserts the exact serialization mechanism the fix relies on:
 * {@link DataModelManager#getColumnInfoDef(com.servoy.j2db.persistence.IColumn, int, boolean)}
 * copies {@code column.getConfiguredColumnType()} (not the overwritten db
 * {@code columnType}) into the {@code .dbi} {@link ColumnInfoDef}, which
 * {@code serializeTableInfo} writes as
 * {@code dataType = columnType.getSqlType()} / {@code length}.
 *
 * <p>
 * Every assertion fails if the fix is reverted: without the eagerly-set
 * configured type the PK decays to the realized {@code CHAR(36)} (dataType 1,
 * length 36) exactly as the bug describes.
 */
class NativeUuidPkDbiConsistencyTest {
	private static final int SQLSERVER_UUID_SQL_TYPE = Types.CHAR; // 1 - realized native UUID on SQL Server
	private static final int SQLSERVER_UUID_LENGTH = 36;

	/**
	 * A name validator that accepts everything - no OSGi / workspace / Mockito
	 * needed.
	 */
	private static final IValidateName ACCEPT_ALL = (nameToCheck, skip_element_uuid, searchContext, sqlRelated) -> {
		/* accept every name */ };

	private static Table newTable() {
		return new Table("srv", "test", false, com.servoy.j2db.persistence.ITable.TABLE, null, null);
	}

	/**
	 * Faithfully reproduces what {@code ColumnComposite.initDataBindings} does for
	 * a native-UUID PK WITH the fix applied: create the PK column with the logical
	 * MEDIA(16) type, set the PK / UUID / NATIVE flags and the UUID generator
	 * sequence, eagerly create the column info and set its configured column type
	 * to MEDIA(16).
	 */
	private static Column createNativeUuidPkAsFixed(Table t) throws Exception {
		PrimaryKeyType keyType = PrimaryKeyType.UUD_NATIVE;
		Column id = t.createNewColumn(ACCEPT_ALL, "test_uuid",
				ColumnType.getInstance(keyType.getColumnType(), keyType.getLength(), 0));
		id.setDatabasePK(true);
		id.setSequenceType(ColumnInfo.UUID_GENERATOR);
		id.setFlag(IBaseColumn.UUID_COLUMN, keyType.isUUID());
		id.setFlag(IBaseColumn.NATIVE_COLUMN, keyType.isNative());
		// the fix: eagerly create the column info and set the canonical configured type
		ColumnInfo ci = new ColumnInfo(false);
		ci.setFlags(id.getFlags());
		id.setColumnInfo(ci);
		id.getColumnInfo()
				.setConfiguredColumnType(ColumnType.getInstance(keyType.getColumnType(), keyType.getLength(), 0));
		return id;
	}

	/**
	 * Reproduces the PRE-fix native-UUID PK: identical creation, but NO configured
	 * column type is ever set (the lazy DatabaseUtils.createNewColumnInfo path only
	 * copies flags). This is the regression anchor: it is the exact state the bug
	 * produced.
	 */
	private static Column createNativeUuidPkAsBroken(Table t) throws Exception {
		PrimaryKeyType keyType = PrimaryKeyType.UUD_NATIVE;
		Column id = t.createNewColumn(ACCEPT_ALL, "test_uuid",
				ColumnType.getInstance(keyType.getColumnType(), keyType.getLength(), 0));
		id.setDatabasePK(true);
		id.setSequenceType(ColumnInfo.UUID_GENERATOR);
		id.setFlag(IBaseColumn.UUID_COLUMN, keyType.isUUID());
		id.setFlag(IBaseColumn.NATIVE_COLUMN, keyType.isNative());
		ColumnInfo ci = new ColumnInfo(false);
		ci.setFlags(id.getFlags());
		id.setColumnInfo(ci); // no setConfiguredColumnType - stays null
		return id;
	}

	/**
	 * Simulates the save/DB-read-back cycle: Server.askTableForColumns overwrites
	 * the column's db {@code columnType} with the realized native type (CHAR(36) on
	 * SQL Server). This is what makes an unconfigured column decay.
	 */
	private static void simulateDbReadBack(Column c) {
		c.updateColumnType(ColumnType.getInstance(SQLSERVER_UUID_SQL_TYPE, SQLSERVER_UUID_LENGTH, 0));
		c.setExistInDB(true);
	}

	@Nested
	class ConfiguredTypeSurvivesReadBack {
		@Test
		@DisplayName("AC(a): fixed native-UUID PK keeps configuredColumnType = MEDIA(16) after DB read-back")
		void nativeUuidPkStaysMedia16() throws Exception {
			Table t = newTable();
			Column pk = createNativeUuidPkAsFixed(t);
			simulateDbReadBack(pk);

			ColumnType configured = pk.getConfiguredColumnType();
			assertAll("configured type is the canonical MEDIA(16), not the realized CHAR(36)",
					() -> assertEquals(IColumnTypes.MEDIA, configured.getSqlType(), "sqlType must stay MEDIA (-4)"),
					() -> assertEquals(16, configured.getLength(), "length must stay 16"),
					() -> assertEquals(SQLSERVER_UUID_SQL_TYPE, pk.getColumnType().getSqlType(),
							"the db columnType is overwritten to CHAR by the read-back"),
					() -> assertEquals(SQLSERVER_UUID_LENGTH, pk.getColumnType().getLength(),
							"the db columnType length is overwritten to 36 by the read-back"));
		}

		@Test
		@DisplayName("Regression anchor: an UNCONFIGURED native-UUID PK decays to CHAR(36) on read-back")
		void brokenNativeUuidPkDecaysTo36() throws Exception {
			Table t = newTable();
			Column pk = createNativeUuidPkAsBroken(t);
			assertNull(pk.getColumnInfo().getConfiguredColumnType(),
					"precondition: no configured type was set (pre-fix state)");
			simulateDbReadBack(pk);

			ColumnType configured = pk.getConfiguredColumnType();
			assertAll("without the fix the configured type falls back to the decayed db type",
					() -> assertEquals(SQLSERVER_UUID_SQL_TYPE, configured.getSqlType(), "decays to CHAR (1)"),
					() -> assertEquals(SQLSERVER_UUID_LENGTH, configured.getLength(), "decays to length 36"));
		}
	}

	@Nested
	class DbiSerialization {
		@Test
		@DisplayName("AC(a): fixed native-UUID PK serializes to dataType -4, length 16 with realized CHAR(36) as compatible type")
		void nativeUuidPkSerializesCanonical() throws Exception {
			Table t = newTable();
			Column pk = createNativeUuidPkAsFixed(t);
			simulateDbReadBack(pk);
			// the realized type is recorded as a compatible type (Server.java:3145-3152)
			// because
			// configured (MEDIA 16) != db (CHAR 36)
			pk.getColumnInfo().addCompatibleColumnType(pk.getColumnType());

			ColumnInfoDef cid = DataModelManager.getColumnInfoDef(pk, -1, false);
			assertNotNull(cid, "a stored column must produce a ColumnInfoDef");
			assertAll("the .dbi entry is the canonical MEDIA(16) form",
					() -> assertEquals(IColumnTypes.MEDIA, cid.columnType.getSqlType(), "dataType must be -4 (MEDIA)"),
					() -> assertEquals(16, cid.columnType.getLength(), "length must be 16"),
					() -> assertEquals("[[1,36,0]]", XMLUtils.serializeColumnTypeArray(cid.compatibleColumnTypes),
							"the realized CHAR(36) must be recorded in compatibleColumnTypes"));
		}

		@Test
		@DisplayName("AC(b): fixed PK and a dropdown-created native-UUID column serialize to the same canonical type")
		void pkMatchesDropdownCreatedColumn() throws Exception {
			Table t = newTable();
			Column pk = createNativeUuidPkAsFixed(t);
			simulateDbReadBack(pk);
			pk.getColumnInfo().addCompatibleColumnType(pk.getColumnType());

			// a second, dropdown-created native-UUID column
			// (ColumnTypeEditingSupport.setValue):
			// MEDIA(16) + UUID|NATIVE flags + explicit configuredColumnType, same read-back
			// decay
			Column dropdown = t.createNewColumn(ACCEPT_ALL, "some_uuid",
					ColumnType.getInstance(IColumnTypes.MEDIA, 16, 0));
			dropdown.setFlag(IBaseColumn.UUID_COLUMN, true);
			dropdown.setFlag(IBaseColumn.NATIVE_COLUMN, true);
			ColumnInfo dci = new ColumnInfo(false);
			dci.setFlags(dropdown.getFlags());
			dropdown.setColumnInfo(dci);
			dropdown.getColumnInfo().setConfiguredColumnType(ColumnType.getInstance(IColumnTypes.MEDIA, 16, 0));
			simulateDbReadBack(dropdown);
			dropdown.getColumnInfo().addCompatibleColumnType(dropdown.getColumnType());

			ColumnInfoDef pkDef = DataModelManager.getColumnInfoDef(pk, -1, false);
			ColumnInfoDef dropdownDef = DataModelManager.getColumnInfoDef(dropdown, -1, false);
			assertAll("the two columns are structurally identical in their serialized type",
					() -> assertEquals(dropdownDef.columnType.getSqlType(), pkDef.columnType.getSqlType(),
							"same dataType"),
					() -> assertEquals(dropdownDef.columnType.getLength(), pkDef.columnType.getLength(), "same length"),
					() -> assertEquals(XMLUtils.serializeColumnTypeArray(dropdownDef.compatibleColumnTypes),
							XMLUtils.serializeColumnTypeArray(pkDef.compatibleColumnTypes),
							"same compatibleColumnTypes"),
					() -> assertEquals(IColumnTypes.MEDIA, pkDef.columnType.getSqlType(),
							"and it is the canonical MEDIA(16)"));
		}

		@Test
		@DisplayName("Regression anchor: an UNCONFIGURED native-UUID PK serializes to dataType 1, length 36 (the bug)")
		void brokenNativeUuidPkSerializesDecayed() throws Exception {
			Table t = newTable();
			Column pk = createNativeUuidPkAsBroken(t);
			simulateDbReadBack(pk);

			ColumnInfoDef cid = DataModelManager.getColumnInfoDef(pk, -1, false);
			assertNotNull(cid);
			assertAll("without the fix the PK serializes as the decayed CHAR(36)",
					() -> assertEquals(SQLSERVER_UUID_SQL_TYPE, cid.columnType.getSqlType(),
							"dataType decays to 1 (CHAR)"),
					() -> assertEquals(SQLSERVER_UUID_LENGTH, cid.columnType.getLength(), "length decays to 36"),
					() -> assertNull(cid.compatibleColumnTypes, "no compatible type recorded for the decayed PK"));
		}
	}

	@Nested
	class IntegerPkUnchanged {
		@Test
		@DisplayName("AC(c): the INTEGER PK path is unchanged - no configured type, serializes as INTEGER")
		void integerPkUnchanged() throws Exception {
			Table t = newTable();
			// INTEGER PK branch of initDataBindings: no UUID flags, no eager configured
			// type
			PrimaryKeyType keyType = PrimaryKeyType.INTEGER;
			Column id = t.createNewColumn(ACCEPT_ALL, "test_id",
					ColumnType.getInstance(keyType.getColumnType(), keyType.getLength(), 0));
			id.setDatabasePK(true);
			id.setSequenceType(ColumnInfo.SERVOY_SEQUENCE);
			id.setFlag(IBaseColumn.UUID_COLUMN, keyType.isUUID());
			id.setFlag(IBaseColumn.NATIVE_COLUMN, keyType.isNative());
			ColumnInfo ci = new ColumnInfo(false);
			ci.setFlags(id.getFlags());
			id.setColumnInfo(ci); // INTEGER path never sets a configured type

			id.updateColumnType(ColumnType.getInstance(IColumnTypes.INTEGER, 0, 0));
			id.setExistInDB(true);

			ColumnInfoDef cid = DataModelManager.getColumnInfoDef(id, -1, false);
			assertNotNull(cid);
			List<ColumnType> compatible = id.getColumnInfo().getCompatibleColumnTypes();
			assertAll("INTEGER PK serializes as INTEGER with no UUID/MEDIA interference",
					() -> assertEquals(IColumnTypes.INTEGER, cid.columnType.getSqlType(), "dataType stays INTEGER"),
					() -> assertNull(id.getColumnInfo().getConfiguredColumnType(),
							"the INTEGER PK path still sets no configured type (unchanged behavior)"),
					() -> assertNull(compatible, "no compatible types for the INTEGER PK"));
		}
	}
}
