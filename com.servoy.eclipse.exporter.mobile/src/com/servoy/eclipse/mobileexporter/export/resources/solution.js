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
${loop_forms}			${formName} : {
				fncs: {
${loop_functions}					${functionName} : ${functionCode}${endloop_functions}
				},
				vrbs: {
${loop_variables}					${variableName} : ["${defaultValue}", ${variableType}]${endloop_variables}
				}
			}${endloop_forms}			     
		},
		
		scopes: {
${loop_scopes}			${scopeName} : {
				fncs: {
${loop_functions}					 ${functionName} : ${functionCode}${endloop_functions}
				},
				vrbs: {
${loop_variables}					${variableName} : ["${defaultValue}",${variableType}]${endloop_variables}
				}
			}${endloop_scopes}			
		},
		
		initScope : function (containerName, subscope, scopeObject) {
			var subs = this[containerName][subscope];

			var fncs = subs.fncs;
			for (var key in fncs) {
			   scopeObject[key] = _ServoyUtils_.wrapFunction(fncs[key], scopeObject);
			}

			var vrbs = subs.vrbs;
			for (var key in vrbs) {
				var val = vrbs[key];
			   _ServoyUtils_.defineVariable(scopeObject, key, eval("(" + val[0] + ")"), val[1]);
			}
		}
	}
}