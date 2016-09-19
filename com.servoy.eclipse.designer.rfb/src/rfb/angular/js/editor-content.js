
var catchedErrors = {
		array: [],
		catchedErrorsDiv: null,
		createErrorTextNodes: function(message, source, lineno, colno, error) {
			 var t = document.createTextNode(message +'\n');
			 catchedErrors.catchedErrorsDiv.appendChild(t);
			 t = document.createTextNode(source+':');
			 catchedErrors.catchedErrorsDiv.appendChild(t);
			 t = document.createTextNode(lineno+':');
			 catchedErrors.catchedErrorsDiv.appendChild(t);
			 t = document.createTextNode(colno+'\n');
			 catchedErrors.catchedErrorsDiv.appendChild(t);
			 t = document.createTextNode(error+'\n');
			 catchedErrors.catchedErrorsDiv.appendChild(t);
		}
}

//this preventDefault should not be needed because we have the glasspane 
//however, the SWT browser on OS X needs this in order to show our editor context menu when right-clicking on the iframe 
window.addEventListener('contextmenu', function (e) { // Not compatible with IE < 9
    e.preventDefault();
}, false);

window.onerror = function(message, source, lineno, colno, error) {
	if (!window.parent.document.body) {
		catchedErrors.array.push({message:message,source:source,lineno:lineno,colno:colno,error:error});
		return;
	}
	if (!catchedErrors.catchedErrorsDiv) {
		var div = document.createElement('div');
		div.style.position = "absolute";
		div.style.overflow = "auto";
		div.style.left = "0px";
		div.style.right = "0px";
		div.style.bottom = "0px";
		div.style.top = "0px";
		window.parent.document.body.appendChild(div);
		catchedErrors.catchedErrorsDiv = document.createElement('pre');
		catchedErrors.catchedErrorsDiv.style.color = "red";
		div.appendChild(catchedErrors.catchedErrorsDiv);
	}
	if (!catchedErrors.textNodesCreated) {
		catchedErrors.textNodesCreated = true;
		for(var i=0;i<catchedErrors.array.length;i++) {
			catchedErrors.createErrorTextNodes(catchedErrors.array[i].message, catchedErrors.array[i].source, catchedErrors.array[i].lineno, catchedErrors.array[i].colno, catchedErrors.array[i].error)
		}
	}
	catchedErrors.createErrorTextNodes(message, source, lineno, colno, error);
	 
	if (typeof(consoleLog) != "undefined") {
		for(var i=0;i<catchedErrors.array.length;i++) {
			consoleLog("onerror",catchedErrors.array[i].message, catchedErrors.array[i].source, catchedErrors.array[i].lineno, catchedErrors.array[i].colno, catchedErrors.array[i].error)
		}
		catchedErrors.array = [];
		consoleLog("onerror",message, source, lineno, colno, error)
	}
	else {
		catchedErrors.array.push({message:message,source:source,lineno:lineno,colno:colno,error:error});
	}
	
}

