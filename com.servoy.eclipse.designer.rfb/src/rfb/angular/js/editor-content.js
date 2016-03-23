
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
          $editorContentService.formData(formModelData);
          formUrl = "designertemplate/" + solutionName + "/" + formName + ".html?";
        });
      } else {
        // this main url is in design (the template must have special markers)
        return formUrl;
      }
    }
  };
}).controller("DesignFormController", function($scope, $editorContentService, $rootScope, $element) {
  $scope.formStyle = {
    left: "0px",
    right: "0px",
    top: "0px",
    bottom: "0px",
    overflow: "hidden"
  }

  $scope.removeComponent = function(name) {
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
  $rootScope.getDesignFormControllerScope = function() {
    return $scope;
  };

  var formData = $editorContentService.formData();

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
      for (arg in arguments){
	  if (arguments[arg] == $scope.drop_highlight) return true;
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
      return null;
    },
    startEdit: function(propertyName) {},
    apply: function(propertyName) {},
    callServerSideApi: function(methodName, args) {
      return null;
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
  $webSocket, $compile, $sabloConverters) {
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
      var nextSibling = angular.element(document.querySelectorAll("[svy-id='" + json.insertBeforeUUID + "']"));
      tpl.insertBefore(nextSibling);
    } else
      parent.append(tpl)
  }
  return {
    refreshDecorators: function() {
      renderDecorators();
    },
    refreshGhosts: function() {
      renderGhosts();
    },
    updateForm: function(name, uuid, w, h) {
      updateForm({
        name: name,
        uuid: uuid,
        w: w,
        h: h
      });
    },
    formData: function(data) {
      if (data) {
        formData = data;
        for (var name in data.components) {
          var compData = data.components[name];
          if (compData.conversions) {
            data.components[name] = $sabloConverters.convertFromServerToClient(compData, compData.conversions, undefined, undefined, undefined)
          }
        }
        if (formData.solutionProperties) {
          $applicationService.setStyleSheets(formData.solutionProperties.styleSheets);
        }
      } else return formData;
    },
    setLayoutData: function(data) {
      layoutData = data;
    },
    updateFormData: function(updates) {
      var data = JSON.parse(updates);
      if (data && (data.components || data.deleted || data.renderGhosts || data.parts || data.containers || data.deletedContainers)) {
        // TODO should it be converted??
        $rootScope.$apply(function() {

          for (var name in data.components) {
            var compData = formData.components[name];
            var newCompData = data.components[name];
            if (newCompData.conversions) {
              newCompData = $sabloConverters.convertFromServerToClient(newCompData, newCompData.conversions, compData, undefined, undefined)
            }
            if (compData) {
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
           
            var elementTemplate = angular.element('[svy-id="' + name + '"]');
            var shouldGetTemplate = true;
            if(elementTemplate.length) {
            	var domParentUUID = elementTemplate.parent('[svy-id]').attr('svy-id');
            	var currentParentUUID = data.childParentMap[name].uuid;
            	if(domParentUUID != currentParentUUID ||
            		((data.childParentMap[name].index > -1) && (data.childParentMap[name].index != elementTemplate.index()))) {
            		elementTemplate.remove();
            	}
            	else {
            		shouldGetTemplate = false;
            	}
            }
            
            if (shouldGetTemplate) {
              var promise = $sabloApplication.callService("$editor", "getTemplate", {
                name: name
              }, false);
              promise.then(handleTemplate)

            }
            var compLayout = layoutData[name];
            if (compLayout) {
              compLayout.left = newCompData.location.x + "px";
              compLayout.top = newCompData.location.y + "px";
              compLayout.width = newCompData.size.width + "px";
              compLayout.height = newCompData.size.height + "px";
            }
          }
          for (var index in data.deleted) {
            var toDelete = angular.element('[svy-id="' + data.deleted[index] + '"]');
            toDelete.remove();
            $rootScope.getDesignFormControllerScope().removeComponent(data.deleted[index]);
          }
          if (data.renderGhosts) renderGhosts();
          if (data.parts) {
            var scope = $rootScope.getDesignFormControllerScope();
            for (var name in data.parts) {
              scope[name] = JSON.parse(data.parts[name]);
            }
          }
          if (data.containers) {
            for (var key in data.containers) {
              var element = angular.element(document.querySelectorAll("[svy-id='" + key + "']"));
              var shouldGetTemplate = true;
              if(element.length) {
            	  var domParentUUID = element.parent('[svy-id]').attr('svy-id');
            	  var currentParentUUID = data.childParentMap[key].uuid;
            	  if(domParentUUID != currentParentUUID ||
            	  	((data.childParentMap[key].index > -1) && (data.childParentMap[key].index != element.index()))) {
            		  element.remove();
            	  }
            	  else {
            		  shouldGetTemplate = false;
            	  }
              }              

              if (shouldGetTemplate) {
                var promise = $sabloApplication.callService("$editor", "getTemplate", {
                  layoutId: key
                }, false);
                promise.then(handleTemplate)
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
}).directive("createscope", function() {
	return {
		restrict: 'A',
		scope: true,
		link: function($scope, $element) {
			  $element.on('$destroy', function(){
				  $scope.$destroy();
		      })
		}
	}
});
