angular.module("contextmenu",['contextmenuactions']).directive("contextmenu", function($editorService){
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