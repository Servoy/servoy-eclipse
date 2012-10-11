if (typeof(_ServoyInit_) == "undefined") {
  _ServoyInit_ = {
	init: function() {
			// create forms scope
			forms = { };
			
			// define all the forms
${loop_forms}			Object.defineProperty(forms, "${formName}", {get: function() {
				return _ServoyUtils_.getFormScope("${formName}");
			  }, configurable: false});
${endloop_forms}			
		    // create top level global scope.
			scopes = { };
			
			// define all scopes			
${loop_scopes}			Object.defineProperty(scopes, "${scopeName}", {get: function() {
				return _ServoyUtils_.getGlobalScope("${scopeName}");
			  }, configurable: false});
${endloop_scopes}			
			// support the 'old' notation "globals.xxx" without scopes. (and init the standard globals directly)
			globals = scopes.globals;

			// define all variables on the toplevel, scope and form
			// also include all the servoy scripting scope variables: "foundset","controller","elements","currentcontroller"
			var windowVariablesArray = [${loop_variables}"${variableName}",${endloop_variables}"foundset","controller","elements","currentcontroller"];
			for(var i = 0;i<windowVariablesArray.length;i++) {
				_ServoyUtils_.defineWindowVariable(windowVariablesArray[i]);
			}
		},
		
		forms: {
${loop_forms}		
				_$${formName}$: function(_$form$) {
${loop_functions}					var ${functionName} = _ServoyUtils_.wrapFunction(${functionCode},_$form$);
			        _$form$.${functionName} = ${functionName};
${endloop_functions}					
${loop_variables}			        _ServoyUtils_.defineVariable(_$form$,"${variableName}",${defaultValue},${variableType});
${endloop_variables}
					// define standard things (controller,foundset,elements)
					_ServoyUtils_.defineStandardFormVariables(_$form$);
			     }
${endloop_forms}			     
		},
		
		scopes: {
${loop_scopes}		
				_$${scopeName}$: function(_$scope$) {		
${loop_functions}				var ${functionName} = _ServoyUtils_.wrapFunction( ${functionCode} ,_$scope$);
				_$scope$.${functionName} = ${functionName};
${endloop_functions}				
${loop_variables}				_ServoyUtils_.defineVariable(_$scope$,"${variableName}",${defaultValue},${variableType});
${endloop_variables}				
			};
${endloop_scopes}			
		
		}
	}
}