angular.module('resizeknobs',[]).directive("resizeknobs", function()
{
	return {
		restrict: 'E',
		transclude: true,
		scope: {
			node: '='
		},
		templateUrl: 'js/modules/resizeknobs/resizeknobs.html',
		replace: false
	    };
	
});