angular.module("toolbar",['toolbaractions','designsize'])
.run(["$templateCache","$http",function($templateCache,$http){
	$http.get("templates/toolbaritem.html").then(function(result){
		$templateCache.put("templates/toolbaritem.html", result.data);
    });
	$http.get("templates/toolbaritemdropdown.html").then(function(result){
		$templateCache.put("templates/toolbaritemdropdown.html", result.data);
    });	
}])
.directive("toolbarItem", ['$templateCache','$compile',function($templateCache,$compile){
	return {
	      restrict: 'E',
	      transclude: true,
	      scope: {
	    	  model: "=model",
	      },
	      link: function($scope, $element, $attrs) {
	    	  $element.html($templateCache.get($scope.model.list ? "templates/toolbaritemdropdown.html" : "templates/toolbaritem.html"));
	          $compile($element.contents())($scope);
	          if($scope.model.list) {
	        	  $scope.onselection = function(selection) {
	        		  var text = $scope.model.onselection(selection);
	        		  if(text) $scope.model.text = text;
	        	  }
	          }
	      },
	      replace: true
	    };
}])
.directive("toolbar", function(TOOLBAR_CATEGORIES){
	return {
	      restrict: 'E',
	      transclude: true,
	      scope: {
	      },
	      controller: function($scope, $element, $attrs, $toolbar) {
	    	  $scope.elements = $toolbar.getButtons(TOOLBAR_CATEGORIES.ELEMENTS);
	    	  $scope.layout = $toolbar.getButtons(TOOLBAR_CATEGORIES.LAYOUT);
	    	  $scope.form = $toolbar.getButtons(TOOLBAR_CATEGORIES.FORM);
	    	  $scope.sticky = $toolbar.getButtons(TOOLBAR_CATEGORIES.STICKY);
	      },
	      templateUrl: 'templates/toolbar.html',
	      replace: true
	    };
	
})
.factory("$toolbar", function(TOOLBAR_CATEGORIES){
	var buttons= [];
	return {
		add: function(buttonState, category) {
			if(!buttons[category]){
				buttons[category] = [];
			}
			buttons[category].push(buttonState);
		},
		
		getButtons: function(category) {
			return buttons[category];
		}
	}
})
.value("TOOLBAR_CATEGORIES", {
	ELEMENTS: "elements",
	LAYOUT: "layout",
	FORM: "forms",
	EDITOR: "editor",
	STICKY: "sticky"
})