/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.debug.script;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.dltk.compiler.problem.IProblemCategory;
import org.eclipse.dltk.compiler.problem.IProblemIdentifier;
import org.eclipse.dltk.javascript.typeinfo.DefaultMetaType;
import org.eclipse.dltk.javascript.typeinfo.IRMember;
import org.eclipse.dltk.javascript.typeinfo.IRMethod;
import org.eclipse.dltk.javascript.typeinfo.IRRecordMember;
import org.eclipse.dltk.javascript.typeinfo.IRRecordType;
import org.eclipse.dltk.javascript.typeinfo.IRType;
import org.eclipse.dltk.javascript.typeinfo.IRTypeDeclaration;
import org.eclipse.dltk.javascript.typeinfo.ITypeSystem;
import org.eclipse.dltk.javascript.typeinfo.ImmutableType;
import org.eclipse.dltk.javascript.typeinfo.RSimpleType;
import org.eclipse.dltk.javascript.typeinfo.RTypes;
import org.eclipse.dltk.javascript.typeinfo.TypeCompatibility;
import org.eclipse.dltk.javascript.typeinfo.model.Member;
import org.eclipse.dltk.javascript.typeinfo.model.Method;
import org.eclipse.dltk.javascript.typeinfo.model.Type;
import org.eclipse.dltk.javascript.typeinfo.model.Visibility;

/**
 * @author jcompagner
 *
 */
final class CustomTypeMetaType extends DefaultMetaType
{
	private static final ConcurrentHashMap<Type, CustomTypeRecordType> instances = new ConcurrentHashMap<>();

	private final ITypeSystem typeSystem;

	public CustomTypeMetaType(ITypeSystem typeSystem)
	{
		this.typeSystem = typeSystem;
	}

	@Override
	public IRType toRType(ITypeSystem typeSystem, Type type)
	{
		// RTypes.simple(ITypeSystem, Type) hard-casts the return value to IRSimpleType:
		//   return (IRSimpleType) type.toRType(typeSystem);
		// So we CANNOT return a plain RRecordType here - it doesn't implement IRSimpleType
		// and that cast would throw ClassCastException.
		//
		// The solution: if the custom type has no API (no Method members), return a
		// CustomTypeRecordType - a hybrid class that extends RSimpleType (so it IS an
		// IRSimpleType, satisfying the cast) but ALSO implements IRRecordType (so the
		// type inferencer treats it as a structural/record type with all-optional members,
		// allowing plain object literals like  var x = {}  to be assigned to it).
		//
		// If the type has API (Method members), a plain RSimpleType is correct so that
		// method calls on the variable are type-checked normally.

		boolean hasAPI = false;
		for (Member member : type.getMembers())
		{
			if (member instanceof Method)
			{
				hasAPI = true;
				break;
			}
		}

		if (hasAPI)
		{
			// has API methods → plain RSimpleType; method-call checking works normally
			return super.toRType(typeSystem, type);
		}

		// no API → hybrid type: satisfies the IRSimpleType cast AND behaves as IRRecordType
		CustomTypeRecordType custom = instances.computeIfAbsent(type, t -> new CustomTypeRecordType(typeSystem, t));
		custom.init(type, typeSystem);
		instances.clear(); // recursion guard: entries are only needed during construction of this type's
		// call tree; clear afterward so stale entries don't affect future calls
		return custom;
	}

	@Override
	public IRType toRType(IRTypeDeclaration declaration)
	{
		// same logic as toRType(ITypeSystem, Type):
		// if the type has API (Method members) → plain RSimpleType
		// if not → CustomTypeRecordType (all members optional, handles hits==0 for {} assignment)
		for (IRMember member : declaration.getMembers())
		{
			if (member instanceof IRMethod)
			{
				return super.toRType(declaration);
			}
		}

		// no API → build CustomTypeRecordType from the resolved declaration's members
		Map<String, IRRecordMember> recordMembers = new LinkedHashMap<>();
		for (IRMember member : declaration.getMembers())
		{
			recordMembers.put(member.getName(),
				new OptionalRecordMember(member.getName(), member.getType(), member.getSource()));
		}
		return new CustomTypeRecordType(declaration, recordMembers);
	}

	public String getId()
	{
		return "CustomTypeMetaType";
	}

	@Override
	public ITypeSystem getPreferredTypeSystem(Type type)
	{
		return typeSystem;
	}

	/**
	 * Hybrid type that satisfies the hard cast in RTypes.simple(ITypeSystem, Type):
	 *   return (IRSimpleType) type.toRType(typeSystem);
	 *
	 * It extends RSimpleType (so it IS an IRSimpleType) but ALSO implements IRRecordType,
	 * so the type inferencer treats it as a structural/record type whose members are all
	 * optional — allowing a plain object literal  var x = {}  to be validly assigned.
	 *
	 * The members map is built lazily from the Type's members on construction, with every
	 * member wrapped in an OptionalRecordMember.
	 */
	private static final class CustomTypeRecordType extends RSimpleType implements IRRecordType
	{
		private final Map<String, IRRecordMember> members;

