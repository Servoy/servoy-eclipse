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
package com.servoy.eclipse.core.scripting.docs;

import com.servoy.j2db.annotations.ServoyDocumented;

@ServoyDocumented(category = ServoyDocumented.JSLIB, publicName = "RegExp", scriptingName = "RegExp")
public class RegExp
{
	/**
	 * Specifies if the "g" modifier is set.
	 *
	 * @sample
	 * var str = 'Visit www.servoy.com';	
	 * var patt1 = new RegExp('www');	
	 * application.output(patt1.global);
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/RegExp/global Mozilla Developer Center documentation for global function
	 */
	public Boolean global;

	/**
	 * Specifies if the "i" modifier is set.
	 *
	 * @sample
	 * var str = 'Visit www.servoy.com';	
	 * var patt1 = new RegExp('www');	
	 * application.output(patt1.ignoreCase);
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/RegExp/ignoreCase
	 */
	public Boolean ignoreCase;

	/**
	 * The string on which the pattern match is performed.
	 *
	 * @sample
	 * var patt1 = new RegExp('www');	
	 * var str = 'visit www.servoy.com';	
	 * patt1.test(str);	
	 * application.output(RegExp.input);
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Deprecated_Features
	 */
	@Deprecated
	public String input;

	/**
	 * An integer specifying the index at which to start the next match.
	 *
	 * @sample
	 * var str = 'The rain in Spain stays mainly in the plain';	
	 * var patt1 = new RegExp('ain', 'g');	
	 * patt1.test(str);	
	 * application.output('Match found. index now at: ' + patt1.lastIndex);
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/RegExp/lastIndex
	 */
	public Number lastIndex;

	/**
	 * The last matched characters.
	 *
	 * @sample
	 * var str = 'The rain in Spain stays mainly in the plain';	
	 * var patt1 = new RegExp('ain');	
	 * patt1.test(str);	
	 * application.output('Match found: ' + RegExp.lastMatch);
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Deprecated_Features
	 */
	@Deprecated
	public String lastMatch;

	/**
	 * The last matched parenthesized substring.
	 *
	 * @sample
	 * var str = 'Visit www.servoy.com (now)';	
	 * var patt1 = new RegExp('(now)', 'g');	
	 * patt1.test(str);	
	 * application.output('Last parenthesized substring is: ' + RegExp.lastParen);
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Deprecated_Features
	 */
	@Deprecated
	public String lastParen;

	/**
	 * The substring in front of the characters most recently matched.
	 *
	 * @sample
	 * var str = 'The rain in Spain stays mainly in the plain';	
	 * var patt1 = new RegExp('ain');	
	 * patt1.test(str);	
	 * application.output('Text before match: ' + RegExp.leftContext);
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Deprecated_Features
	 */
	@Deprecated
	public String leftContext;

	/**
	 * Specifies if the "m" modifier is set.
	 *
	 * @sample
	 * var str = 'Visit www.servoy.com';	
	 * var patt1 = new RegExp('www','m');	
	 * application.output(patt1.multiline);
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/RegExp/multiline
	 */
	public Boolean multiline;

	/**
	 * The substring after the characters most recently matched
	 *
	 * @sample
	 * var str = 'The rain in Spain stays mainly in the plain';	
	 * var patt1 = new RegExp('ain');	
	 * patt1.test(str);	
	 * application.output('Text after match: ' + RegExp.rightContext);
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Deprecated_Features
	 */
	@Deprecated
	public String rightContext;

	/**
	 * The text used for pattern matching.
	 *
	 * @sample
	 * var str = 'Visit www.servoy.com';	
	 * var patt1 = new RegExp('www.','g');	
	 * application.output('The regular expression is: ' + patt1.source);
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/RegExp/source
	 */
	public String source;

	/**
	 * Change the regular expression.
	 *
	 * @sample
	 * var str='Visit www.servoy.com'; 
	 * var patt=new RegExp('soft'); 
	 * application.output(patt.test(str)==true);
	 * patt.compile('servoy');	
	 * application.output(patt.test(str)==true);
	 *
	 * @param regexp
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/RegExp/source
	 */
	@Deprecated
	public void compile(Object regexp)
	{
	}

	/**
	 * Search a string for a specified value. Returns the found value and remembers the position.
	 *
	 * @sample
	 * var str='Visit www.servoy.com';
	 * var patt=new RegExp('servoy');
	 * application.output(patt.exec(str));
	 *
	 * @param string
	 * 
	 * @return A String representing the found value.
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/RegExp/exec
	 */
	public String exec(Object string)
	{
		return null;
	}

	/**
	 * Search a string for a specified value. Returns true or false.
	 *
	 * @sample
	 * var str='Visit www.servoy.com';	
	 * var patt=new RegExp('soft'); 
	 * application.output(patt.test(str)==true);	
	 * patt.compile('servoy');	
	 * application.output(patt.test(str)==true)
	 *
	 * @param string 
	 * 
	 * @return true if a match was found in the string. false otherwise.
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/RegExp/test
	 */
	public Boolean test(Object string)
	{
		return null;
	}
}
