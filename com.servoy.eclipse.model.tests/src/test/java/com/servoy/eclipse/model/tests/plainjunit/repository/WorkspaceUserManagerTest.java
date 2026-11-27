/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2026 Servoy BV

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

package com.servoy.eclipse.model.tests.plainjunit.repository;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.model.repository.WorkspaceUserManager;
import com.servoy.eclipse.model.repository.WorkspaceUserManager.User;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.server.shared.IUserManager;
import com.servoy.j2db.server.shared.SecurityInfo;
import com.servoy.j2db.util.Utils;

/**
 * {@link WorkspaceUserManager}: the static (de)serializers of the security files, {@link User}, and the in-memory user/group API.
 * The manager is made operational without a resources project and in manual write mode, so nothing is written to the workspace.
 */
class WorkspaceUserManagerTest
{
	private static final String CLIENT = "client";

	@Nested
	@DisplayName("User")
	class Users
	{
		@Test
		@DisplayName("toJSON / fromJSON round trip, password_last_set_time defaults to 0")
		void jsonRoundTrip() throws Exception
		{
			User user = new User("anna", "hash", "uid-1", 42L);

			User back = User.fromJSON(new JSONObject(user.toJSON().toString()));
			JSONObject withoutTime = new JSONObject("{\"name\":\"bob\",\"password_hash\":\"h\",\"user_uid\":\"u\"}");

			assertAll(() -> assertEquals("anna", back.name), () -> assertEquals("hash", back.passwordHash), () -> assertEquals("uid-1", back.userUid),
				() -> assertEquals(42L, back.passwordLastSetTime), () -> assertEquals(0L, User.fromJSON(withoutTime).passwordLastSetTime));
		}

		@Test
		@DisplayName("users sort case insensitively by name, then hash, then uid; equal only when all three match")
		void ordering()
		{
			User a = new User("anna", "h1", "u1");
			assertAll(() -> assertTrue(a.compareTo(new User("Bob", "h1", "u1")) < 0), () -> assertTrue(new User("Anna", "h1", "u1").compareTo(a) < 0),
				() -> assertTrue(a.compareTo(new User("anna", "h2", "u1")) < 0), () -> assertTrue(a.compareTo(new User("anna", "h1", "u2")) < 0),
				() -> assertTrue(new User(null, "h", "u").compareTo(a) < 0), () -> assertTrue(a.compareTo(new User("anna", null, "u1")) > 0),
				() -> assertEquals(a, new User("anna", "h1", "u1")), () -> assertNotEquals(a, new User("anna", "h1", "u2")),
				() -> assertNotEquals(a, "anna"));
		}

		@Test
		@DisplayName("clone copies name, hash and uid; toString shows them")
		void cloneAndToString()
		{
			User a = new User("anna", "h1", "u1", 5L);
			User copy = a.clone();

			assertAll(() -> assertEquals(a, copy), () -> assertEquals("Name: anna, PWD Hash: h1, UID: u1", a.toString()),
				// pins current behaviour: the password last set time is not cloned
				() -> assertEquals(0L, copy.passwordLastSetTime));
		}
	}

	@Nested
	@DisplayName("static (de)serializers")
	class Serializers
	{
		@Test
		@DisplayName("no groups, users or memberships serialize to an empty string, which deserializes to nothing")
		void empty() throws Exception
		{
			List<String> groups = new ArrayList<>();
			List<User> users = new ArrayList<>();
			Map<String, List<String>> memberships = new HashMap<>();

			assertEquals("", WorkspaceUserManager.serializeUserAndGroupInfo(groups, users, memberships));
			WorkspaceUserManager.deserializeUserAndGroupInfo("{}", groups, users, memberships);
			assertAll(() -> assertTrue(groups.isEmpty()), () -> assertTrue(users.isEmpty()), () -> assertTrue(memberships.isEmpty()));
		}

		@Test
		@DisplayName("groups, users and non-empty memberships round trip; empty memberships are left out")
		void roundTrip() throws Exception
		{
			Map<String, List<String>> memberships = new HashMap<>();
			memberships.put("Administrators", List.of("u1"));
			memberships.put("empty", List.of());
			String json = WorkspaceUserManager.serializeUserAndGroupInfo(List.of("Administrators", "empty"),
				List.of(new User("anna", "h1", "u1", 7L)), memberships);

			List<String> groups = new ArrayList<>();
			List<User> users = new ArrayList<>();
			Map<String, List<String>> back = new HashMap<>();
			WorkspaceUserManager.deserializeUserAndGroupInfo(json, groups, users, back);

			assertAll(() -> assertEquals(List.of("Administrators", "empty"), groups), () -> assertEquals(List.of(new User("anna", "h1", "u1")), users),
				() -> assertEquals(7L, users.get(0).passwordLastSetTime), () -> assertEquals(Map.of("Administrators", List.of("u1")), back));
		}

