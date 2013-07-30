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
				_ServoyUtils_.defineWindowVariable(windowVariablesArray[i],true);
			}
		},
		
		forms: {${loop_forms}
			${formName} : {
				_sv_init: function(_$form$, _$oldScope$, _$tmpScope$) {
					var history = _ServoyUtils_.history;
${loop_functions}					if (!this._sv_fncs[${functionName}])
					{
						var ${functionName} = _ServoyUtils_.wrapFunction(${functionCode},_$form$);
						_$form$.${functionName} = ${functionName};
					}
${endloop_functions}
					if (!_$tmpScope$) {
						for (var key in this._sv_fncs) {
							_$form$[key] = _ServoyUtils_.wrapFunction(eval("(" + _ServoyInit_.getFunctionStart("forms", "${formName}", key) + this._sv_fncs[key] + ")"), _$form$);
							eval("var " + key + " = _$form$[key];");
						}
						for (var key in this._sv_vrbs) {
							var val = this._sv_vrbs[key];
						   _ServoyUtils_.defineVariable(_$form$, key, _$oldScope$ ? _$oldScope$[key] : eval("(" + val[0] + ")"), val[1]);
						}
					}
				},
				_sv_fncs: {
				},
				_sv_vrbs: {
${loop_variables}					${variableName} : [${defaultValue}, ${variableType}]${endloop_variables}
				}
			}${endloop_forms}
		},
		
		scopes: {${loop_scopes}
			${scopeName} : {
				_sv_init: function(_$scope$,_$oldScope$, _$tmpScope$) {
					var history = _ServoyUtils_.history;
${loop_functions}					if (!this._sv_fncs[${functionName}])
					{
						var ${functionName} = _ServoyUtils_.wrapFunction( ${functionCode} ,_$scope$);
						_$scope$.${functionName} = ${functionName};
					}
${endloop_functions}
					if (!_$tmpScope$) {
						for (var key in this._sv_fncs) {
							_$scope$[key] = _ServoyUtils_.wrapFunction(eval("(" + _ServoyInit_.getFunctionStart("globals", "${scopeName}", key) + this._sv_fncs[key] + ")"), _$scope$);
							eval("var " + key + " = _$scope$[key];");
						}				
						for (var key in this._sv_vrbs) {
							var val = this._sv_vrbs[key];
						   _ServoyUtils_.defineVariable(_$scope$, key, _$oldScope$ ? _$oldScope$[key] : eval("(" + val[0] + ")"), val[1]);
						}
					}
				},
				_sv_fncs: {
				},
				_sv_vrbs: {
${loop_variables}					${variableName} : [${defaultValue}, ${variableType}]${endloop_variables}
				}
			}${endloop_scopes}
		},
		// used for all solution model forms.
		initScope : function (containerName, subscope, scopeToInit, oldScope) {
			var subs = this[containerName][subscope];
			var history = _ServoyUtils_.history;
			var fncs = subs._sv_fncs;
			for (var key in fncs) {
				scopeToInit[key] = _ServoyUtils_.wrapFunction(eval("(" + this.getFunctionStart(containerName, subscope, key) + fncs[key] + ")"), scopeToInit);
				eval("var " + key + " = scopeToInit[key];");
			}

			var vrbs = subs._sv_vrbs;
			for (var key in vrbs) {
				var val = vrbs[key];
			   _ServoyUtils_.defineVariable(scopeToInit, key, oldScope ? oldScope[key] : eval("(" + val[0] + ")"), val[1]);
			}
		},
		
		getFunctionStart : function (s1Name, s2Name, fName) {
			return "function ";
		}
		
	}
}