/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.eclipse.ui.scripting;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.dltk.ui.PreferenceConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;

import com.servoy.eclipse.ui.Activator;
import com.servoy.j2db.scripting.IExecutingEnviroment;

public class ScriptColorProvider /* implements IScriptColorProvider, IScriptColorPreferenceProvider */
{
	private static final String BOLD_KEY = PreferenceConstants.EDITOR_BOLD_SUFFIX;

	/**
	 * Preference key suffix for italic preferences.
	 */
	private static final String ITALIC_KEY = PreferenceConstants.EDITOR_ITALIC_SUFFIX;

	/**
	 * Preference key suffix for strike through preferences.
	 */
	private static final String STRIKETHROUGH_KEY = PreferenceConstants.EDITOR_STRIKETHROUGH_SUFFIX;

	/**
	 * Preference key suffix for underline preferences.
	 */
	private static final String UNDERLINE_KEY = PreferenceConstants.EDITOR_UNDERLINE_SUFFIX;

	private static String COLOR_KEY = "color"; //$NON-NLS-1$

	private static String SERVOY_CAT = "Keywords";
	private static String SERVOY_OP = "Math Operators";
	private static String SERVOY_COMPARE_OPERATORS = "Compare operators";
	private static String SERVOY_AUX_OPERATOR = "Additional";
	private static String SERVOY_MAIN = "Servoy";

	private final Map<String, IToken> keywords;
	private final Map<String, IToken> symbols;
	private IRule[] rules;

	private final IPreferenceStore fPreferenceStore;
	private final ISharedTextColors sharedTextColors;

//	private ColorProviderCategory servoy_cat;
//
//	private ColorProviderCategory servoy_op;
//
//	private ColorProviderCategory servoy_aux;
//
//	private ColorProviderCategory servoy_comp;
//
//	private ColorProviderCategory servoy_main;

	public ScriptColorProvider()
	{
		fPreferenceStore = Activator.getDefault().getPreferenceStore();
		sharedTextColors = Activator.getDefault().getSharedTextColors();
		keywords = new HashMap<String, IToken>();
		symbols = new HashMap<String, IToken>();
		initDefaults();
		createKeywords();
		createRules();
	}

