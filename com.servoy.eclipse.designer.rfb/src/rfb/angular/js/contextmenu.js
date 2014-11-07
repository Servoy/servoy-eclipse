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
						if(ghost && (ghost.type == EDITOR_CONSTANTS.PART_PERSIST_TYPE)) {
							$("#contextMenu").hide();
							return false;
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
		}
	}
})