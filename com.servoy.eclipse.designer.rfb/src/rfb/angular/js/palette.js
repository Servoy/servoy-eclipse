angular.module("palette",['ui.bootstrap']).directive("palette", function(){
	return {
	      restrict: 'E',
	      transclude: true,
	      scope: {},
	      controller: function($scope, $element, $attrs, $http) {
	    	 $scope.categories = [];
	    	  
	    	 function addCategory(name, isOpen) {
	    		 for(var c in $scope.categories) {
	    			 if($scope.categories[c].name == name) {
	    				 return $scope.categories[c];
	    			 }
	    		 }
	    		 var category = {name : name, components: [], isOpen: isOpen};
	    		 $scope.categories.push(category);
	    		 
	    		 return category;
	    	 }
	    	 
	    	 function addComponent(categoryName, name, icon) {
	    		 var category = addCategory(categoryName, true);
	    		 category.components.push({name : name, icon : icon});
	    	 }
	    	 
	    	 addCategory("Elements", true);
	    	 addCategory("Shapes", true);
	    	 addCategory("Containers", true);
	    	 addCategory("Beans", true);
	    	 $http({method: 'GET', url: '/designer/palette'}).success(function(data) {
		    		for(var i = 0; i < data.length; i++) {
		    			addComponent(data[i].categoryName ? data[i].categoryName : "Beans", data[i].displayName, data[i].icon ? "/" + data[i].icon : "");
		    		}
	    	 });
	      },
	      templateUrl: 'templates/palette.html',
	      replace: true
	    };
	
})