	private void initDefaults()
	{
////		servoy_cat = new ColorProviderCategory(SERVOY_CAT);
////		servoy_op = new ColorProviderCategory(SERVOY_OP);
////		servoy_aux = new ColorProviderCategory(SERVOY_AUX_OPERATOR);
////		servoy_comp = new ColorProviderCategory(SERVOY_COMPARE_OPERATORS);
////		servoy_main = new ColorProviderCategory(SERVOY_MAIN);
//
//		initKey(IExecutingEnviroment.TOPLEVEL_JSUNIT, new RGB(0, 200, 0), false, false, false, false, servoy_cat);
//
//		initKey(IExecutingEnviroment.TOPLEVEL_UTILS, new RGB(0, 200, 0), false, false, false, false, servoy_cat);
//
//		initKey(IExecutingEnviroment.TOPLEVEL_SECURITY, new RGB(0, 200, 0), false, false, false, false, servoy_cat);
//
//		initKey("elements", new RGB(100, 200, 0), false, false, false, false, servoy_cat);
//
//		initKey("controller", new RGB(50, 200, 0), false, false, false, false, servoy_cat);
//
//		initKey("currentcontroller", new RGB(50, 200, 0), false, false, false, false, servoy_cat);
//
//		initKey(IExecutingEnviroment.TOPLEVEL_APPLICATION, new RGB(0, 200, 0), false, false, false, false, servoy_cat);
//
//		initKey(IExecutingEnviroment.TOPLEVEL_DATABASE_MANAGER, new RGB(0, 200, 0), false, false, false, false, servoy_cat);
//
//		initKey(IExecutingEnviroment.TOPLEVEL_SOLUTION_MODIFIER, new RGB(0, 200, 0), false, false, false, false, servoy_cat);
//
//		initKey("globals", new RGB(0, 200, 50), false, false, false, false, servoy_cat);
//
//		initKey(IExecutingEnviroment.TOPLEVEL_FORMS, new RGB(0, 200, 100), false, false, false, false, servoy_cat);
//
//		initKey(IExecutingEnviroment.TOPLEVEL_HISTORY, new RGB(0, 200, 0), false, false, false, false, servoy_cat);
//
//		initKey(IExecutingEnviroment.TOPLEVEL_PLUGINS, new RGB(255, 0, 0), false, false, false, false, servoy_cat);
//
//		initKey(SERVOY_COMPARE_OPERATORS, new RGB(0, 0, 255), true, false, false, false, servoy_main);
//
//		initKey("_super", new RGB(85, 0, 127), true, false, false, false, servoy_aux);
//		/*
//		 * initKey("[", new RGB(0, 0, 255), true, false, false, false, servoy_aux);
//		 * 
//		 * initKey("]", new RGB(0, 0, 255), true, false, false, false, servoy_aux);
//		 */
//		initKey("null", new RGB(0, 0, 255), true, false, false, false, servoy_aux);
//
//		initKey("undefined", new RGB(0, 0, 255), true, false, false, false, servoy_aux);
///*
// * initKey("</", new RGB(0, 0, 255), false, false, false, false, servoy_aux);
// */
//
//		initKey(SERVOY_OP, new RGB(255, 0, 0), false, false, false, false, servoy_main);
//
//		initKey("TODO", new RGB(180, 0, 0), false, true, false, false, servoy_aux);
//
//		initKey("CHECKME", new RGB(180, 0, 0), false, true, false, false, servoy_aux);
//
//		initKey("FIXME", new RGB(180, 0, 0), false, true, false, false, servoy_aux);
//
//		servoy_main.addItem(servoy_cat);
//		servoy_main.addItem(servoy_aux);


	}

	/*
	 * void initKey(String key, RGB defRgbValue, boolean defIsBold, boolean defIsItalic, boolean defIsStrikeThrough, boolean defIsUnderline,
	 * ColorProviderCategory category) { if (getRgbFor(getColorKey(key)) == null) { PreferenceConverter.setDefault(fPreferenceStore, getColorKey(key),
	 * defRgbValue); }
	 * 
	 * ColorProviderCategory item = new ColorProviderCategory(key, getColorKey(key)); category.addItem(item);
	 * 
	 * //bold if (getDecorationFor(getBoldKey(key)) == null) { fPreferenceStore.setDefault(getBoldKey(key), defIsBold); }
	 * 
	 * //italic if (getDecorationFor(getItalicKey(key)) == null) { fPreferenceStore.setDefault(getItalicKey(key), defIsItalic); }
	 * 
	 * //strike through if (getDecorationFor(getStrikeThroughKey(key)) == null) { fPreferenceStore.setDefault(getStrikeThroughKey(key), defIsStrikeThrough); }
	 * 
	 * //underline if (getDecorationFor(getUnderlineKey(key)) == null) { fPreferenceStore.setDefault(getUnderlineKey(key), defIsUnderline); } }
	 */

	private Boolean getDecorationFor(String key)
	{
		if (fPreferenceStore.isDefault(key) || !fPreferenceStore.contains(key))
		{
			return null;
		}
		return new Boolean(fPreferenceStore.getBoolean(key));
	}

	private RGB getRgbFor(String colorKey)
	{
		if (fPreferenceStore.contains(colorKey))
		{
			return PreferenceConverter.getColor(fPreferenceStore, colorKey);
		}
		return null;
	}