		@Test
		@DisplayName("security permissions round trip per group; groups without entries are left out")
		void permissionsRoundTrip() throws Exception
		{
			Map<String, List<SecurityInfo>> access = new HashMap<>();
			access.put("g1", List.of(new SecurityInfo("col_a", 1), new SecurityInfo("col_b", 3)));
			access.put("g2", List.of());

			Map<String, List<SecurityInfo>> back = WorkspaceUserManager
				.deserializeSecurityPermissionInfo(WorkspaceUserManager.serializeSecurityPermissionInfo(access));

			assertAll(() -> assertEquals(List.of("g1"), new ArrayList<>(back.keySet())),
				() -> assertEquals(List.of("col_a=1", "col_b=3"), back.get("g1").stream().map(s -> s.element_uid + "=" + s.access).toList()));
		}

		@Test
		@DisplayName("a group with an empty permission object is not added when deserializing")
		void emptyGroupPermissions() throws Exception
		{
			assertEquals(Map.of(), WorkspaceUserManager.deserializeSecurityPermissionInfo("{\"g1\":{}}"));
		}
	}

	@Nested
	@DisplayName("in-memory users and groups")
	class InMemory
	{
		private WorkspaceUserManager manager;

		@BeforeEach
		void setUp()
		{
			manager = new WorkspaceUserManager();
			manager.setWriteMode(WorkspaceUserManager.WRITE_MODE_MANUAL);
			manager.setOperational(true);
		}

		private int user(String name, String uid) throws Exception
		{
			return manager.createUser(CLIENT, name, "secret-" + name, uid, false);
		}

		@Test
		@DisplayName("a manager without resources project that is not set operational refuses changes")
		void notOperational() throws Exception
		{
			WorkspaceUserManager idle = new WorkspaceUserManager();

			assertAll(() -> assertFalse(idle.isOperational()),
				() -> assertEquals(IUserManager.ERR_RESOURCE_PROJECT_MISSING, idle.createUser(CLIENT, "a", "b", null, false)),
				() -> assertEquals(-1, idle.createGroup(CLIENT, "g")), () -> assertNull(idle.getUserName(CLIENT, "u1")),
				() -> assertFalse(idle.setPassword(CLIENT, "u1", "x")), () -> assertFalse(idle.deleteUser(CLIENT, "u1")),
				() -> assertFalse(idle.deleteGroups(CLIENT, List.of("g"))));
		}

		@Test
		@DisplayName("createUser stores a PBKDF2 hash, finds the user by name, uid and id; empty input and duplicate names are refused")
		void createUser() throws Exception
		{
			int id = user("anna", "u1");

			assertAll(() -> assertTrue(id > 0), () -> assertEquals("u1", manager.getUserUID(CLIENT, "anna")),
				() -> assertEquals("anna", manager.getUserName(CLIENT, "u1")), () -> assertEquals("anna", manager.getUserName(CLIENT, id)),
				() -> assertEquals("u1", manager.getUserUID(CLIENT, id)), () -> assertEquals(id, manager.getUserIdByUID(CLIENT, "u1")),
				() -> assertEquals(id, manager.getUserIdByUserName(CLIENT, "anna")), () -> assertEquals(-1, manager.getUserIdByUID(CLIENT, "nobody")),
				() -> assertTrue(Utils.validatePrefixedPBKDF2Hash("secret-anna", manager.getUserPasswordHash(CLIENT, "u1"))),
				() -> assertEquals(IUserManager.ERR_USERNAME_EXISTS, user("anna", "u2")),
				() -> assertEquals(IUserManager.ERR_EMPTY_USERNAME_OR_PASSWORD, manager.createUser(CLIENT, " ", "x", null, false)),
				() -> assertEquals(IUserManager.ERR_EMPTY_USERNAME_OR_PASSWORD, manager.createUser(CLIENT, "x", "", null, false)));
		}

		@Test
		@DisplayName("createUser without uid generates one; an already hashed password is stored as is")
		void generatedUidAndPrehashed() throws Exception
		{
			manager.createUser(CLIENT, "bob", "prehashed", null, true);
			String uid = manager.getUserUID(CLIENT, "bob");

			assertAll(() -> assertEquals(36, uid.length()), () -> assertEquals("prehashed", manager.getUserPasswordHash(CLIENT, uid)));
		}

