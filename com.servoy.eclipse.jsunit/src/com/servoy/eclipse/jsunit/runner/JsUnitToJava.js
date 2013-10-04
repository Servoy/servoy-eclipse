function JsUnitToJava()
{
}

/**
 * Generates an array representing the test name tree (test suite/test hierarchy). If argument is null, returns an empty array.
 * First element is the name of the given test suite. Next element is NEXT_CHILD_GROUP. For each of the elements a list of child elements ended with NEXT_CHILD_GROUP will follow.
 * For example ["1" NEXT_CHILD_GROUP "2" "3" NEXT_CHILD_GROUP "4" NEXT_CHILD_GROUP "5" "6"  NEXT_CHILD_GROUP  NEXT_CHILD_GROUP  NEXT_CHILD_GROUP  NEXT_CHILD_GROUP] will stand for the tree
 *  
 *     "1"
 *  |---+---|
 * "2"     "3"
 *  |     |-+-|
 * "4"   "5" "6"
 */
function JsUnitToJava_getTestTree(testSuite)
{
	var tests = new Array();
	var testTree = new Array();
	if (testSuite != null)
	{
		tests.push(testSuite);
		testTree.push((testSuite.getName && typeof( testSuite.getName ) == "function") ? testSuite.getName() : "test name not found");
		testTree.push(JsUnitToJava.prototype.NEXT_CHILD_GROUP);
		
		var parentElement = 0;

		while (parentElement < tests.length)
		{
			var parent = tests[parentElement];
			if (parent instanceof TestSuite)
			{
				for (var i = 0; i < parent.mTests.length; i++)
				{
					tests.push(parent.mTests[i]);
					testTree.push((parent.mTests[i].getName && typeof( parent.mTests[i].getName ) == "function") ? parent.mTests[i].getName() : "test name not found");
				}
			}
			testTree.push(JsUnitToJava.prototype.NEXT_CHILD_GROUP);
			parentElement++;
		}
	}
	return testTree;
}

JsUnitToJava.prototype.getTestTree = JsUnitToJava_getTestTree;
JsUnitToJava.prototype.NEXT_CHILD_GROUP = null;
JsUnitToJava.prototype.ASSERTION_EXCEPTION_MESSAGE = "just for stack";

if (typeof(Packages) != 'undefined')
{
	// if we are running under Rhino, get the stack trace using the JS engine
	
	// some browsers (opera/chrome/ie) - when the code is evaled in window scope - will define these functions anyway
	// even if the initial check for "Packages" wouldn't pass (they define all function declarations before running the JS condition); FF doesn't do that, but still;
	// without this ___defineTypes the main scope would still see/be overridden by these wrapped functions even if the if condition above is false
	function ___defineTypes(scope)
    {
		scope.RhinoStackError = function RhinoStackError()
		{
			this.rhinoException = new Packages.org.mozilla.javascript.EvaluatorException(JsUnitToJava.prototype.ASSERTION_EXCEPTION_MESSAGE);

			// Error.apply(this, arguments); // doesn't do what we want, ignores "this"
			var er = new Error();
			if (typeof (er.stack) != 'undefined' && er.stack != null) this.stack = er.stack; // only useful for non-IE, IE works nicely without it, actually it won't work with it
		}
	
		scope.JsUnitError = function JsUnitError(msg)
		{
			RhinoStackError.call(this);
			this.message = msg || "";
		}
    }
	___defineTypes(this);
	delete ___defineTypes;
	
	RhinoStackError.prototype = new Error();
	RhinoStackError.prototype.constructor = RhinoStackError;
	JsUnitError.prototype = new RhinoStackError();
	JsUnitError.prototype.constructor = JsUnitError;
}
else
{
	// else try to get the trace from native browser Error objects
	
	// see comment for the "___defineTypes" above
	function ___defineTypes(scope)
    {
		scope.JsUnitError = function JsUnitError(msg)
		{
			// Error.apply(this, arguments); // doesn't do what we want, ignores "this"
			var er = new Error(msg);
			if (typeof (er.stack) != 'undefined' && er.stack != null) this.stack = er.stack; // only useful for non-IE, IE works nicely without it, actually it won't work with it
			this.message = msg || "";
		}
    }
	___defineTypes(this);
	delete ___defineTypes;
	
	JsUnitError.prototype.constructor = JsUnitError;
}

function TestSuite_addTest( test ) 
{ 
    this.mTests.push( test ); 
}
TestSuite.prototype.addTest = TestSuite_addTest;