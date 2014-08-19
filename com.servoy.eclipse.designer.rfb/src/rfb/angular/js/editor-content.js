angular.module('editorContent',['servoyApp'])
 .controller("MainController", function($scope, $window, $timeout, $windowService, $webSocket){
	 function getURLParameter(name) {
			return decodeURIComponent((new RegExp('[?|&]' + name + '=' + '([^&;]+?)(&|#|;|$)').exec($window.location.search)||[,""])[1].replace(/\+/g, '%20'))||null
	}
	 var formName = getURLParameter("f");
	 $scope.getUrl = function() {
		 if ($webSocket.isConnected())
			 return $windowService.getFormUrl(formName);
	 }
 })