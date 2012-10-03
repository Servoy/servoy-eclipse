if (typeof(_ServoyInit_) == "undefined") {
  _ServoyInit_ = {
	init: function() {
			// create forms scope
			forms = { };
			
			// define all the forms
${loop_forms}			Object.defineProperty(forms, "${formName}", {get: function() {
				var _$form$ = _ServoyUtils_.getFormScope("${formName}");
				if (!_$form$._$initialized$) {
${loop_functions}					var ${functionName} = _ServoyUtils_.wrapFunction(${functionCode},_$form$);
			        _$form$.${functionName} = ${functionName};
${endloop_functions}					
${loop_variables}			        _ServoyUtils_.defineVariable(_$form$,"${variableName}",${defaultValue},${variableType});
${endloop_variables}
					// define standard things (controller,foundset,elements)
					_ServoyUtils_.defineStandardFormVariables(_$form$);
										
			        _$form$._$initialized$ = true;
			     }
			     return _$form$;
			  }, configurable: false});
${endloop_forms}			
		    // create top level global scope.
			scopes = { };
			
${loop_scopes}			var _$createScope$ = function() {
				scopes.${scopeName} = _ServoyUtils_.getGlobalScope("${scopeName}");
				
${loop_functions}				var ${functionName} = _ServoyUtils_.wrapFunction( ${functionCode} ,scopes.${scopeName});
				scopes.${scopeName}.${functionName} = ${functionName};
${endloop_functions}				
${loop_variables}				_ServoyUtils_.defineVariable(scopes.${scopeName},"${variableName}",${defaultValue},${variableType});
${endloop_variables}				
				return 
			};
			_$createScope$();
${endloop_scopes}			
			// support the 'old' notation "globals.xxx" without scopes.
			globals = scopes.globals;

			// define all variables on the toplevel, scope and form
			// also include all the servoy scripting scope variables: "foundset","controller","elements","currentcontroller"
			var windowVariablesArray = [${loop_variables}"${variableName}",${endloop_variables}"foundset","controller","elements","currentcontroller"];
			for(var i = 0;i<windowVariablesArray.length;i++) {
				_ServoyUtils_.defineWindowVariable(windowVariablesArray[i]);
			}
		}	
	}
}