		@Test
		@DisplayName("passwords are checked against the PBKDF2 hash and, for old users, the MD5 hash")
		void checkPassword() throws Exception
		{
			user("anna", "u1");
			manager.createUser(CLIENT, "old", Utils.calculateMD5HashBase64("pw"), "u2", true);

			assertAll(() -> assertEquals("u1", manager.checkPasswordForUserName(CLIENT, "anna", "secret-anna")),
				() -> assertNull(manager.checkPasswordForUserName(CLIENT, "anna", "wrong")),
				() -> assertNull(manager.checkPasswordForUserName(CLIENT, "nobody", "x")),
				() -> assertNull(manager.checkPasswordForUserName(CLIENT, "anna", "")),
				() -> assertEquals("u2", manager.checkPasswordForUserName(CLIENT, "old", "pw")),
				() -> assertTrue(manager.checkPasswordForUserUID(CLIENT, "u1", "secret-anna")),
				() -> assertTrue(manager.checkPasswordForUserUID(CLIENT, "u2", "pw")),
				() -> assertFalse(manager.checkPasswordForUserUID(CLIENT, "u1", "wrong")),
				() -> assertFalse(manager.checkPasswordForUserUID(CLIENT, "nobody", "x")),
				() -> assertFalse(manager.checkPasswordForUserUID(CLIENT, null, "x")));
		}

		@Test
		@DisplayName("setPassword rehashes and records the time; unknown users are refused")
		void setPassword() throws Exception
		{
			user("anna", "u1");
			long before = System.currentTimeMillis();

			assertAll(() -> assertTrue(manager.setPassword(CLIENT, "u1", "new")), () -> assertTrue(manager.checkPasswordForUserUID(CLIENT, "u1", "new")),
				() -> assertTrue(manager.getPasswordLastSet(CLIENT, "u1") >= before), () -> assertFalse(manager.setPassword(CLIENT, "u9", "new")),
				() -> assertEquals(-1, manager.getPasswordLastSet(CLIENT, "u9")), () -> assertTrue(manager.setPassword(CLIENT, "u1", "raw", false)),
				() -> assertEquals("raw", manager.getUserPasswordHash(CLIENT, "u1")));
		}

		@Test
		@DisplayName("groups get ids, memberships are listed per user and per group, administrators are detected")
		void groupsAndMemberships() throws Exception
		{
			int anna = user("anna", "u1");
			int bob = user("bob", "u2");
			int admins = manager.createGroup(CLIENT, IRepository.ADMIN_GROUP);
			int sales = manager.createGroup(CLIENT, "sales");

			assertAll(() -> assertEquals(-1, manager.createGroup(CLIENT, "sales"), "duplicate group"),
				() -> assertEquals(-1, manager.createGroup(CLIENT, " "), "blank group"),
				() -> assertFalse(manager.checkIfAdministratorsAreAvailable(CLIENT)));

			assertTrue(manager.addUserToGroup(CLIENT, anna, admins));
			assertTrue(manager.addUserToGroup(CLIENT, anna, sales));
			assertTrue(manager.addUserToGroup(CLIENT, anna, sales));
			assertTrue(manager.addUserToGroup(CLIENT, bob, sales));

			IDataSet annaGroups = manager.getUserGroups(CLIENT, anna);
			IDataSet salesUsers = manager.getUsersByGroup(CLIENT, "sales");
			assertAll(() -> assertArrayEquals(new String[] { IRepository.ADMIN_GROUP, "sales" }, manager.getUserGroups(CLIENT, "u1")),
				() -> assertEquals(2, annaGroups.getRowCount()), () -> assertEquals(2, salesUsers.getRowCount()),
				() -> assertEquals(2, manager.getGroups(CLIENT).getRowCount()), () -> assertEquals(2, manager.getUsers(CLIENT).getRowCount()),
				() -> assertEquals(sales, manager.getGroupId(CLIENT, "sales")), () -> assertEquals("sales", manager.getGroupNameById(CLIENT, sales)),
				() -> assertEquals(-1, manager.getGroupId(CLIENT, "nope")), () -> assertNull(manager.getGroupNameById(CLIENT, 999)),
				() -> assertTrue(manager.checkIfAdministratorsAreAvailable(CLIENT)), () -> assertTrue(manager.checkIfUserIsAdministrator(CLIENT, "u1")),
				() -> assertFalse(manager.checkIfUserIsAdministrator(CLIENT, "u2")), () -> assertFalse(manager.addUserToGroup(CLIENT, 999, sales)),
				() -> assertFalse(manager.addUserToGroup(CLIENT, -1, sales)), () -> assertNull(manager.getUserGroups(CLIENT, 999)));
		}