	/**
	 * 
	 */
	private void createKeywords()
	{
		createKeyword(IExecutingEnviroment.TOPLEVEL_JSUNIT);
		createKeyword(IExecutingEnviroment.TOPLEVEL_UTILS);
		createKeyword(IExecutingEnviroment.TOPLEVEL_SECURITY);
		createKeyword("elements"); //$NON-NLS-1$
		createKeyword("controller"); //$NON-NLS-1$
		createKeyword("currentcontroller"); //$NON-NLS-1$
		createKeyword(IExecutingEnviroment.TOPLEVEL_APPLICATION);
		createKeyword(IExecutingEnviroment.TOPLEVEL_DATABASE_MANAGER);
		createKeyword(IExecutingEnviroment.TOPLEVEL_SOLUTION_MODIFIER);
		createKeyword("globals"); //$NON-NLS-1$
		createKeyword(IExecutingEnviroment.TOPLEVEL_FORMS);
		createKeyword(IExecutingEnviroment.TOPLEVEL_HISTORY);
		createKeyword(IExecutingEnviroment.TOPLEVEL_PLUGINS);

		createKeyword("null"); //$NON-NLS-1$
		createKeyword("undefined"); //$NON-NLS-1$
		createKeyword("_super"); //$NON-NLS-1$

		createKeyword("FIXME"); //$NON-NLS-1$
		createKeyword("CHECKME"); //$NON-NLS-1$
		createKeyword("TODO"); //$NON-NLS-1$
	}

	private void createKeyword(String keyword)
	{
		keywords.put(keyword, new Token(createTextAttribute(keyword)));

	}

	private void createSymbol(String symbol, String category)
	{
		symbols.put(symbol, new Token(createTextAttribute(category)));

	}

	private void createSymbol(String symbol)
	{
		symbols.put(symbol, new Token(createTextAttribute(symbol)));
	}

	/**
	 * 
	 */
	private void createRules()
	{
		createSymbol("!", SERVOY_COMPARE_OPERATORS);//$NON-NLS-1$
		createSymbol("!==", SERVOY_COMPARE_OPERATORS);//$NON-NLS-1$
		createSymbol("===", SERVOY_COMPARE_OPERATORS);//$NON-NLS-1$
		createSymbol("==", SERVOY_COMPARE_OPERATORS);//$NON-NLS-1$
		createSymbol(">=", SERVOY_COMPARE_OPERATORS); //$NON-NLS-1$
		createSymbol("<=", SERVOY_COMPARE_OPERATORS); //$NON-NLS-1$
		createSymbol("!=", SERVOY_COMPARE_OPERATORS); //$NON-NLS-1$
		createSymbol(">", SERVOY_COMPARE_OPERATORS); //$NON-NLS-1$
		createSymbol("<", SERVOY_COMPARE_OPERATORS); //$NON-NLS-1$
		createSymbol("[", SERVOY_COMPARE_OPERATORS); //$NON-NLS-1$
		createSymbol("]", SERVOY_COMPARE_OPERATORS); //$NON-NLS-1$
		createSymbol("+", SERVOY_OP); //$NON-NLS-1$
		createSymbol("-", SERVOY_OP); //$NON-NLS-1$
		createSymbol("*", SERVOY_OP); //$NON-NLS-1$
		createSymbol("/", SERVOY_OP); //$NON-NLS-1$
		createSymbol("=", SERVOY_OP); //$NON-NLS-1$
		createSymbol("^", SERVOY_OP); //$NON-NLS-1$
		createSymbol("|", SERVOY_OP); //$NON-NLS-1$
		createSymbol("&", SERVOY_OP); //$NON-NLS-1$
		createSymbol("%", SERVOY_OP); //$NON-NLS-1$
		createSymbol("~", SERVOY_OP); //$NON-NLS-1$

		createSymbol("||", SERVOY_COMPARE_OPERATORS); //$NON-NLS-1$
		createSymbol("&&", SERVOY_COMPARE_OPERATORS); //$NON-NLS-1$

		createSymbol("</", SERVOY_COMPARE_OPERATORS); //$NON-NLS-1$ // special support for xml
		createSymbol("/>", SERVOY_COMPARE_OPERATORS); //$NON-NLS-1$ // special support for xml

		rules = new IRule[1];
		rules[0] = new IRule()
		{
			public IToken evaluate(ICharacterScanner scanner)
			{
				int c = scanner.read();
				if (c != ICharacterScanner.EOF)
				{
					String single = Character.toString((char)c);
					c = scanner.read();
					if (c != ICharacterScanner.EOF)
					{
						String dbl = single + (char)c;
						if (dbl.equals("==") || dbl.equals("!=")) //$NON-NLS-1$
						{
							c = scanner.read();
							if (c != ICharacterScanner.EOF)
							{
								IToken token = symbols.get(dbl + (char)c);
								if (token != null) return token;
							}
							scanner.unread();
						}
						IToken token = symbols.get(dbl);
						if (token != null) return token;

					}
					scanner.unread();
					IToken token = symbols.get(single);
					if (token != null) return token;
				}
				scanner.unread();
				return Token.UNDEFINED;
			}
		};
	}


