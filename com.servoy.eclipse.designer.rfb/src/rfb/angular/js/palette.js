angular.module("palette",['ui.bootstrap']).directive("palette", function($editorService){
	return {
	      restrict: 'E',
	      transclude: true,
	      controller: function($scope, $element, $attrs, $http) {
	    	 $scope.packages = [];
	    	 $http({method: 'GET', url: '/designer/palette'}).success(function(data) {
	    		 $scope.packages = data;
		    		for(var i = 0; i < data.length; i++) {
		    			data[i].isOpen = "true";
		    		}
	    	 });
	    	 
	    	 $scope.enterDragMode = function(event,componentName)
	    	 {
	    		 var dragClone = null;
	    		 
	    		 var mousemovecallback = $scope.registerDOMEvent("mousemove","EDITOR", function(ev){
	    			 if (dragClone)
	    			 {
	    				 var css = { top: ev.pageY, left: ev.pageX }
		    			 dragClone.css(css);
	    			 }
	    			 else
	    			 {
	    				 dragClone = $(event.target).clone()
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
	    			 }	 
	    		 });
	    		 var mouseupcallback = $scope.registerDOMEvent("mouseup","EDITOR", function(ev){
	    			 if (mousemovecallback) $scope.unregisterDOMEvent("mousemove","EDITOR",mousemovecallback);
	    			 if (mouseupcallback)  $scope.unregisterDOMEvent("mouseup","EDITOR",mouseupcallback);
	    			 if (dragClone)
	    			 {
	    				 dragClone.remove();
	    				 var component = {};
		    			 component.name = componentName;
		    			 component.x = ev.pageX;
		    			 component.y = ev.pageY;
		    			 component = $scope.convertToContentPoint(component);
		    			 if (component.x >0 && component.y >0)
		    			 {
		    				 $editorService.createComponent(component); 
		    			 }
	    			 }	 
	    		 });
	    	 }
	      },
	      templateUrl: 'templates/palette.html',
	      replace: true
	    };
	
})
