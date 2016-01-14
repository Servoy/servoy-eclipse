angular.module('editorContent', ['servoyApp']).controller('MainController', function($scope, $window, $timeout,
  $windowService, $webSocket, $sabloApplication,
  $servoyInternal, $rootScope, $compile, $solutionSettings, $editorContentService) {
  $rootScope.createComponent = function(html, model) {
    var compScope = $scope.$new(true);
    compScope.model = model;
    compScope.api = {};
    compScope.handlers = {};
    var el = $compile(html)(compScope);
    angular.element('body').append(el);
    return el;
  }
  var vm = this;
  $rootScope.highlight = false;
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
        me.onopen()
      }
      setTimeout(onopenCaller, 0);
    }

    SwtWebSocket.prototype.send = function(str) {
      parent.window.SwtWebsocketBrowserFunction('send', str, this.id)
    }
  }
  $servoyInternal.connect();
  var formName = $webSocket.getURLParameter("f");
  var solutionName = $webSocket.getURLParameter("s");
  var high = $webSocket.getURLParameter("highlight");
  $rootScope.highlight = high;
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
          formUrl = "designertemplate/" + solutionName + "/" + formName + ".html?highlight=" + $rootScope.highlight;
        });
      } else {
        // this main url is in design (the template must have special markers)
        return formUrl;
      }
    }
  };


}).controller("DesignFormController", function($scope, $editorContentService, $rootScope) {
  var vm = this;
  $scope.formStyle = {
    left: "0px",
    right: "0px",
    top: "0px",
    bottom: "0px",
    overflow:"hidden"
  }

  $scope.removeComponent = function(name) {
    delete model[name];
    delete api[name];
    delete handlers[name];
    delete servoyApi[name];
    delete layout[name];
  }

  $rootScope.getDesignFormControllerScope = function() {
    return $scope;
  };

  var formData = $editorContentService.formData();

  if (formData.parts) {
	  for(var name in formData.parts) {
		  $scope[name] = JSON.parse(formData.parts[name]);
	  }
  }
  var model = {}
  var api = {}
  var handlers = {}
  var servoyApi = {}
  var layout = {}

  $editorContentService.setLayoutData(layout);

  $scope.model = function(name, noCreate) {
    var ret = model[name];
    if (!ret && !noCreate) {
      if (formData.components[name]) ret = formData.components[name];
      else ret = {}
      model[name] = ret;
    }
    return ret;
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
			formWillShow: function(formname,relationname,formIndex) {
			},
			hideForm: function(formname,relationname,formIndex) {
				return null;
			},
			getFormUrl: function(formUrl) {
				return null;
			},
			startEdit: function(propertyName) {
			},
			apply: function(propertyName) {
			},
			callServerSideApi: function(methodName,args) {
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
        ret.position = 'absolute'
      }
      layout[name] = ret;
    }
    return ret;
  }
}).factory("$editorContentService", function($rootScope, $applicationService, $sabloApplication, $sabloConstants,
  $webSocket, $compile,$sabloConverters) {
  var formData = null;
  var layoutData = null;
  
  function handleTemplate(data) {
	  // append the template
	  var json = JSON.parse(data);
	  if (json.renderGhosts) {
	  	renderGhosts();
	  	return;
	  }
	  // was there already a template generated before this, then just skip it
	  if ($rootScope.getDesignFormControllerScope().model(name,true)) return;
	  var parentId = json.parentId;
	  if (!parentId) parentId = 'svyDesignForm';
	  
	  var parent = angular.element(document.getElementById(parentId));
	  if (!parent.length){
	      parent = angular.element(document.querySelectorAll("[svy-id='"+parentId+"']"));
	  }
	  var tpl = $compile(json.template)($rootScope.getDesignFormControllerScope());
	  if (json.insertBeforeUUID){
	      var nextSibling = angular.element(document.querySelectorAll("[svy-id='"+json.insertBeforeUUID+"']"));
	      tpl.insertBefore(nextSibling);
	  }
	  else
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
        for( var name in data.components) {
        	var compData = data.components[name];
        	if (compData.conversions) {
        		data.components[name] = $sabloConverters.convertFromServerToClient(compData, compData.conversions, undefined, undefined, undefined)
			}
        }
        if (formData.solutionProperties) {
          $applicationService.setStyleSheet(formData.solutionProperties.styleSheet);
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
              for (var key in compData) {
                if (!newCompData[key]) {
                  compData[key] = null;
                  if (modifyFunction) modifyFunction(key, null)
                }
              }
              // copy it inside so that we update the data inside the model
              for (var key in newCompData) {
                compData[key] = newCompData[key];
                if (modifyFunction) modifyFunction(key, newCompData[key])
              }
            } else {
              formData.components[name] = newCompData;
            }
            // always test if the model is really already initialized.
            // if not then there is no template yet.
            if (!$rootScope.getDesignFormControllerScope().model(name,true)) {
              var highlight = $webSocket.getURLParameter("highlight");
              var promise = $sabloApplication.callService("$editor", "getTemplate", {
                name: name,
                highlight: highlight
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
            var name = toDelete.attr("name");
            if (name) $rootScope.getDesignFormControllerScope().removeComponent(name);
            toDelete.remove();

          }
          if (data.renderGhosts) renderGhosts();
          if (data.parts) {
        	  var scope = $rootScope.getDesignFormControllerScope();
        	  for(var name in data.parts) {
        		  scope[name] = JSON.parse(data.parts[name]);
        	  }
          }
          if (data.containers) {
        	  for(var key in data.containers) {
        		  var element = angular.element(document.querySelectorAll("[svy-id='"+key+"']"));
        		  if (element && element.length > 0) {
        			  // TODO just update the attributes?
        		  }
        		  else {
        			var highlight = $webSocket.getURLParameter("highlight");
					var promise = $sabloApplication.callService("$editor", "getTemplate", {
					layoutId: key,
					highlight: highlight
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
      if (data && data.solutionProperties && formData.solutionProperties.styleSheet) {
        $applicationService.setStyleSheet(formData.solutionProperties.styleSheet);
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
