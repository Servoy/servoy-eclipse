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
	    	  $scope.loadToolbar = function() {
		    	  $scope.elements = $toolbar.getButtons(TOOLBAR_CATEGORIES.ELEMENTS);
		    	  $scope.ordering = $toolbar.getButtons(TOOLBAR_CATEGORIES.ORDERING);
		    	  $scope.alignment = $toolbar.getButtons(TOOLBAR_CATEGORIES.ALIGNMENT);
		    	  $scope.distribution = $toolbar.getButtons(TOOLBAR_CATEGORIES.DISTRIBUTION);
		    	  $scope.sizing = $toolbar.getButtons(TOOLBAR_CATEGORIES.SIZING);
		    	  $scope.form = $toolbar.getButtons(TOOLBAR_CATEGORIES.FORM);
		    	  $scope.display = $toolbar.getButtons(TOOLBAR_CATEGORIES.DISPLAY);
		    	  $scope.sticky = $toolbar.getButtons(TOOLBAR_CATEGORIES.STICKY);	    		  
	    	  }
	    	  $scope.loadToolbar();
	    	  $toolbar.registerPanel($scope);
	      },
	      templateUrl: 'templates/toolbar.html',
	      replace: true
	    };
	
})
.factory("$toolbar", function(TOOLBAR_CATEGORIES){
	var buttons= [];
	var panelScope;
	return {
		registerPanel: function(scope) {
			panelScope = scope;
		},
		
		add: function(buttonState, category) {
			if(!buttons[category]){
				buttons[category] = [];
			}
			buttons[category].push(buttonState);
		},
		
		getButtons: function(category) {
			return buttons[category];
		},
		
		refresh: function() {
			if(panelScope) panelScope.loadToolbar()
		}
	}
})
.value("TOOLBAR_CATEGORIES", {
	ELEMENTS: "elements",
	ORDERING: "ordering",
	ALIGNMENT: "alignment",
	DISTRIBUTION: "distribution",
	SIZING: "sizing",
	FORM: "forms",
	DISPLAY: "display",
	EDITOR: "editor",
	STICKY: "sticky"
})