		@Test
		@DisplayName("removing a user from a group, renaming a group and deleting groups")
		void groupChanges() throws Exception
		{
			int anna = user("anna", "u1");
			int sales = manager.createGroup(CLIENT, "sales");
			manager.createGroup(CLIENT, "other");
			manager.addUserToGroup(CLIENT, anna, sales);

			assertTrue(manager.removeUserFromGroup(CLIENT, anna, sales));
			assertEquals(0, manager.getUserGroups(CLIENT, "u1").length);
			assertFalse(manager.changeGroupName(CLIENT, sales, "other"), "the new name is taken");
			assertTrue(manager.changeGroupName(CLIENT, sales, "marketing"));
			assertAll(() -> assertEquals(-1, manager.getGroupId(CLIENT, "sales")), () -> assertTrue(manager.getGroupId(CLIENT, "marketing") > 0),
				() -> assertEquals("marketing", manager.getGroupInfos().stream().filter(g -> g.getName().equals("marketing")).findFirst().get().getName()));

			// pins current behaviour: after the rename the id still maps to the old name, so getGroupNameById does not find it
			assertNull(manager.getGroupNameById(CLIENT, sales));
		}

		@Test
		@DisplayName("deleteUser removes the user and its memberships; unknown users are refused")
		void deleteUser() throws Exception
		{
			int anna = user("anna", "u1");
			int sales = manager.createGroup(CLIENT, "sales");
			manager.addUserToGroup(CLIENT, anna, sales);

			assertAll(() -> assertTrue(manager.deleteUser(CLIENT, anna)), () -> assertNull(manager.getUserUID(CLIENT, "anna")),
				() -> assertEquals(0, manager.getUsersByGroup(CLIENT, "sales").getRowCount()), () -> assertFalse(manager.deleteUser(CLIENT, "u1")),
				() -> assertFalse(manager.deleteUser(CLIENT, (String)null)));
		}

		@Test
		@DisplayName("changeUserName and setUserUID keep the user id; taken names and unknown uids are refused")
		void renameUser() throws Exception
		{
			int anna = user("anna", "u1");
			user("bob", "u2");

			assertAll(() -> assertFalse(manager.changeUserName(CLIENT, "u1", "bob")), () -> assertTrue(manager.changeUserName(CLIENT, "u1", "annie")),
				() -> assertEquals("u1", manager.getUserUID(CLIENT, "annie")), () -> assertFalse(manager.changeUserName(CLIENT, "u9", "x")),
				() -> assertTrue(manager.setUserUID(CLIENT, "u1", "u1-new")), () -> assertEquals(anna, manager.getUserIdByUID(CLIENT, "u1-new")),
				() -> assertEquals("u1-new", manager.getUserUID(CLIENT, anna)), () -> assertFalse(manager.setUserUID(CLIENT, "u9", "x")),
				() -> assertFalse(manager.setUserUID(CLIENT, "u2", "")));
		}

		@Test
		@DisplayName("copyDataFrom makes a deep, manual-write copy")
		void copyDataFrom() throws Exception
		{
			int anna = user("anna", "u1");
			int sales = manager.createGroup(CLIENT, "sales");
			manager.addUserToGroup(CLIENT, anna, sales);
			WorkspaceUserManager copy = new WorkspaceUserManager();

			copy.copyDataFrom(manager);
			manager.changeUserName(CLIENT, "u1", "changed");

			assertAll(() -> assertEquals("u1", copy.getUserUID(CLIENT, "anna")), () -> assertArrayEquals(new String[] { "sales" }, copy.getUserGroups(CLIENT, "u1")),
				() -> assertEquals(anna, copy.getUserIdByUID(CLIENT, "u1")), () -> assertEquals(WorkspaceUserManager.WRITE_MODE_MANUAL, copy.getWriteMode()),
				() -> assertTrue(copy.isOperational()));
		}

		@Test
		@DisplayName("table and form security access is kept per group in memory")
		void securityAccess() throws Exception
		{
			manager.createGroup(CLIENT, "sales");
			manager.setTableSecurityAccess(CLIENT, "sales", Integer.valueOf(3), "example_data", "orders", "order_id");

			assertEquals(List.of("order_id=3"),
				manager.getGroupInfos().get(0).tableSecurity.get("example_data.orders").stream().map(s -> s.element_uid + "=" + s.access).toList());
		}
	}
}
