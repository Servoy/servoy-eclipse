function AssertionFailedErrorTest( name )
{
    TestCase.call( this, name );
}
function AssertionFailedErrorTest_testToString()
{
    var afe = new AssertionFailedError( "The Message", null );
    this.assertEquals( "AssertionFailedError: The Message", afe );
    bla(this);
}
AssertionFailedErrorTest.prototype = new TestCase();
AssertionFailedErrorTest.glue(this);

function bla(th)
{
	th.assertTrue("Bla", false);
}

function TestFailureTest( name )
{
    TestCase.call( this, name );
}
function TestFailureTest_testExceptionMessage()
{
    var ft = new TestFailure( this.mTest, this.mException );
    //throw new Error();
    ha.p;
    this.assertEquals( "AssertionFailedError: Message", ft.exceptionMessage());
}
function TestFailureTest_testFailedTest()
{
    var ft = new TestFailure( this.mTest, this.mException );
    this.assertEquals( "testFunction", ft.failedTest());
}
function TestFailureTest_testIsFailure()
{
    var ft = new TestFailure( this.mTest, this.mException );
    this.assertTrue( ft.isFailure());
    ft = new TestFailure( this.mTest, new Error( "Error" ));
    this.assertFalse( ft.isFailure());
}
function TestFailureTest_testThrownException()
{
    var ft = new TestFailure( this.mTest, this.mException );
    this.assertEquals( this.mException, ft.thrownException());
}
function TestFailureTest_testToString()
{
    var ft = new TestFailure( this.mTest, this.mException );
    this.assertEquals( 
        "Test testFunction failed: AssertionFailedError: Message", ft );
}
function TestFailureTest_testTrace()
{
    var ft = new TestFailure( this.mTest, 
        new AssertionFailedError( "Message", "Trace" ));
    this.assertEquals( "Trace", ft.trace());
}
TestFailureTest.prototype = new TestCase();
TestFailureTest.glue(this);
TestFailureTest.prototype.mException = new AssertionFailedError( "Message", null );
TestFailureTest.prototype.mTest = "testFunction";

function JsUnitTestSuite()
{
    TestSuite.call( this, "JsUnitTestSuite" );
    this.addTestSuite( AssertionFailedErrorTest );
    this.addTestSuite( TestFailureTest );
}
JsUnitTestSuite.prototype = new TestSuite();
JsUnitTestSuite.prototype.suite = function (){ return new JsUnitTestSuite(); }