angular.module('editorContent',['servoyApp'])
 .controller("MainController", function($scope, $window, $timeout, $windowService, $document, $webSocket, $servoyInternal, $sabloApplication, $rootScope, $compile, $solutionSettings, $editorContentService, $element){
  $rootScope.createComponent = function(html, model) {
    if (model) $rootScope.getDesignFormControllerScope().setModel(model.componentName, model);
    var el = $compile(html)($rootScope.getDesignFormControllerScope());
    $rootScope.getDesignFormElement().append(el);
    return el;
  }
     	
  $rootScope.createAbsoluteComponent = function(html, model) {
      var compScope = $scope.$new(true);
      compScope.model = model;
      compScope.api = {};
      compScope.handlers = {};
      var el = $compile(html)(compScope);
      $rootScope.getDesignFormElement().append(el);
      return el;
  }
     	
	//create an absolute position div on the body that holds the element that is being dragged
  $rootScope.createTransportDiv = function(element, event) {
    var dragClone = element.cloneNode(true);
    dragClone.removeAttribute('svy-id');
    var dragCloneDiv = angular.element($document[0].createElement('div'));
    dragCloneDiv.css({
      position: 'absolute',
      width: 200,
      heigth: 100,
      top: event.pageY,
      left: event.pageX,
      'z-index': 4,
      'pointer-events': 'none',
      'list-style-type': 'none',
      display: 'none'
    });
    dragCloneDiv.append(dragClone);
    angular.element($document[0].body).append(dragCloneDiv);
    return dragCloneDiv;
  }
  $rootScope.showWireframe = false;
  $solutionSettings.enableAnchoring = false;
  $scope.solutionSettings = $solutionSettings;
  var realConsole = $window.console;
  $window.console = {
    log: function(msg) {
      if (typeof consoleLog !== 'undefined') {
        consoleLog("log", msg)
      } else if (realConsole) {
        realConsole.log(msg)
      } else alert(msg);

    },
    error: function(msg) {
      if (typeof consoleLog !== 'undefined') {
        consoleLog("error", msg)
      } else if (realConsole) {
        realConsole.error(msg)
      } else alert(msg);
    }
  }

  if (typeof WebSocket === 'undefined' || $webSocket.getURLParameter("replacewebsocket") == 'true') {

    WebSocket = SwtWebSocket;

    function SwtWebSocket(url) {
      var me = this;
      me.id = parent.window.addWebSocket(me);

      function onopenCaller() {
        parent.window.SwtWebsocketBrowserFunction('open', url, me.id)
        me.onopen({isReconnect:false});
      }
      setTimeout(onopenCaller, 0);
    }

    SwtWebSocket.prototype.send = function(str) {
      parent.window.SwtWebsocketBrowserFunction('send', str, this.id)
    }
    SwtWebSocket.prototype.close = function(str) {
     // ignore
    }
  }
  $servoyInternal.connect();
  var formName = $webSocket.getURLParameter("f");
  var solutionName = $webSocket.getURLParameter("s");
  var formModelData = null;
  var formUrl = null;
  $scope.getUrl = function() {
    if (formUrl) return formUrl;
    if ($webSocket.isConnected()) {
      if (formModelData == null) {
        formModelData = {};

        var promise = $sabloApplication.callService("$editor", "getData", {
          form: formName,
          solution: solutionName
        }, false);

        promise.then(function(data) {
          formModelData = JSON.parse(data);
          $editorContentService.formData(false, formModelData);
          formUrl = "designertemplate/" + solutionName + "/" + formName + ".html?";
        });
      } else {
        // this main url is in design (the template must have special markers)
        return formUrl;
      }
    }
  };
}).controller("DesignFormController", function($scope, $editorContentService, $rootScope, $element,$templateCache,$compile,$webSocket) {
  $rootScope.getDesignFormControllerScope = function() {
    return $scope;
  };

  $scope.formStyle = {
    left: "0px",
    right: "0px",
    top: "0px",
    bottom: "0px",
    overflow: "hidden",
    "overflow-x": "hidden",
    "overflow-y": "hidden"
  }
  
  $rootScope.computeHeight = function () {
      return $element[0].scrollHeight;
  } 
  var watches = [];
  
  $scope.originalWatch = $scope.$watch;
  $scope.$watch = function(watchExp, listener, objectEquality, prettyPrintExpression) {
	  var unreg = $scope.originalWatch(watchExp, listener, objectEquality, prettyPrintExpression);
	  watches.push({exp: watchExp, unreg:unreg})
	  return unreg;
  }
  function unRegisterWatch(exp) {
	  for(var i=watches.length;i-->0;) {
		  if (watches[i].exp == exp) {
			  watches[i].unreg();
			  watches.splice(i,1);
			  return;
		  }
	  }
  }

  $scope.removeComponent = function(name) {
	for(var i =$scope.$$watchers.length;i-- >0;) {
		var data = $scope.$$watchers[i].last;
		if (data) {
			if (model[name] == data) unRegisterWatch($scope.$$watchers[i].exp);
			else if (api[name] == data) unRegisterWatch($scope.$$watchers[i].exp);
			else if (handlers[name] == data) unRegisterWatch($scope.$$watchers[i].exp);
			else if (servoyApi[name] == data) unRegisterWatch($scope.$$watchers[i].exp);
			else if (angular.equals(layout[name],data)) unRegisterWatch($scope.$$watchers[i].exp);
		}
	}
    delete model[name];
    delete api[name];
    delete handlers[name];
    delete servoyApi[name];
    delete layout[name];
    delete formData.components[name]
  }

  $rootScope.getDesignFormElement = function() {
      return $element;
  };

  var formData = $editorContentService.formData(true);

  if (formData.parts) {
    for (var name in formData.parts) {
      $scope[name] = JSON.parse(formData.parts[name]);
    }
  }
  var model = {}
  var api = {}
  var handlers = {}
  var servoyApi = {}
  var layout = {}

  $editorContentService.setLayoutData(layout);

  $scope.canContainDraggedElement = function() {
	  if($scope.drop_highlight) {
	  	  var drop = $scope.drop_highlight.split(".");
	  	  if (arguments.length == 2 && arguments[1] instanceof Array)
	  	  {
	  		  return arguments[1].indexOf(drop[1]) >= 0 ;
	  	  }	  
	  	  var allowedChildren = $rootScope.allowedChildren[arguments[0]];
	  	  if (allowedChildren.indexOf(drop[1]) >= 0) return true; //component
	      for (arg in allowedChildren){
			var a = allowedChildren[arg].split(".");
			if(a[0] == drop[0] && ((a[1] == "*") || (a[1] == drop[1]))) return true;
	      }
	  }
      return false;
  }
  
  $scope.model = function(name, noCreate) {
    var ret = model[name];
    if (!ret && !noCreate) {
      if (formData.components[name]) ret = formData.components[name];
      else {
    	  ret = {}
    	  formData.components[name] = ret;
      }
      model[name] = ret;
    }
    return ret;
  }
  
  $scope.setModel = function(name, modelObject){
    if (!model[name]){
      model[name] = modelObject;
    }
    return model[name];
  }
  $scope.api = function(name) {
    var ret = api[name];
    if (!ret) {
      ret = {}
      api[name] = ret;
    }
    return ret;
  }
  $scope.handlers = function(name) {
      var ret = handlers[name];
      if (!ret) {
        ret = {}
        handlers[name] = ret;
      }
      return ret;
    }
    // dummy servoy api, ignore all calls
  var servoyApi = {
    formWillShow: function(formname, relationname, formIndex) {},
    hideForm: function(formname, relationname, formIndex) {
      return null;
    },
    getFormUrl: function(formUrl) {
      return "/designer/formplaceholder.html?formName="+formUrl+"&editingForm="+ $webSocket.getURLParameter("f") ;
    },
    startEdit: function(propertyName) {},
    apply: function(propertyName) {},
    callServerSideApi: function(methodName, args) {
      return null;
    },
    getFormComponentElements: function(propertyName, templateUUID) {
    	return $compile($templateCache.get(templateUUID))($scope);
	},
	isInDesigner: function() {
		return true;
	}
  }
  $scope.servoyApi = function(name) {
    return servoyApi;
  }
  $scope.layout = function(name) {
    var ret = layout[name];
    if (!ret) {
      ret = {}
      if (formData.components[name]) {
        ret.left = formData.components[name].location.x + "px";
        ret.top = formData.components[name].location.y + "px";
        ret.width = formData.components[name].size.width + "px";
        ret.height = formData.components[name].size.height + "px";
      }
      ret.position = 'absolute'
      layout[name] = ret;
    }
    return ret;
  }
}).factory("$editorContentService", function($rootScope, $applicationService, $sabloApplication, $sabloConstants,
  $webSocket, $compile, $sabloConverters, $templateCache, $timeout) {
  var formData = null;
  var layoutData = null;

  function handleTemplate(data) {
	    // append the template
	    var json = JSON.parse(data);
	    if (json.renderGhosts) {
	      renderGhosts();
	      return;
	    }
	    var parentId = json.parentId;
	    if (!parentId) parentId = 'svyDesignForm';

	    var parent = angular.element(document.getElementById(parentId));
	    if (!parent.length) {
	      parent = angular.element(document.querySelectorAll("[svy-id='" + parentId + "']"));
	    }
	    var tpl = $compile(json.template)($rootScope.getDesignFormControllerScope());
	    if (json.insertBeforeUUID) {
//	    	var testSibling = function(counter,insertBeforeUUID)
//			{
//				var sibling = document.querySelectorAll("[svy-id='" + insertBeforeUUID + "']");
//			   	if(sibling[0])
//			   	{
//			   		var nextSibling = angular.element(sibling);
//			   		tpl.insertBefore(nextSibling);
//			   	}
//			   	else if (counter++ < 10)
//			   	{
//			   		$timeout(function() {testSibling(counter, insertBeforeUUID)},100);
//			   	}
//			   	else
//			   	{
//			   		parent.append(tpl);//next sibling is not here yet, append to parent
//			   	}
//			};
//			testSibling(0,json.insertBeforeUUID);
			var sibling = document.querySelectorAll("[svy-id='" + json.insertBeforeUUID + "']");
	    	if(sibling[0])
	    	{
	    		var nextSibling = angular.element(sibling);
	    		tpl.insertBefore(nextSibling);
	    	}
	    	else
	    	{
	    		parent.append(tpl);//next sibling is not here yet, append to parent
	    	}
	    } else
	    {
	    	parent.append(tpl)
	    }
	  }
  
  function updateElementIfParentChange(elementId, updateData, getTemplateParam,forceUpdate) {
    var elementTemplate = angular.element('[svy-id="' + elementId + '"]');
    var shouldGetTemplate = true;
    if(elementTemplate.length) {
    	var domParentUUID = elementTemplate.parent().closest('[svy-id]').attr('svy-id');
    	var currentParentUUID = updateData.childParentMap[elementId].uuid;
    	if(forceUpdate || domParentUUID != currentParentUUID ||
    		((updateData.childParentMap[elementId].index > -1) && (updateData.childParentMap[elementId].index != elementTemplate.index()))) {
    		elementTemplate.remove();
    	}
    	else {
    		shouldGetTemplate = false;
    	}
    }
    
    if (shouldGetTemplate) {
      var promise = $sabloApplication.callService("$editor", "getTemplate", getTemplateParam, false);
      promise.then(handleTemplate)
      return null;
    }
    else {
      return elementTemplate;
    }
  }
  
  return {
    refreshDecorators: function() {
      renderDecorators();
    },
    refreshGhosts: function() {
      renderGhosts();
    },
    updateForm: function(uuid,w, h) {
      updateForm({
    	uuid : uuid,  
        w: w,
        h: h
      });
    },
    formData: function(designControllerReady, data) {
      if (data) {
        formData = data;
      }
      
      if (designControllerReady && formData) {
    	// we need the scope from DesignFormController so we will only be able to do the property conversions ($sabloConverters.convertFromServerToClient) when that controller is available
        function compModelGetter(compName) {
        	return function() { return formData.components[compName]; }
        }
        for (var name in formData.components) {
          var compData = formData.components[name];
          if (compData.conversions) {
        	  formData.components[name] = $sabloConverters.convertFromServerToClient(compData, compData.conversions, undefined, $rootScope.getDesignFormControllerScope(), compModelGetter(name));
          }
        }
        for (var template in formData.formcomponenttemplates) {
        	$templateCache.put(template,formData.formcomponenttemplates[template] )
        }
        if (formData.solutionProperties) {
          $applicationService.setStyleSheets(formData.solutionProperties.styleSheets);
        }
      }
      
      return formData;
    },
    updateStyleSheets : function(stylesheets)
    {
    	$applicationService.setStyleSheets(stylesheets);
    },
    setLayoutData: function(data) {
      layoutData = data;
    },
    updateFormData: function(updates) {
      var data = JSON.parse(updates);
      if (data && (data.components || data.deleted || data.renderGhosts || data.parts || data.containers || data.deletedContainers || data.compAttributes)) {
        // TODO should it be converted??
        $rootScope.$apply(function() {
          function compModelGetter(compName) {
            return function() { return formData.components[compName]; }
          }

          var toDeleteA = [];
          
          if(data.updatedFormComponentsDesignId) {
        	  for (var index in data.updatedFormComponentsDesignId) {
                  for (var name in formData.components) {
                	  if((name.lastIndexOf(data.updatedFormComponentsDesignId[index] + '$', 0) === 0) && (data.formComponentsComponents.indexOf(name) == -1) ) {
                		  toDeleteA.push(name);
                	  }
                  }  
        	  }
          }

          if(data.deleted) {
        	  toDeleteA = toDeleteA.concat(data.deleted);
          }
          
          for (var index in toDeleteA) {
              var toDelete = angular.element('[svy-id="' + toDeleteA[index] + '"]');
              toDelete.remove();
              $rootScope.getDesignFormControllerScope().removeComponent(toDeleteA[index]);
          }          
          
          for (var name in data.components) {
        	var forceUpdate = false;
            var compData = formData.components[name];
            var newCompData = data.components[name];
            if (newCompData.conversions) {
              newCompData = $sabloConverters.convertFromServerToClient(newCompData, newCompData.conversions, compData, $rootScope.getDesignFormControllerScope(), compModelGetter(name))
            }
            if (compData) {
            	
              if(data.updatedFormComponentsDesignId) {
            	  var fixedName = name.replace(/-/g, "_");
            	  if(!isNaN(fixedName[0])) {
            		  fixedName = "_" + fixedName;
            	  }
            	  if((data.updatedFormComponentsDesignId.indexOf(fixedName)) != -1 && (compData.containedForm != newCompData.containedForm)) {
            		  forceUpdate = true;
            	  }
              }
            	
              var modifyFunction = compData[$sabloConstants.modelChangeNotifier];
              var key;
              for (key in compData) {
                if (!newCompData[key]) {
                  compData[key] = null;
                  if (modifyFunction) modifyFunction(key, null)
                }
              }
              // copy it inside so that we update the data inside the model
              for (key in newCompData) {
                compData[key] = newCompData[key];
                if (modifyFunction) modifyFunction(key, newCompData[key])
              }
            } else {
              formData.components[name] = newCompData;
            }

            if (!forceUpdate && data.refreshTemplate)
            {
            	for (var index in data.refreshTemplate) 
            	{
            		if (name == data.refreshTemplate[index])
            		{
            			forceUpdate = true;
            			break;
            		}	
            	}
            }
      
            if((!data.formComponentsComponents || data.formComponentsComponents.indexOf(name) == -1) && data.childParentMap[name] != undefined) {
            	updateElementIfParentChange(name, data, { name: name },forceUpdate);
            }

            var compLayout = layoutData[name];
            if (compLayout) {
              compLayout.left = newCompData.location.x + "px";
              compLayout.top = newCompData.location.y + "px";
              compLayout.width = newCompData.size.width + "px";
              compLayout.height = newCompData.size.height + "px";
            }
          }
          for (var template in data.formcomponenttemplates) {
        	  $templateCache.put(template,data.formcomponenttemplates[template] )
          }

          if (data.renderGhosts) renderGhosts();
          if (data.parts) {
            var scope = $rootScope.getDesignFormControllerScope();
            for (var name in data.parts) {
              scope[name] = JSON.parse(data.parts[name]);
            }
          }
          if (data.compAttributes)
          {
        	  for (var key in data.compAttributes) {
        		  var element = $(document).find("[svy-id='"+key+"']");
        		  if (element[0] && element[0].firstElementChild)
        		  {
        			  var elem = $(element[0].firstElementChild);
        			  for (attribute in data.compAttributes[key]) {
        				  elem.attr(attribute,data.compAttributes[key][attribute]);
        			  }
        		  }
        	  }
          }

          if (data.containers) {
            for(var j = 0; j < data.containers.length; j++) {
              var key = data.containers[j].uuid;
              var element = updateElementIfParentChange(key, data, { layoutId: key },false);
              if(element) {
        		  for (attribute in data.containers[j]) {
        		      if('uuid' === attribute) continue;
        			  if ('class' === attribute)
        			  {
        				  var oldValue = element.attr(attribute);
        				  var newValue = data.containers[j][attribute];
        				  if (oldValue)
        				  {
        					  var oldValuesArray = oldValue.split(" ");
        					  var newValuesArray = newValue.split(" ");
        					  var ngClassValues = [];
        					  var ngClassValue= element.attr('ng-class');
        					  if (ngClassValue)
        					  {
        						  // this is not valid json, how to parse it ?
        						  ngClassValue = ngClassValue.replace("{","").replace("}","").replace(/"/g,"");
        						  ngClassValues = ngClassValue.split(":");
        						  for (var i=0;i< ngClassValues.length-1;i++ )
        						  {
        							  if (ngClassValues[i].indexOf(',') >= 0)
        							  {
        								  ngClassValues[i] = ngClassValues[i].slice(ngClassValues[i].lastIndexOf(',')+1,ngClassValues[i].length);
        							  }	  
        						  }	  
        					  }	  
        					  for (var i=0;i< oldValuesArray.length;i++)
        					  {
        						  if (newValuesArray.indexOf(oldValuesArray[i]) < 0 && ngClassValues.indexOf(oldValuesArray[i]) >= 0)
        						  {
        							  //this value is missing, check if servoy added class
        							  newValue += " " + oldValuesArray[i];
        						  }	  
        					  }	  
        				  }	
        				  element.attr(attribute,newValue); 
        			  }
        			  else
        			  {
        				  element.attr(attribute,data.containers[j][attribute]); 
        			  }	  
                  }
              }
            }
          }          
          
          for (var index in data.deletedContainers) {
            var toDelete = angular.element('[svy-id="' + data.deletedContainers[index] + '"]');
            toDelete.remove();

          }          

          renderDecorators();
        });
      }
      if (data && data.solutionProperties && formData.solutionProperties.styleSheets) {
        $applicationService.setStyleSheets(formData.solutionProperties.styleSheets);
      }
    }

  }
}).factory("loadingIndicator", function() {
  //the loading indicator should not be shown in the editor
  return {
    showLoading: function() {},
    hideLoading: function() {}
  }
});
