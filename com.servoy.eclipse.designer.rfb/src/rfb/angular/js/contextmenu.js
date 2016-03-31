angular.module("contextmenu",['contextmenuactions']).directive("contextmenu", function($editorService, EDITOR_CONSTANTS, $selectionUtils){
	return {
		restrict: 'E',
		transclude: true,
		controller: function($scope, $element, $attrs,$contextmenu) {

			$("body").on("click", function(e) {
				$("#contextMenu").hide();
			})
			$("body").on("contextmenu", function(e) {
				var contentPoint =  $scope.convertToContentPoint( { top: e.pageY, left: e.pageX });
				if (contentPoint.left >= 0 && contentPoint.top >= 0)
				{
					var utils = $selectionUtils.getUtilsForScope($scope);
					var selection = utils.getNode(e);
					if(selection) {
						var ghost = $scope.getGhost(selection.getAttribute("svy-id"));
						if(ghost && (ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_PART)) {
							$("#contextMenu").hide();
							return false;
						}
            					var i;
            					for (i = 0; i < $scope.actions.length; i++){
            					    if ($scope.actions[i].text === "Add") {
            						var types = selection.getAttribute("svy-allowed-children");
            						if (types){
            						    $scope.actions[i].subMenu = [];
            						    var typesArray = types.split(",");
            						    var k;
            						    for (k = 0; k < typesArray.length; k++){
            							var addAction = function (k) {//save the k in a closure
                							$scope.actions[i].subMenu.push({
                            						text: typesArray[k],
                            						execute: function()
                            						{
                            						    $("#contextMenu").hide();
                            						    var component = {};
                            						    component.dropTargetUUID = selection.getAttribute("svy-id");
                            						    var nameAndPackage = typesArray[k].split(".");
                            						    component.name = nameAndPackage[1];
                            						    component.packageName = nameAndPackage[0];
                            						    component = $scope.convertToContentPoint(component);
                            						    $editorService.createComponent(component);
                            						}	
                							});
            							};
            							addAction(k);
            						    }
            						}
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
				}
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