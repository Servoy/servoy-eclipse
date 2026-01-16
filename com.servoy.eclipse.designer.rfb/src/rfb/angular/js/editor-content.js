
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
		div.id = "errorsDiv"
		div.style.position = "absolute";
		div.style.overflow = "auto";
		div.style.left = "0px";
		//div.style.right = "0px";
		div.style.height = "300px";
		div.style.top = "35px";
		div.style.zIndex = "10000";
		div.style.display = 'none';
		window.parent.document.body.appendChild(div);
		catchedErrors.catchedErrorsDiv = document.createElement('pre');
		catchedErrors.catchedErrorsDiv.style.color = "red";
		div.appendChild(catchedErrors.catchedErrorsDiv);
		var close = document.createElement('a');
		close.id = "closeErrors"
		close.style.float="right";
		close.href="#";
		close.innerHTML="x"
		catchedErrors.catchedErrorsDiv.appendChild(close);
		catchedErrors.catchedErrorsDiv.appendChild(document.createElement('br'));
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
 .controller("MainController", function($scope, $window, $timeout, $windowService, $document, $webSocket, $servoyInternal,
                $sabloApplication, $rootScope, $compile, $solutionSettings, $editorContentService, $element, $typesRegistry, $pushToServerUtils) {
                    
  $typesRegistry.disablePushToServerWatchesReceivedFromServer(); // we don't want watches to try to send client side changes to server; we are in designer; we only want to show things
                    
  $rootScope.createComponent = function(html, model) {
    if (model) $rootScope.getDesignFormControllerScope().setModel(model.componentName, model);
    var el = $compile(html)($rootScope.getDesignFormControllerScope());
    $rootScope.getDesignFormElement().append(el);
    return el;
  }

	var getFormHeight = function() {
		var children = $document.querySelectorAll('#svyDesignForm').children();
		var height = 0;
		for (var i = 0; i < children.length; i++) {
			height += children[i].scrollHeight;
		}
		return height;
	}
	var toCheckSize = undefined;
	var adjustFormSize = function(newValue){	
			//we want to adjust the content sizes if the form height changes
			//there is no way to find out when all the css changes are applied
			//so we check it a few times, until it becomes stable
			
			if (toCheckSize !== undefined)
			{
				clearTimeout(toCheckSize);
			}
			var height = getFormHeight();
			var timesChecked = 0;
			var checkSize = function() { 
				if (timesChecked < 5) {
					if (getFormHeight() !== height) {
						$rootScope.$broadcast("ADJUST_SIZE");
						height = getFormHeight();
						timesChecked = 0;
					}
					timesChecked++;
					toCheckSize = setTimeout(checkSize, 1000);
				}
			};
			toCheckSize = setTimeout(checkSize, 500);
	};
	
	//trigger content loaded to set content sizes
	$scope.$on('content-loaded:ready', function() {	
		if (!$rootScope.getDesignFormControllerScope().absoluteLayout) {
			$rootScope.$broadcast("ADJUST_SIZE");
	
			//register the watchers only when the form is loaded because we need the form div to be present
			$rootScope.$watch('showWireframe', adjustFormSize);
			$rootScope.$watch('showSolutionLayoutsCss', adjustFormSize);
			$rootScope.$watch('showSolutionCss', adjustFormSize);
			$rootScope.$watch('maxLevel', adjustFormSize);
			
			$scope.$on('UPDATE_FORM_DATA', function() {	
				adjustFormSize();
			});
		}
	});
  
  //this preventDefault should not be needed because we have the glasspane 
  //however, the SWT browser on OS X needs this in order to show our editor context menu when right-clicking on the iframe 
  window.addEventListener('contextmenu', function (e) { // Not compatible with IE < 9
    e.preventDefault();
    $rootScope.ctxmenu(e);//workaround to make the contextmenu show on osx
  }, false);
     	
  $rootScope.createAbsoluteComponent = function(html, model) {
      var compScope = $scope.$new(true);
      compScope.model = model;
      compScope.api = {};
      compScope.handlers = {};
      compScope.svy_servoyApi = $rootScope.servoyApi;
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
    
    var highlightEl = dragClone;
	if (highlightEl.clientWidth == 0 && highlightEl.clientHeight == 0 && highlightEl.firstElementChild) highlightEl = highlightEl.firstElementChild;
    $(highlightEl).addClass('highlight_element');
    
    dragCloneDiv.append(dragClone);
    angular.element($document[0].body).append(dragCloneDiv);
    return dragCloneDiv;
  }
  $rootScope.showWireframe = false;
  $rootScope.showSolutionLayoutsCss = true;
  $rootScope.showSolutionCss = true;
  $rootScope.maxLevel = 3;
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
  var containerId = $webSocket.getURLParameter("cont");
  var formModelData = null;
  var formUrl = null;
  $rootScope.flushMain = function()
  {
	  formModelData = null;
	  formUrl = null;  
  };
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
          formUrl = "designertemplate/" + solutionName + "/" + formName + ".html?" + (containerId ? ("cont="+containerId):"") ;
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
  
  if(!formData.components)
	  formData.components = {};
  
  if (formData.formProperties && formData.formProperties.absoluteLayout) {
	  $scope.absoluteLayout = formData.formProperties.absoluteLayout[''];
  }
  $rootScope.addBottom = $scope.absoluteLayout || /MSIE\//.test(window.navigator.userAgent) || /Trident\//.test(window.navigator.userAgent);

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
	  		  return arguments[1].indexOf(drop[1]) >= 0 && arguments[0] == drop[0];
	  	  }	  
	  	  var allowedChildren = $rootScope.allowedChildren[arguments[0]];
	  	  if (allowedChildren && allowedChildren.indexOf(drop[1]) >= 0) return true; //component
	      for (arg in allowedChildren){
			var a = allowedChildren[arg].split(".");
			if(a[0] == drop[0] && ((a[1] == "*") || (a[1] == drop[1]))) return true;
	      }
	  }
      return false;
  }
  
  $scope.highlightElement = function(elementId) {
	  var designHighlightOn = $rootScope.design_highlight == "highlight_element";
	  if(!designHighlightOn && $rootScope.drag_highlight && $rootScope.drag_highlight.length) {
		  for(var i = 0; i < $rootScope.drag_highlight.length; i++) {
			  var id = $($rootScope.drag_highlight[i]).attr("svy-id");
			  if(elementId == id) {
				  designHighlightOn = true;
				  break;
			  }
		  }
	  }
	  return designHighlightOn;
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
  $rootScope.servoyApi = {
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
    /**
     * @param _propertyName deprecated; not used
     */
    getFormComponentElements: function(_propertyName, formComponentValue) {
    	return $compile($templateCache.get(formComponentValue.uuid))($scope);
	},
	isInDesigner: function() {
		return true;
	},
	trustAsHtml: function() {
		return false;
	},
	isInAbsoluteLayout: function(){
		return $scope.absoluteLayout;
	}
  }
  $scope.servoyApi = function(name) {
    return $rootScope.servoyApi;
  }
  $scope.layout = function(name) {
    var ret = layout[name];
    if (!ret) {
      ret = {}
      if (formData.components[name]) {
    	  if (formData.formProperties['useCssPosition'][name])
    	  {
    		  $editorContentService.setCSSPositionProperties(ret,formData.components[name]['cssPosition']);
    	  }
    	  else
    	  {
    		  ret.left = formData.components[name].location.x + "px";
    		  ret.top = formData.components[name].location.y + "px";
    		  ret.width = formData.components[name].size.width + "px";
    		  ret.height = formData.components[name].size.height + "px";
    	  }
      }
      ret.position = 'absolute'
      layout[name] = ret;
    }
    return ret;
  }
  $scope.flushDesign = function()
  {
	  delete formData;
	  delete model;
      delete api;
      delete handlers;
      delete servoyApi;
      delete layout;
  }
}).factory("$editorContentService", function($rootScope, $applicationService, $sabloApplication, $sabloConstants,
  $webSocket, $compile, $sabloConverters, $templateCache, $timeout, $typesRegistry, $pushToServerUtils) {
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
	    if (!parent.length) {
	    	// element is the showedContainer and its parent is missing
	    	 parent = angular.element(document.getElementById('svyDesignForm'));
	    }
	    var tpl = $compile(json.template)($rootScope.getDesignFormControllerScope());
	    var old_element = document.querySelectorAll("[svy-id='" + tpl[0].getAttribute("svy-id") + "']");
		if (old_element.length == 1) return; //when it's already there, we don't do anything (this happens when the parent is overridden)
		
		if (parent.children().length > 0) {
			// we have to calculate insert point based on ordered location, just like on server side
			var inserted = false;
			var nodeLocation = parseInt((formData.formProperties.absoluteLayout[''] && tpl.children(0)) ? tpl.children(0).attr('form-index') : tpl.attr('svy-location'));
			for (var i=0;i<parent.children().length;i++)
			{
				//skip parts
				if(formData.formProperties.absoluteLayout[''] && parent.children()[i].children.length == 0) continue;
				var currentLocation = parseInt((formData.formProperties.absoluteLayout[''] && parent.children()[i].children[0]) ? parent.children()[i].children[0].getAttribute('form-index') : parent.children()[i].getAttribute('svy-location'));
				if (nodeLocation < currentLocation)
				{
					tpl.insertBefore(parent.children()[i]);
					inserted = true;
					break;
				}
			}
			if (!inserted)
			{	
				parent.append(tpl);
			}
		}
		else
		{
			parent.append(tpl)
		}
  }
	   
  function updateElementIfParentChange(elementId, updateData, getTemplateParam,forceUpdate,elementsToRemove) {
    var elementTemplate = angular.element('[svy-id="' + elementId + '"]');
    var shouldGetTemplate = true;
    if(elementTemplate.length) {
    	var domParentUUID = elementTemplate.parent().closest('[svy-id]').attr('svy-id');
    	var currentParentUUID = updateData.childParentMap[elementId].uuid;
    	if(forceUpdate || domParentUUID != currentParentUUID) {
    		elementsToRemove.push(elementTemplate);
    	}
    	else if (updateData.childParentMap[elementId].location > -1 && elementTemplate.attr('svy-location') && updateData.childParentMap[elementId].location != parseInt(elementTemplate.attr('svy-location'))){
    		// location(order) is changed, we need to reinsert this node
    		elementsToRemove.push(elementTemplate);
    	}
    	else if (updateData.childParentMap[elementId].formIndex > -1 && elementTemplate.children(0) && updateData.childParentMap[elementId].formIndex != parseInt(elementTemplate.children(0).attr('form-index'))){
    		// location(order) is changed, we need to reinsert this node
    		elementsToRemove.push(elementTemplate);
    	}
    	else {
    		shouldGetTemplate = false;
    	}
    }
    
    if (shouldGetTemplate) {
        if (updateData.childParentMap[updateData.childParentMap[elementId].uuid] ===  undefined) {//we don't want get the template if we also get the template of the parent
            var promise = $sabloApplication.callService("$editor", "getTemplate", getTemplateParam, false);
            promise.then(handleTemplate)
        }
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
    updateForm: function(uuid,parentUuid,w, h) {
    	if (formData.parentUuid !== parentUuid)
    	{
    		this.contentRefresh();
    	}
    	updateForm({
    		uuid : uuid,  
    		w: w,
    		h: h
    	});
    },
    contentRefresh: function()
    {
    	formData.components = null;
    	formData.parts = null;
    	$rootScope.getDesignFormControllerScope().flushDesign();
    	$rootScope.flushMain();
    	$templateCache.removeAll();
    	$rootScope.$digest();
    },
    formData: function(designControllerReady, data) {
      if (data) {
        formData = data;
      }
      
      if (designControllerReady && formData) {
        if (formData.componentSpecs) {
            $typesRegistry.addComponentClientSideSpecs(formData.componentSpecs);
        }

    	// we need the scope from DesignFormController so we will only be able to do the property conversions ($sabloConverters.convertFromServerToClient) when that controller is available
        for (var name in formData.components) {
          var componentSpecification = $typesRegistry.getComponentSpecification(formData.componentSpecNames[name]);
          var propertyContextCreator = $pushToServerUtils.newRootPropertyContextCreator(function(propertyName) { return formData.components[name][propertyName]; }, componentSpecification);
          
          var serverDataForComponent = formData.components[name];
          formData.components[name] = {};
          for (var propName in serverDataForComponent) {
        	  formData.components[name][propName] = $sabloConverters.convertFromServerToClient(serverDataForComponent[propName],
        	                   componentSpecification ? componentSpecification.getPropertyType(propName): undefined,
        	                   undefined, undefined, undefined, $rootScope.getDesignFormControllerScope(), propertyContextCreator.withPushToServerFor(propName));
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
    setCSSPositionProperties: function(cssPositionObject,cssPosition){
    	var properties = ['left','right','top','bottom','width','height','min-width','min-height'];
    	for ( var i = 0 ;i<properties.length;i++)
    	{
    		var prop = properties[i];
    		delete cssPositionObject[prop];
    		if (cssPosition[prop] !== undefined)
    		{
    			cssPositionObject[prop] = cssPosition[prop] ;
    		}
    	}	
    	return cssPositionObject;
    },
    updateFormData: function(updates) {
      var data = JSON.parse(updates);
      if (data && (data.components || data.deleted || data.renderGhosts || data.parts || data.containers || data.deletedContainers || data.compAttributes)) {
    	  var setCSSPositionProperties = this.setCSSPositionProperties;
        // TODO should it be converted??
        $rootScope.$apply(function() {

          var toDeleteA = [];
          var domElementsToRemove = [];
          
          if (data.containers) {
              for(var j = 0; j < data.containers.length; j++) {
                var key = data.containers[j].uuid;
                var element = updateElementIfParentChange(key, data, { layoutId: key },false,domElementsToRemove);
                if(element) {
                  for (attribute in data.containers[j]) {
                      if('uuid' === attribute) continue;
                      element.attr(attribute, data.containers[j][attribute]);
                  }
                }
              }
          }
            
          for (var index in data.deletedContainers) {
            var toDelete = angular.element('[svy-id="' + data.deletedContainers[index] + '"]');
            toDelete.remove();

          }          
          
          if(data.updatedFormComponentsDesignId) {
        	  for (var index in data.updatedFormComponentsDesignId) {
                  for (var name in formData.components) {
                      // doSomethingHere(); in titanium this check was replaced by something like if (PersistIdentifier.fromJSONString(child).isDirectlyNestedInside(formComponentComponentIdentifier)) {
                	  if((name.lastIndexOf(data.updatedFormComponentsDesignId[index] + '$', 0) === 0) && (data.formComponentsComponents.indexOf(name) == -1) ) {
                		  toDeleteA.push(name);
                	  }
                  }  
        	  }
          }

          if(data.deleted) {
        	  toDeleteA = toDeleteA.concat(data.deleted);
          }
          
          if (toDeleteA.length > 0)
          {
        	  flushGhostContainerElements();
          }	  
          
          for (var index in toDeleteA) {
              var toDelete = angular.element('[svy-id="' + toDeleteA[index] + '"]');
              toDelete.remove();
              $rootScope.getDesignFormControllerScope().removeComponent(toDeleteA[index]);
          }          
          
          if (data.componentSpecs) {
              $typesRegistry.addComponentClientSideSpecs(data.componentSpecs);
          }
          
          for (var name in data.components) {
            var componentSpecification = $typesRegistry.getComponentSpecification(data.componentSpecNames[name]);
            var propertyContextCreator = $pushToServerUtils.newRootPropertyContextCreator(function(propertyName) { return data.components[name][propertyName]; }, componentSpecification);
            
        	var forceUpdate = false;
            var compData = formData.components[name];
            var newCompData = data.components[name];

            var modifyFunction = undefined;
            if (compData) {
            	
              if (data.updatedFormComponentsDesignId) {
            	  var fixedName = name.replace(/-/g, "_");
            	  if(!isNaN(fixedName[0])) {
            		  fixedName = "_" + fixedName;
            	  }
            	  if((data.updatedFormComponentsDesignId.indexOf(fixedName)) != -1 && (compData.containedForm != newCompData.containedForm)) {
            		  forceUpdate = true;
            	  }
              }
            	
              modifyFunction = compData[$sabloConstants.modelChangeNotifier];
              
              // keys that were there and are no longer there should get cleared
              var key;
              for (key in compData) {
                if (!newCompData[key]) {
                  compData[key] = null;
                  if (modifyFunction) modifyFunction(key, null)
                }
              }
            }
            
            if (!compData) compData = formData.components[name] = {};
            
            for (var propName in newCompData) {
              compData[propName] = $sabloConverters.convertFromServerToClient(newCompData[propName],
                               componentSpecification ? componentSpecification.getPropertyType(propName) : undefined,
                               compData ? compData[propName] : undefined, undefined, undefined, $rootScope.getDesignFormControllerScope(),
                               propertyContextCreator.withPushToServerFor(propName));
              if (modifyFunction) modifyFunction(key, newCompData[key])
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
            	updateElementIfParentChange(name, data, { name: name },forceUpdate,domElementsToRemove);
            }

            var compLayout = layoutData[name];
            if (compLayout) {
            	if (formData.formProperties['useCssPosition'][name])
            	{
            		setCSSPositionProperties(compLayout,newCompData['cssPosition']);
            	}
            	else
            	{
            		 compLayout.left = newCompData.location.x + "px";
                     compLayout.top = newCompData.location.y + "px";
                     compLayout.width = newCompData.size.width + "px";
                     compLayout.height = newCompData.size.height + "px";
            	}	
            }
          }
          
          if(domElementsToRemove.length) {
            for (var index in domElementsToRemove) {
              domElementsToRemove[index].remove();
            }
            flushGhostContainerElements();
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

          renderDecorators();
          if (!$rootScope.getDesignFormControllerScope().absoluteLayout) {
			$rootScope.$broadcast("UPDATE_FORM_DATA");
		  }
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
}).directive('svySolutionLayoutClass',  function ($rootScope) {
	return {
		restrict: 'A',
		link: function (scope, element) {
			scope.$watch(function() {
        return element.attr('svy-solution-layout-class');
      }, function(newVal, oldValue) {
        updateElementClass('svy-solution-layout-class', $rootScope.showSolutionLayoutsCss, oldValue);
      });
      
			scope.$watch(function() {
        return element.attr('svy-layout-class');
      }, function(newVal, oldValue) {
        updateElementClass('svy-layout-class', true, oldValue);
      });

      scope.$watch(function() {
        return $rootScope.showSolutionLayoutsCss;
      }, function(newVal) {
        updateElementClass('svy-solution-layout-class', $rootScope.showSolutionLayoutsCss);
      });

		function removeContainerChildrenClass(element, children)
		{
			if (children > 0 && children < 10)
			{
				element.removeClass('containerChildren'+children);
			}
			if (children >= 10)
			{
				element.removeClass('containerChildren10')
			}
		}
		
		function addContainerChildrenClass(element, children)
		{
			if (children > 0 && children < 10)
			{
				element.addClass('containerChildren'+children);
			}
			if (children >= 10)
			{
				element.addClass('containerChildren10')
			}
		}
		
		scope.$watch(function() {
			return $rootScope.maxLevel;
		}, function(newVal, oldVal) {
			if (!$rootScope.showWireframe) return;
			var depth = element.parentsUntil('#svyDesignForm').length;
			var children = element.children().length;
			if (oldVal && depth == oldVal) {
				element.removeClass('maxLevelDesign');
				removeContainerChildrenClass(element, children);
			}
		
			if (depth == $rootScope.maxLevel) {
				element.addClass('maxLevelDesign');
				addContainerChildrenClass(element, children);
			}
		});
		
		scope.$watch(function() {
        	return $rootScope.showWireframe;
      	}, function(newVal, oldVal) {
			if (!$rootScope.showWireframe && element.hasClass('maxLevelDesign')) {
				element.removeClass('maxLevelDesign');
				removeContainerChildrenClass(element, element.children().length);
			}
			if ($rootScope.showWireframe)
			{
				var depth = element.parentsUntil('#svyDesignForm').length;
				if (depth == $rootScope.maxLevel) {
					element.addClass('maxLevelDesign');
					addContainerChildrenClass(element, element.children().length);
				}
			}
      });

		scope.$watch(function() {
			return element.children().length;
		}, function(newVal, oldVal) {
			if (!element.hasClass('maxLevelDesign')) return;
			removeContainerChildrenClass(element, oldVal);
			addContainerChildrenClass(element, newVal);
		});


      function updateElementClass(fromClass, isAdding, toRemove) {
        var classes;
        if(toRemove) {
          classes = toRemove.split(" ");
          for(var i = 0; i < classes.length; i++) {
            element.removeClass(classes[i]);
          }
        }
        classes = element.attr(fromClass).split(" ");
        for(var i = 0; i < classes.length; i++) {
          if(isAdding) {
              if(!element.hasClass(classes[i])) {
                element.addClass(classes[i]);
              }
          }
          else {
              element.removeClass(classes[i]);
          }
        }
      }
		}
	}
}).directive('elementReady', function($timeout, $rootScope) {
	return {
      restrict: 'A',
      link: function(scope, element, attrs) {
        $timeout(function(){
          element.ready(function() {
            scope.$apply(function() {
              $rootScope.$broadcast(attrs.elementReady+':ready');
            });
          });
        });
      }
    };
});