		private CustomTypeRecordType(ITypeSystem typeSystem, Type type)
		{
			super(typeSystem, type);
			members = new HashMap<>();
		}

		/**
		 * will only initalizes itself once as long as it doesn't have members..
		 * @param type
		 * @param typeSystem
		 */
		private void init(Type type, ITypeSystem typeSystem)
		{
			if (members.isEmpty())
			{
				for (Member member : type.getMembers())
				{
					IRType memberType = member.getType() != null ? RTypes.create(typeSystem, member.getType()) : RTypes.any();
					members.put(member.getName(), new OptionalRecordMember(member.getName(), memberType, member));
				}
			}
		}

		/** Private constructor used by makeImmutable. */
		private CustomTypeRecordType(IRTypeDeclaration declaration, Map<String, IRRecordMember> members)
		{
			super(declaration);
			this.members = members;
		}


		// --- IRRecordType ---

		@Override
		public IRRecordMember getMember(String name)
		{
			return members.get(name);
		}

		@Override
		public Collection<IRRecordMember> getMembers()
		{
			return members.values();
		}


		@Override
		public void init(ITypeSystem context, org.eclipse.emf.common.util.EList<Member> members)
		{
			// already initialised in constructor; nothing to do
		}

		// --- IRSimpleType / RSimpleType assignment check ---

		@Override
		public TypeCompatibility isAssignableFrom(IRType type)
		{
			// Accept any IRRecordType whose members are compatible (same logic as RRecordType)
			if (type instanceof IRRecordType other)
			{
				if (members.isEmpty()) return TypeCompatibility.TRUE;
				for (IRRecordMember otherMember : other.getMembers())
				{
					IRRecordMember self = members.get(otherMember.getName());
					if (self == null) continue;
					if (!self.getType().isAssignableFrom(otherMember.getType()).ok())
						return TypeCompatibility.FALSE;
				}
				// all our members are always optional (see OptionalRecordMember.isOptional),
				// so we never need to reject based on missing members — {} is always valid
				return TypeCompatibility.TRUE;
			}
			return super.isAssignableFrom(type);
		}

		// --- ImmutableType (required by IRRecordType extends ImmutableType<IRRecordType>) ---

		@Override
		public IRRecordType makeImmutable(Map<Object, Object> visited)
		{
			boolean changed = false;
			Map<String, IRRecordMember> copy = new LinkedHashMap<>();
			for (Map.Entry<String, IRRecordMember> e : members.entrySet())
			{
				IRRecordMember immutable = e.getValue().makeImmutable(visited);
				changed = changed || immutable != e.getValue();
				copy.put(e.getKey(), immutable);
			}
			IRTypeDeclaration decl = getDeclaration();
			if (decl instanceof ImmutableType< ? > t)
			{
				IRTypeDeclaration immutableDecl = (IRTypeDeclaration)t.makeImmutable(visited);
				if (immutableDecl != decl || changed)
					return new CustomTypeRecordType(immutableDecl, copy);
			}
			else if (changed)
			{
				return new CustomTypeRecordType(decl, copy);
			}
			return this;
		}
	}

	/**
	 * An IRRecordMember wrapper that always reports itself as optional.
	 * Used to allow partial object literals to be assigned to a CustomType variable without errors.
	 */
	private static final class OptionalRecordMember implements IRRecordMember
	{
		private final String name;
		private final IRType type;
		private final Object source;

		OptionalRecordMember(String name, IRType type, Object source)
		{
			this.name = name;
			this.type = type != null ? type : RTypes.any();
			this.source = source;
		}

		@Override
		public String getName()
		{
			return name;
		}

		@Override
		public IRType getType()
		{
			return type;
		}

		@Override
		public boolean isOptional()
		{
			return true;
		}

		@Override
		public Visibility getVisibility()
		{
			return Visibility.PUBLIC;
		}

		@Override
		public IRTypeDeclaration getDeclaringType()
		{
			return null;
		}

		@Override
		public boolean isStatic()
		{
			return false;
		}

		@Override
		public boolean isVisible()
		{
			return true;
		}

		@Override
		public Set<IProblemCategory> getSuppressedWarnings()
		{
			return Collections.emptySet();
		}

		@Override
		public boolean isSuppressed(IProblemIdentifier problemIdentifier)
		{
			return false;
		}

		@Override
		public boolean isDeprecated()
		{
			return false;
		}

		@Override
		public Object getSource()
		{
			return source;
		}

		@Override
		public IRRecordMember makeImmutable(Map<Object, Object> visited)
		{
			if (type instanceof ImmutableType< ? > t)
			{
				IRType immutableType = (IRType)t.makeImmutable(visited);
				if (immutableType != type)
				{
					return new OptionalRecordMember(name, immutableType, source);
				}
			}
			return this;
		}
	}
}