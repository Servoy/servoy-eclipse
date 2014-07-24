angular.module('editorContent',['servoyApp'])
 .controller("MainController", function($scope, $window, $timeout){
	 function getURLParameter(name) {
			return decodeURIComponent((new RegExp('[?|&]' + name + '=' + '([^&;]+?)(&|#|;|$)').exec($window.location.search)||[,""])[1].replace(/\+/g, '%20'))||null
	}
	 $timeout(function() {
	 $scope.formtemplate = "/solutions/" + getURLParameter("s") + "/forms/" +  getURLParameter("f")+ ".html"
	 },100)
 })