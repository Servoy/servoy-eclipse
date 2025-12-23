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

package com.servoy.eclipse.knowledgebase;

/**
 * Interface for knowledge base operations exposed via extension point.
 * Allows UI plugins to interact with knowledge base without reflection.
 * 
 * @author mvid
 * @since 2026.3
 */
public interface IKnowledgeBaseOperations
{
	/** Extension point ID */
	String EXTENSION_ID = Activator.PLUGIN_ID + ".knowledgeBaseOperations";

	/**
	 * Load knowledge base from a specific package by name.
	 * @param packageName the package name to load
	 */
	void loadKnowledgeBase(String packageName);

	/**
	 * Reload all knowledge bases for the currently active solution.
	 */
	void reloadAllKnowledgeBases();

	/**
	 * Load knowledge bases for a specific solution.
	 * @param solutionName the solution name
	 */
	void loadKnowledgeBasesForSolution(String solutionName);
}
