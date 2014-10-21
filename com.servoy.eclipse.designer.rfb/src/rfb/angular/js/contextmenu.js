angular.module("contextmenu",[]).directive("contextmenu", function($editorService){
	return {
		restrict: 'E',
		transclude: true,
		controller: function($scope, $element, $attrs) {

			$("body").on("click", function(e) {
				$("#contextMenu").hide();
			})
			$("body").on("contextmenu", function(e) {
				$("#contextMenu")
					.css({
						display: "block",
						left: e.pageX,
						top: e.pageY
					})
				return false;
			});
			var actions = [];
			
			actions.push(
				{
					text: "Open in Script Editor",
					icon: "images/js.gif",
					shortcut: "Ctrl+Shift+Z",
					execute:function()
					{
						$("#contextMenu").hide();
						$editorService.executeAction('openScript');
					}
				}
			);
			$scope.actions = actions;
		},
		templateUrl: 'templates/contextmenu.html',
		replace: true
	};
})