	/**
	 * @see org.eclipse.dltk.javascript.ui.scriptcolor.provider.IScriptColorProvider#getRules()
	 */
	public IRule[] getRules()
	{
		return rules;
	}

	/**
	 * @see org.eclipse.dltk.javascript.ui.scriptcolor.provider.IScriptColorProvider#getKeywords()
	 */
	public String[] getKeywords()
	{
		return keywords.keySet().toArray(new String[keywords.size()]);
	}

	/**
	 * @see org.eclipse.dltk.javascript.ui.scriptcolor.provider.IScriptColorProvider#getToken(java.lang.String)
	 */
	public IToken getToken(String keyword)
	{
		return keywords.get(keyword);
	}


	private TextAttribute createTextAttribute(String keyword)
	{
		String colorKey = getColorKey(keyword);
		RGB rgb = PreferenceConverter.getColor(fPreferenceStore, colorKey);
		if (rgb == null) rgb = new RGB(0, 0, 0);
		Color color = sharedTextColors.getColor(rgb);
		int style = fPreferenceStore.getBoolean(getBoldKey(keyword)) ? SWT.BOLD : SWT.NORMAL;
		if (fPreferenceStore.getBoolean(getItalicKey(keyword))) style |= SWT.ITALIC;

		if (fPreferenceStore.getBoolean(getStrikeThroughKey(keyword))) style |= TextAttribute.STRIKETHROUGH;

		if (fPreferenceStore.getBoolean(getUnderlineKey(keyword))) style |= TextAttribute.UNDERLINE;

		return new TextAttribute(color, null, style);
	}

	/**
	 * @param keyword
	 * @return
	 */
	private String getItalicKey(String keyword)
	{
		return getColorKey(keyword) + ITALIC_KEY;
	}

	/**
	 * @param keyword
	 * @return
	 */
	private String getUnderlineKey(String keyword)
	{
		return getColorKey(keyword) + UNDERLINE_KEY;
	}

	/**
	 * @param keyword
	 * @return
	 */
	private String getStrikeThroughKey(String keyword)
	{
		return getColorKey(keyword) + STRIKETHROUGH_KEY;
	}

	/**
	 * @param keyword
	 * @return
	 */
	private String getBoldKey(String keyword)
	{
		return getColorKey(keyword) + BOLD_KEY;
	}

	/**
	 * @param keyword
	 * @return
	 */
	private String getColorKey(String keyword)
	{
		return getPreferenceKeyPrefix() + keyword + COLOR_KEY;
	}

//	public IColorProviderCategoryItem[] getCategories()
//	{
//		return new IColorProviderCategoryItem[] { servoy_main };
//	}

	public IPreferenceStore getPreferenceStore()
	{
		return this.fPreferenceStore;
	}

	public void save()
	{
		Activator.getDefault().savePluginPreferences();

		createKeywords();
		createRules();
	}

	/**
	 * @see org.eclipse.dltk.javascript.ui.scriptcolor.provider.IScriptColorPreferenceProvider#getPreferenceKeyPrefix()
	 */
	public String getPreferenceKeyPrefix()
	{
		return "_SERVOY_"; //$NON-NLS-1$
	}
}
