angular.module("palette", ['ui.bootstrap', 'ui.sortable'])
.config(['$provide', function($provide) {
	$provide.decorator('accordionDirective', function($delegate) {
		var directive = $delegate[0];
		directive.replace = true;
		return $delegate;
	});
}]).directive("palette", function($editorService, $compile, $selectionUtils, $rootScope, EDITOR_EVENTS, $document) {
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
					$scope.packages = got.data;
					packageOrder = {};
					packageOrder[layoutType] = [];
					for(var i = 0; i < got.data.length; i++) {
						got.data[i].isOpen = "true";
						packageOrder[layoutType].push(got.data[i].packageName);
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

			$rootScope.$on(EDITOR_EVENTS.RELOAD_PALETTE, function(e) {
				loadPalette(formName);
			});

			/**
			* enterDragMode($event,item.name,package.packageName,item.tagName,item.model,item.topContainer,layoutName)  for new components from the palette
			* enterDragMode($event,ghost,null,null,null,ghost,null) for a ghost
			*/
			$scope.enterDragMode = function(event, componentName, packageName, tagName, model, type, topContainer,
				layoutName, propertyName) {
					var dragClone = null;
					var angularElement = null;
					var mouseentercallback;
					var mouseleavecallback;
					var mouseupcallback;
					var t;
					var insertedClone;
					var insertedCloneParent;
					var mousemovecallback = $scope.registerDOMEvent("mousemove", "EDITOR", function(ev) {
						if (dragClone) {
						    if (layoutName)
						   	    editorScope.getEditorContentRootScope().drop_highlight = layoutName;
						   	else
						   	    editorScope.getEditorContentRootScope().drop_highlight = type;
						   	editorScope.getEditorContentRootScope().$apply();
						    if (insertedClone){
								insertedCloneParent.removeChild(insertedClone);
								insertedClone = null;
							}
							var css = {
								top: ev.pageY,
								left: ev.pageX
							};
							dragClone.css(css);
							if (angularElement) {
								var x = (window.pageXOffset !== undefined) ? window.pageXOffset : document.documentElement.scrollLeft;
								var y = (window.pageYOffset !== undefined) ? window.pageYOffset : document.documentElement.scrollTop;
								var angularCss = {
									top: ev.pageY - y + 2,
									left: ev.pageX - x +2
								};
								angularElement.css($scope.convertToContentPoint(angularCss));
							}

							var canDrop = utils.getDropNode(type, topContainer, layoutName, ev, componentName);
							if (!canDrop.dropAllowed) {
								$scope.glasspane.style.cursor = "no-drop";
							} else $scope.glasspane.style.cursor = "";

							if (canDrop.dropTarget && !$scope.isAbsoluteFormLayout() && angularElement) {
								if ($scope.glasspane.style.cursor == "") {

									if (t) clearTimeout(t);
									t = setTimeout(function() {
										
										if (canDrop.beforeChild) {
											insertedClone = angular.element(angularElement)[0].firstElementChild.cloneNode(true);
											canDrop.dropTarget.insertBefore(insertedClone, canDrop.beforeChild);
											angularElement.css('opacity', '1');
											insertedCloneParent = canDrop.dropTarget;
										} else if (angularElement.parent()[0] != canDrop.dropTarget || canDrop.append) {
											insertedClone = angular.element(angularElement)[0].firstElementChild.cloneNode(true);
											angular.element(canDrop.dropTarget).append(insertedClone);
											angularElement.css('opacity', '1');
											insertedCloneParent = canDrop.dropTarget;
										}
										editorScope.refreshEditorContent();
									}, 200);

								} else {
									angularElement.css('opacity', '0');
									$scope.getEditorContentRootScope().getDesignFormElement()[0].removeChild(angularElement);
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
									    angularElement = $scope.getEditorContentRootScope().createAbsoluteComponent('<div><'+tagName+' svy-model=\'model\' svy-api=\'api\' svy-handlers=\'handlers\' svy-autoapply-disabled=\'true\'/></div>', model);
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
								}
								angularElement.css(css);
								//
							}
						}
					});
					mouseentercallback = $scope.registerDOMEvent("mouseenter","CONTENTFRAME_OVERLAY", function(){
						if (angularElement) {
							dragClone.css('opacity', '0');
							// if ($scope.isAbsoluteFormLayout()) {
							angularElement.css('opacity', '1');
							// }
						}
					});
					mouseleavecallback = $scope.registerDOMEvent("mouseenter","PALETTE", function(){
						if (angularElement) {
							dragClone.css('opacity', '1');
							// if ($scope.isAbsoluteFormLayout()) {
							angularElement.css('opacity', '0');
							//}
						}
					});
					mouseupcallback = $scope.registerDOMEvent("mouseup", "EDITOR", function(ev) {
						if (mousemovecallback) $scope.unregisterDOMEvent("mousemove", "EDITOR", mousemovecallback);
						if (mouseupcallback) $scope.unregisterDOMEvent("mouseup", "EDITOR", mouseupcallback);
						if (mouseentercallback) $scope.unregisterDOMEvent("mouseenter", "CONTENTFRAME_OVERLAY",
						mouseentercallback);
						if (mouseleavecallback) $scope.unregisterDOMEvent("mouseenter", "PALETTE", mouseleavecallback);
						$scope.glasspane.style.cursor = "";
						editorScope.getEditorContentRootScope().drop_highlight = null;
						editorScope.getEditorContentRootScope().$apply();
						if (dragClone) {
							if (insertedClone){
								insertedCloneParent.removeChild(insertedClone);
								insertedClone = null;
							}
							$document[0].body.removeChild(dragClone[0]);
							
							var canDrop = utils.getDropNode(type, topContainer, layoutName, ev, componentName);
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
							if (propertyName) component.propertyName = propertyName;
							if (angularElement && $scope.isAbsoluteFormLayout()) {
								var x = (window.pageXOffset !== undefined) ? window.pageXOffset : document.documentElement.scrollLeft;
								var y = (window.pageYOffset !== undefined) ? window.pageYOffset : document.documentElement.scrollTop;
								component.x = component.x - x;
								component.y = component.y - y;
							}

							if (model) {
								component.w = model.size ? model.size.width : 100;
								component.h = model.size ? model.size.height : 100;
							} else {
								component.w = 100;
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
					});
				}
			},
			templateUrl: 'templates/palette.html',
			replace: true
		};

	})
