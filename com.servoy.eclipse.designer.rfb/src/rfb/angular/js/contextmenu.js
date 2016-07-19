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
					if(componentName == "component" || packageAndComponent[1] == "*") return "Component";
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
						var allowedChildren = $allowedChildren.get(selection.getAttribute("svy-layoutname"));
						var types = selection.getAttribute("svy-types");
						if (allowedChildren || types){
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
										text: k < typesStartIdx ? getDisplayName(typesArray[k]) : typesArray[k].type + ' for ' + typesArray[k].property,
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
														component.propertyName = typesArray[k].property;
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

				$scope.$digest();
				$("#contextMenu")
				.css({
					display: "block",
					left: e.pageX,
					top: e.pageY
				})
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