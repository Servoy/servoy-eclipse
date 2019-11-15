angular.module("toolbar",['toolbaractions','designsize'])
.run(["$templateCache","$http",function($templateCache,$http){
	$http.get("templates/toolbaritem.html").then(function(result){
		$templateCache.put("templates/toolbaritem.html", result.data);
    });
	$http.get("templates/toolbaritemdropdown.html").then(function(result){
		$templateCache.put("templates/toolbaritemdropdown.html", result.data);
    });	
	$http.get("templates/toolbarswitch.html").then(function(result){
		$templateCache.put("templates/toolbarswitch.html", result.data);
    });	
	$http.get("templates/toolbaritemspinner.html").then(function(result){
		$templateCache.put("templates/toolbaritemspinner.html", result.data);
    });
}])
.directive("toolbarSwitch", ['$templateCache','$compile',function($templateCache,$compile){
	return {
	      restrict: 'E',
	      transclude: true,
	      scope: {
	    	  model: "=model",
	      },
	      link: function($scope, $element, $attrs) {
	    	  $element.html($templateCache.get("templates/toolbarswitch.html"));
	          $compile($element.contents())($scope);
	      },
	      replace: true
	    };
}])
.directive("toolbarSpinner", ['$templateCache','$compile',function($templateCache,$compile){
	return {
		restrict: 'E',
		transclude: true,
		scope: {
			model: "=model",
		},
		link: function($scope, $element, $attrs) {
			$element.html($templateCache.get("templates/toolbaritemspinner.html"));
			$compile($element.contents())($scope);
			$scope.value = $scope.model.min;
			$scope.$watch('model.initialValue', function(newValue, oldValue){
				$scope.value = $scope.model.initialValue;
			});
			$scope.$watch('value', function(newValue, oldValue) {
				if (newValue == undefined || newValue < $scope.model.min || newValue > $scope.model.max) {
					return;
				}
		
				$scope.value = newValue;
				$scope.model.onSet(newValue);
			});
			$scope.dec = function() {
				$scope.value--;
			}
			$scope.inc = function() {
				$scope.value++;
			}
			$scope.checkInput = function() {
				if ($scope.value == undefined) {
					$scope.value = $scope.model.initialValue; 
				}
				if ($scope.value < $scope.model.min) {
					$scope.value = $scope.model.min;
				}
				if ($scope.value > $scope.model.max) {
					$scope.value = $scope.model.max;
				}
			}
		},
		replace: true
	};
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
.directive("toolbar", function(TOOLBAR_CATEGORIES,$pluginRegistry){
	return {
	      restrict: 'E',
	      transclude: true,
	      scope: {
	      },
	      controller: function($scope, $element, $attrs, $toolbar) {
	    	  var editor;
	    	  $pluginRegistry.registerPlugin(function(editorScope) {
	    			editor = editorScope;
	    			$scope.elements = $toolbar.getButtons(TOOLBAR_CATEGORIES.ELEMENTS);
	    			$scope.form = $toolbar.getButtons(TOOLBAR_CATEGORIES.FORM);
//			    	$scope.show_data = $toolbar.getButtons(TOOLBAR_CATEGORIES.SHOW_DATA);
	    			if(editor.isAbsoluteFormLayout()) {
	    			  $scope.display = $toolbar.getButtons(TOOLBAR_CATEGORIES.DISPLAY);
	    			  $scope.ordering = $toolbar.getButtons(TOOLBAR_CATEGORIES.ORDERING);
			    	  $scope.alignment = $toolbar.getButtons(TOOLBAR_CATEGORIES.ALIGNMENT);
			    	  $scope.distribution = $toolbar.getButtons(TOOLBAR_CATEGORIES.DISTRIBUTION);
			    	  $scope.sizing = $toolbar.getButtons(TOOLBAR_CATEGORIES.SIZING);
			    	  $scope.grouping = $toolbar.getButtons(TOOLBAR_CATEGORIES.GROUPING);//TODO move this outside the if when SVY-9108 Should be possible to group elements in responsive form. is done

	    			}
	    			else {
	    				$scope.ordering = $toolbar.getButtons(TOOLBAR_CATEGORIES.ORDERING_RESPONSIVE);
						$scope.zoom = $toolbar.getButtons(TOOLBAR_CATEGORIES.ZOOM);
						$scope.zoom_level = $toolbar.getButtons(TOOLBAR_CATEGORIES.ZOOM_LEVEL);
	    				$scope.design_mode = $toolbar.getButtons(TOOLBAR_CATEGORIES.DESIGN_MODE);
	    				$scope.sticky = $toolbar.getButtons(TOOLBAR_CATEGORIES.STICKY);
	    			}
	    			$scope.standard_actions = $toolbar.getButtons(TOOLBAR_CATEGORIES.STANDARD_ACTIONS);
	    	  });
	    	  
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
		}
	}
})
.value("TOOLBAR_CATEGORIES", {
	ELEMENTS: "elements",
	ORDERING: "ordering",
	ORDERING_RESPONSIVE: "ordering_responsive",
	ALIGNMENT: "alignment",
	DISTRIBUTION: "distribution",
	SIZING: "sizing",
	GROUPING: "grouping",
	ZOOM: "zoom",
	ZOOM_LEVEL: "zoom_level",
	FORM: "forms",
	DISPLAY: "display",
	EDITOR: "editor",
	STICKY: "sticky",
//	SHOW_DATA: "show_data",
	DESIGN_MODE: "design_mode",
	STANDARD_ACTIONS: "standard_actions"
})