angular.module("palette",['ui.bootstrap']).directive("palette", function($editorService){
	return {
	      restrict: 'E',
	      transclude: true,
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
	    	 
	    	 function addComponent(categoryName,displayName, name, icon) {
	    		 var category = addCategory(categoryName, true);
	    		 category.components.push({displayName : displayName, name: name, icon : icon});
	    	 }
	    	 
	    	 addCategory("Elements", true);
	    	 addCategory("Shapes", true);
	    	 addCategory("Containers", true);
	    	 addCategory("Beans", true);
	    	 $http({method: 'GET', url: '/designer/palette'}).success(function(data) {
		    		for(var i = 0; i < data.length; i++) {
		    			addComponent(data[i].categoryName ? data[i].categoryName : "Beans", data[i].displayName, data[i].name, data[i].icon ? "/" + data[i].icon : "");
		    		}
	    	 });
	    	 
	    	 $scope.enterDragMode = function(event,componentName)
	    	 {
	    		 var dragClone = $(event.target).clone()
	    		 dragClone.attr('id', 'dragNode')
	    		 dragClone.css({
	    			 position: 'absolute',
	    			 top: event.pageY,
	    			 left: event.pageX,
	    			 'z-index': 4,
	    			 'pointer-events': 'none',
	    			 'list-style-type': 'none'
	    		 })
	    		 $('body').append(dragClone);
	    		 
	    		 var mousemovecallback = $scope.registerDOMEvent("mousemove","EDITOR", function(ev){
	    			 var css = { top: ev.pageY, left: ev.pageX }
	    			 dragClone.css(css);
	    		 });
	    		 var mouseupcallback = $scope.registerDOMEvent("mouseup","EDITOR", function(ev){
	    			 dragClone.remove();
	    			 if (mousemovecallback) $scope.unregisterDOMEvent("mousemove","EDITOR",mousemovecallback);
	    			 if (mouseupcallback)  $scope.unregisterDOMEvent("mouseup","EDITOR",mouseupcallback);
	    			 var component = {};
	    			 component.name = componentName;
	    			 component.x = ev.pageX;
	    			 component.y = ev.pageY;
	    			 component = $scope.convertToContentPoint(component);
	    			 $editorService.createComponent(component);
	    		 });
	    	 }
	      },
	      templateUrl: 'templates/palette.html',
	      replace: true
	    };
	
})