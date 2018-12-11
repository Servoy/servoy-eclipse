angular.module("palette", ['ui.bootstrap', 'ui.sortable'])
.config(['$provide', function($provide) {
	$provide.decorator('uibAccordionDirective', function($delegate) {
		var directive = $delegate[0];
		directive.replace = true;
		return $delegate;
	});
}]).directive("palette", function($editorService, $compile, $selectionUtils, $rootScope, EDITOR_EVENTS, $document, $interval) {
	return {
		restrict: 'E',
		transclude: true,
		controller: function($scope, $element, $attrs, $http, $pluginRegistry) {
			$scope.packages = [];
			$scope.sortableOptions = {
				handle: ' .handle',
				// items: ' .panel:not(.panel-heading)'
				//axis: 'y'
				stop: function(e, ui) {
					var data = $scope.packages;
					packageOrder[layoutType] = [];
					for (var i = 0; i < data.length; i++) {
						packageOrder[layoutType].push(data[i].packageName);
					}
					$editorService.updatePaletteOrder(packageOrder);
				}
			};
			$scope.searchText = '';
			$scope.openPackageManager = function()
			{
				$editorService.openPackageManager();
			}
			
			var fontPromise = $editorService.getSystemFont();
			fontPromise.then(function(result){
			    $scope.systemFont = 'font-family: '+result.font + '; font-size: '+result.size+'px; font-weight: 400;';
			});
			
			var utils = $selectionUtils.getUtilsForScope($scope);

			var layoutType = null;
			var formName = null;
			var packageOrder = null;
			var editorScope = null;
			var loadPalette = function() {
				$http({
					method: 'GET',
					url: '/designer/palette?layout=' + layoutType+'&formName='+formName
				}).then(function(got) {
					var propertyValues;
					if(got.data[got.data.length-1] && got.data[got.data.length-1]['propertyValues'])
					{
						propertyValues = got.data[got.data.length-1]['propertyValues'];
						$scope.packages = got.data.slice(0, got.data.length -1);
					}
					else
					{
						$scope.packages = got.data;
					}
					packageOrder = {};
					packageOrder[layoutType] = [];
					for(var i = 0; i < $scope.packages.length; i++) {
						$scope.packages[i].isOpen = true;
						packageOrder[layoutType].push($scope.packages[i].packageName);
						
						if ($scope.packages[i].components)
						{
							for (var j = 0; j < $scope.packages[i].components.length; j++) {
								if (propertyValues && $scope.packages[i].components[j].properties) {
									$scope.packages[i].components[j].isOpen = false;
									//we still need to have the components with properties on the component for filtering
									$scope.packages[i].components[j].components = propertyValues;
								}
							}
						}
					}
				});
			}

			$pluginRegistry.registerPlugin(function(scope) {
				editorScope = scope;
				if (scope.isAbsoluteFormLayout())
					layoutType = "Absolute-Layout"; // TODO extract as constant, this is a key in the main attributes of the manifest
				else
					layoutType = "Responsive-Layout";
				formName = scope.getFormName();
				loadPalette(formName);
			});

			$scope.showPreviewImage = function(preview) {
				var host = angular.element(location).attr('host');
				var protocol = angular.element(location).attr('protocol');
				$editorService.showImageInOverlayDiv(protocol + "//" + host + "/" + preview);
			};

			/**
			 * TODO refactor this; currently it is extremely ambiguous
			* enterDragMode($event,item.name,package.packageName,item.tagName,item.model,item.topContainer,layoutName)  for new components from the palette
			* enterDragMode($event,ghost.type,null,null,null,ghost.type,null,null,ghostPropertyName) for a ghost
			*/
			$scope.enterDragMode = function(event, componentName, packageName, tagName, model, type, topContainer,
				layoutName, ghostPropertyName,componentTagName, propertyName, propertyValue) {
					var dragClone = null;
					var angularElement = null;
					var mouseentercallback;
					var mouseleavecallback;
					var mouseupcallback;
					var mouseoutcallback;
					var t;
					var insertedClone;
					var insertedCloneParent;
					
					var autoscrollEnter = [];
					var autoscrollLeave = [];
					var autoscrollStop = [];

					var mousemovecallback = $scope.registerDOMEvent("mousemove", "EDITOR", function(ev) {
						if (dragClone) {
						    if (layoutName)
						   	    editorScope.getEditorContentRootScope().drop_highlight = packageName + "." + layoutName;
						   	else if (ghostPropertyName)
						   	    editorScope.getEditorContentRootScope().drop_highlight = componentName + "." + type;
						   	else
						   	    editorScope.getEditorContentRootScope().drop_highlight = packageName + "." + type;
						   	editorScope.getEditorContentRootScope().$apply();
							var css = {
								top: ev.pageY,
								left: ev.pageX
							};
							dragClone.css(css);
							if (angularElement) {
								var x = (window.pageXOffset !== undefined) ? window.pageXOffset : document.documentElement.scrollLeft;
								var y = (window.pageYOffset !== undefined) ? window.pageYOffset : document.documentElement.scrollTop;
								var offset = 0;
								if (!$scope.isAbsoluteFormLayout()) offset = 1;
								var angularCss = {
									top: ev.pageY - y + offset,
									left: ev.pageX - x + offset
								};
								angularElement.css($scope.convertToContentPoint(angularCss));
							}

							var canDrop = utils.getDropNode(type, topContainer, layoutName ? packageName + "." + layoutName : layoutName, ev, componentName);
							if (!canDrop.dropAllowed) {
								$scope.glasspane.style.cursor = "not-allowed";
							} else $scope.glasspane.style.cursor = "pointer";

							if (canDrop.dropTarget && !$scope.isAbsoluteFormLayout() && angularElement) {
								if ($scope.glasspane.style.cursor == "pointer") {

									if (t) clearTimeout(t);
									t = setTimeout(function() {
										
										if (canDrop.beforeChild) {
											if (insertedClone == null) insertedClone = angular.element(angularElement)[0].firstElementChild.cloneNode(true);
											canDrop.dropTarget.insertBefore(insertedClone, canDrop.beforeChild);
											angularElement.css('opacity', '1');
											insertedCloneParent = canDrop.dropTarget;
										} else if (angularElement.parent()[0] != canDrop.dropTarget || canDrop.append) {
											if (insertedClone == null) insertedClone = angular.element(angularElement)[0].firstElementChild.cloneNode(true);
											angular.element(canDrop.dropTarget).append(insertedClone);
											angularElement.css('opacity', '1');
											insertedCloneParent = canDrop.dropTarget;
										}
										editorScope.refreshEditorContent();
									}, 200);

								} else {
									angularElement.css('opacity', '0');
									$scope.getEditorContentRootScope().getDesignFormElement()[0].removeChild(angularElement[0]);
								}
							}
						} else {
							dragClone = angular.element(event.target).clone();
							utils.setDraggingFromPallete(type);
							$scope.setSelection(null);
							dragClone.attr('id', 'dragNode')
							dragClone.css({
								position: 'absolute',
								top: event.pageY,
								left: event.pageX,
								'z-index': 4,
								'pointer-events': 'none',
								'list-style-type': 'none'
							})
							$document[0].body.appendChild(dragClone[0]);
							if (type == 'component' || type == "layout" || type == "template") {
								if (type == 'component') {
									if ($scope.isAbsoluteFormLayout())
									    angularElement = $scope.getEditorContentRootScope().createAbsoluteComponent('<div><'+ (componentTagName ? componentTagName : tagName) +' svy-model=\'model\' svy-api=\'api\' svy-handlers=\'handlers\' svy-servoyApi=\'svy_servoyApi\' svy-autoapply-disabled=\'true\'/></div>', model);
									else 
									    angularElement = $scope.getEditorContentRootScope().createComponent('<div>'+tagName+'</div>', model);
								}
								else {
									// tagname is the full element
									angularElement = $scope.getEditorContentRootScope().createComponent('<div>'+tagName+'</div>');
								}
								var elWidth = model.size ? model.size.width : 200;
								var elHeight = model.size ? model.size.height : 100;
								css = $scope.convertToContentPoint({
									position: 'absolute',
									top: event.pageY,
									left: event.pageX,
									width: (elWidth + 'px'),
									'z-index': 4,
									opacity: 0,
									transition: 'opacity .5s ease-in-out 0'
								});
								if ($scope.isAbsoluteFormLayout()){
								    css.height = elHeight + 'px';
								    css.outline = '1px dotted black';
								}
								angularElement.css(css);
							}
						}
						ev.preventDefault();
					});
					
					mouseentercallback = $scope.registerDOMEvent("mouseenter","CONTENTFRAME_OVERLAY", function(){
						if (angularElement) {
						    	$scope.setPointerEvents("all");	
							dragClone.css('opacity', '0');
							angularElement.css('opacity', '1');
						}
					});
					
					mouseleavecallback = $scope.registerDOMEvent("mouseenter","PALETTE", function(){
						if (angularElement) {
						    	$scope.setPointerEvents("none");	
							dragClone.css('opacity', '1');
							angularElement.css('opacity', '0');
						}
					});
					
					var directions = ["BOTTOM_AUTOSCROLL", "RIGHT_AUTOSCROLL", "LEFT_AUTOSCROLL", "TOP_AUTOSCROLL"];
					function getAutoscrollEnterCallback(direction) {
						return $scope.registerDOMEvent("mouseenter",direction, function(event){
						    autoscrollStop[direction] = $scope.startAutoScroll(direction);
						});						
					}
					function getAutoscrollLeaveCallback(direction) {
						return $scope.registerDOMEvent("mouseleave",direction, function(){
						    if (angular.isDefined(autoscrollStop[direction])) {
						            $interval.cancel(autoscrollStop[direction]);
						            autoscrollStop[direction] = undefined;
						    }
						});				
					}					
					
					for(var i = 0; i < directions.length; i++) {
						var direction = directions[i];
						autoscrollEnter[direction] =  getAutoscrollEnterCallback(direction);
						autoscrollLeave[direction] = getAutoscrollLeaveCallback(direction);
					}
					
					function canceldrag(ev)
					{
						for (var direction in autoscrollStop) {
							if (angular.isDefined(autoscrollStop[direction])) {
								$interval.cancel(autoscrollStop[direction]);
								autoscrollStop[direction] = undefined;
							}
						}

					    $scope.setPointerEvents("none");
						if (mousemovecallback) $scope.unregisterDOMEvent("mousemove", "EDITOR", mousemovecallback);
						if (mouseupcallback) $scope.unregisterDOMEvent("mouseup", "EDITOR", mouseupcallback);
						if (mouseoutcallback) $scope.unregisterDOMEvent("mouseleave", "EDITOR", mouseoutcallback);
						if (mouseentercallback) $scope.unregisterDOMEvent("mouseenter", "CONTENTFRAME_OVERLAY",
						mouseentercallback);
						if (mouseleavecallback) $scope.unregisterDOMEvent("mouseenter", "PALETTE", mouseleavecallback);

						for (var direction in autoscrollEnter) {
							if(autoscrollEnter[direction]) $scope.unregisterDOMEvent("mouseenter", direction, autoscrollEnter[direction]);
						}
						for (var direction in autoscrollLeave) {
							if(autoscrollLeave[direction]) $scope.unregisterDOMEvent("mouseleave", direction, autoscrollLeave[direction]);
						}
						
						
						$scope.glasspane.style.cursor = "";
						editorScope.getEditorContentRootScope().drop_highlight = null;
						editorScope.getEditorContentRootScope().$apply();

						
					}
					
					function onmouseup(ev)
					{
						canceldrag(); 
						if (dragClone) {
							if (insertedClone){
								insertedCloneParent.removeChild(insertedClone);
								insertedClone = null;
							}
							$document[0].body.removeChild(dragClone[0]);
							
							var canDrop = utils.getDropNode(type, topContainer, layoutName ? packageName + "." + layoutName : layoutName, ev, componentName);
							utils.setDraggingFromPallete(null);
							
							if (angularElement) {
								// if the drop was still allowed an the beforeChild is not set
								// then ask the current angularelement for the next sibling because it
								// is in the location where it should be dropped but the canDrop matched purely on the parent
								if (canDrop.dropAllowed && !canDrop.beforeChild) {
									canDrop.beforeChild = angularElement[0].nextElementSibling;
								}
								if (t) clearTimeout(t);
								$scope.getEditorContentRootScope().getDesignFormElement()[0].removeChild(angularElement[0]);
								//the next element can be the dummy one that was inserted just to see how it would look
								//so get the next one because that one should be a real element with a real svy-id
								if (canDrop.beforeChild && canDrop.beforeChild.getAttribute("svy-id") === "null")
								canDrop.beforeChild = canDrop.beforeChild.nextElementSibling;
							}
							if (!canDrop.dropAllowed) return;

							var component = {};
							if (canDrop.dropTarget) {
								component.dropTargetUUID = canDrop.dropTarget.getAttribute("svy-id");
							}

							if (canDrop.beforeChild) {
								component.rightSibling = canDrop.beforeChild.getAttribute("svy-id");
							}
							component.name = componentName;
							component.packageName = packageName;

							component.x = ev.pageX;
							component.y = ev.pageY;
							if (ghostPropertyName) component.ghostPropertyName = ghostPropertyName;
							if (propertyName) component[propertyName] = propertyValue;							
							if (angularElement && $scope.isAbsoluteFormLayout()) {
								var x = (window.pageXOffset !== undefined) ? window.pageXOffset : document.documentElement.scrollLeft;
								var y = (window.pageYOffset !== undefined) ? window.pageYOffset : document.documentElement.scrollTop;
								component.x = component.x - x;
								component.y = component.y - y;
							}

							// default sizes should be the same as the one we create the dragging component with above.
							if (model) {
								component.w = model.size ? model.size.width : 200;
								component.h = model.size ? model.size.height : 100;
							} else {
								component.w = 200;
								component.h = 100;
							}
							if (type != "component" && type != "layout" && type != "template") {
								component.type = type;
								if (!(canDrop.dropTarget.getAttribute("svy-types").indexOf(type) > 0))
								return; // releasing a ghost, but component does not support this ghost type
							}
							component = $scope.convertToContentPoint(component);
							if (component.x > 0 && component.y > 0) {
								$editorService.createComponent(component);
							}
						} else {
							if (angularElement) {
								$scope.getEditorContentRootScope().getDesignFormElement().removeChild(angularElement);
							}
						}

					}
					
					mouseupcallback = $scope.registerDOMEvent("mouseup", "EDITOR", onmouseup);
					mouseoutcallback = $scope.registerDOMEvent("mouseleave", "EDITOR", function(ev){
						canceldrag();
						if (dragClone) {
							if (insertedClone){
								insertedCloneParent.removeChild(insertedClone);
								insertedClone = null;
							}
							$document[0].body.removeChild(dragClone[0]);
							utils.setDraggingFromPallete(null);
						}
						else if (angularElement) {
							$scope.getEditorContentRootScope().getDesignFormElement().removeChild(angularElement);
						}
					});
				}
			},
			templateUrl: 'templates/palette.html',
			replace: true
		};

}).directive("paletteComponents", function($parse) {
    return {
	restrict: "E",
	scope: {
	    "package":"=",
	    "searchText":"=",
	    "enterDragMode":"=",
	    "showPreviewImage":"="
	},
	link: function($scope, $element, $attrs) {
	    $scope.items = $scope.package.components;
	    if ($attrs['svyComponents']){
		 $scope.items = $scope.package.categories[$attrs['svyComponents']];
	    }
	},
	templateUrl: 'templates/palettecomponents.html'
    };
}).filter('filterByNested', ['$parse','$filter', function( $parse, $filter ) {
	
	//Object.values is not supported in IE and Object.keys does not always work
    Object.values = Object.values || function values(obj) {
    	  var ret = [];
    	  for (var prop in obj) if (obj.hasOwnProperty(prop)) ret.push(obj[prop])
    	  return ret;
    }
	
    return function(collection, properties, search, strict) {
    	
        search = (typeof search === 'string' || typeof search === 'number') ?
          String(search).toLowerCase() : undefined;
       
        if(!Array.isArray(collection) || search == undefined) {
          return collection;
        }
        if (!Array.isArray(properties) && properties)
        {
        	properties = [properties];
        }
        
        return collection.filter(function(elm) {
          return properties.some(function(prop) {
        	  var comparator = String($parse(prop)(elm)).toLowerCase();
              var found = strict ? comparator === search : comparator.indexOf(search) >= 0;
              if (!found)
              { 
            	  //search property in nested arrays
            	  return Object.values(elm).some(function(val) {
            		  if (Array.isArray(val) && val.length > 0)
            		  {
            			  return $filter('filterByNested')(val, properties, search, strict).length > 0;
            		  }
            	  });
              }
              return found;
          });
        });
      }
    }])