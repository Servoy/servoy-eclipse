angular.module("contextmenu",['contextmenuactions']).directive("contextmenu", function($editorService, EDITOR_CONSTANTS, $selectionUtils){
	return {
		restrict: 'E',
		transclude: true,
		controller: function($scope, $element, $attrs,$contextmenu, $allowedChildren) {

			function findComponentDisplayName(arrayOfComponents, componentName) {
				if(arrayOfComponents && arrayOfComponents.length) {
					for(var j = 0; j < arrayOfComponents.length; j++) {
						if(arrayOfComponents[j].name == componentName) {
							return arrayOfComponents[j].displayName;
						}
					}
				}
				return null;
			}
			
			function getDisplayName(componentName) {
				if($scope.packages && $scope.packages.length) {
					var packageAndComponent = componentName.split(".");
					if(componentName == "component" || packageAndComponent[1] == "*") return "Component [...]";
					if (componentName == "template") return "Template [...]";
					for(var i = 0; i < $scope.packages.length; i++) {
						if($scope.packages[i].packageName == packageAndComponent[0]) {
							var displayName = findComponentDisplayName($scope.packages[i].components, packageAndComponent[1]);
							if(displayName) return displayName;

							var categories = $scope.packages[i].categories;
							if(categories) {
								for (property in categories) {
								    if (categories.hasOwnProperty(property)) {
								    	displayName = findComponentDisplayName(categories[property], packageAndComponent[1]);
								    	if(displayName) return displayName;
								    }
								}
							}
						}
					}
				}
				
				return componentName;
			}
			
			$("body").on("click", function(e) {
				$("#contextMenu").hide();
			})
			$("body").on("keyup", function(e) {
				if (e.keyCode == 27) {
					// esc key, close menu
					$("#contextMenu").hide();
				}
				
			})
			$("body").on("contextmenu", function(e) {
				return false;
			})
			$scope.registerDOMEvent("contextmenu", "CONTENTFRAME_OVERLAY", function(e) {
				var utils = $selectionUtils.getUtilsForScope($scope);
				var selection = utils.getNode(e);
				if(!selection) {
					selection = $(".contentframe").contents().find("#svyDesignForm").get(0);
				}

				var ghost = $scope.getGhost(selection.getAttribute("svy-id"));
				if(ghost && (ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_PART)) {
					$("#contextMenu").hide();
					return false;
				}
				var i;
				for (i = 0; i < $scope.actions.length; i++){
					if ($scope.actions[i].text === "Add") {
						var allowedChildren = (selection.getAttribute("svy-types") != null || (selection.getAttribute("svy-layoutname") == null && $scope.isAbsoluteFormLayout())) ? [] : $allowedChildren.get(selection.getAttribute("svy-layoutname"));
						var types = selection.getAttribute("svy-types");
						if (allowedChildren.length > 0 || (types && types.length > 2)){
							$scope.actions[i].getItemClass = function() { return "dropdown-submenu"};
							$scope.actions[i].subMenu = [];
							var typesArray = allowedChildren ? allowedChildren : [];

							var typesStartIdx = typesArray.length; 
							if(types) {
								var typesA = types.slice(1, -1).split(",");
								var propertiesA = selection.getAttribute("svy-types-properties").slice(1, -1).split(",");

								for(var x = 0; x < typesA.length; x++) {
									typesArray.push({"type" : typesA[x], "property" : propertiesA[x]});
								}
							}

							var k;
							for (k = 0; k < typesArray.length; k++){
								var addAction = function (k, typesStartIdx) {//save the k in a closure
									$scope.actions[i].subMenu.push({
										text: k < typesStartIdx ? getDisplayName(typesArray[k]) : typesArray[k].type + ' -> ' + typesArray[k].property,
												execute: function()
												{
													$("#contextMenu").hide();
													var component = {};
													if(selection.getAttribute("svy-id")) component.dropTargetUUID = selection.getAttribute("svy-id");

													if(k < typesStartIdx) {
														if (typesArray[k].indexOf(".") > 0)
														{
															var nameAndPackage = typesArray[k].split(".");
															component.name = nameAndPackage[1];
															component.packageName = nameAndPackage[0];
														}
														else
														{
															component.name = typesArray[k];
															component.packageName = undefined;
														}
													}
													else {
														component.type = typesArray[k].type; 
														component.ghostPropertyName = typesArray[k].property;
													}

													component = $scope.convertToContentPoint(component);
													$editorService.createComponent(component);
												}	
									});
								};
								addAction(k, typesStartIdx);
							}
						}
						else {
							$scope.actions[i].getItemClass = function() { return "invisible"};
						}
					}
				}
				
				$scope.adjustMenuPosition = function(element) {
					var win = $(window);
					var viewport = {
						top : win.scrollTop(),
						left : win.scrollLeft()
					};
					viewport.right = viewport.left + win.width();
					viewport.bottom = viewport.top + win.height();
					
					if (element) {
						var bounds = element.offset();
					    bounds.right = bounds.left + element.outerWidth();
					    bounds.bottom = bounds.top + element.outerHeight();
					    
					    var left = bounds.left;
					    var top = bounds.top;
					    
					    if (bounds.bottom > viewport.bottom) {
					    	//-10 to make it closer to the cursor
					    	top -= element.outerHeight() - 10;
					    }
					    if (bounds.right > viewport.right)
					    {
					    	left -= element.outerWidth() - 10;
					    }
					    
					    element
						.css({
							left: left,
							top: top
						})		
					}
					else {	
						var submenu = $(".dropdown-submenu:hover");
					    if (submenu[0]) {
					    	var menu = $(submenu[0]).find(".dropdown-menu");
						    var ctxmenu = $("#contextMenu");
						    if (menu.height() > 200 && (win.height() - ctxmenu.offset().top - menu.height()) <= 100) {
								if (ctxmenu.offset().top > menu.height()) {
									menu.css({ top: -ctxmenu.offset().top + menu.height() });
								} else {
									menu.css({ top: -ctxmenu.offset().top });
								}
							} else {
								menu.css({ top: "" });
							}
						    //the submenu can only be displayed on the right or left side of the contextmenu
						    if (ctxmenu.outerWidth() + ctxmenu.offset().left + menu.outerWidth() > viewport.right) {
						    	//+5 to make it overlap the menu a bit
						    	menu.css({ left: -1*menu.outerWidth() + 5 })
						    }
						    else {
						    	menu.css({ left: ctxmenu.outerWidth() - 5})
						    }
					    }
				    }
				};

				$scope.$digest();
				$("#contextMenu")
				.css({
					display: "block",
					left: e.pageX,
					top: e.pageY
				})
				$scope.adjustMenuPosition($("#contextMenu"));
				return false;
			});
			$scope.actions = $contextmenu.getActions();
		},
		templateUrl: 'templates/contextmenu.html',
		replace: true
	};
}).factory("$contextmenu", function(TOOLBAR_CATEGORIES){
	var actions= [];
	return {
		add: function(action) {
			actions.push(action);
		},
		
		getActions: function() {
			return actions;
		},
		
		getAddActions: function() {
		    console.log("add actions!")
		    return [];
		}
	}
})