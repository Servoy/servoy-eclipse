angular.module("palette",['ui.bootstrap']).directive("palette", function($editorService,$compile){
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
	    	 
	    	 $scope.enterDragMode = function(event,componentName,packageName,tagName,model)
	    	 {
	    		 var dragClone = null;
	    		 var angularElement = null;
	    		 var mouseentercallback;
	    		 var mouseleavecallback;
	    		 var mouseupcallback;
	    		 var mousemovecallback = $scope.registerDOMEvent("mousemove","EDITOR", function(ev){
	    			 if (dragClone)
	    			 {
	    				 var css = { top: ev.pageY, left: ev.pageX }
		    			 dragClone.css(css);
	    				 css = $scope.convertToContentPoint(css);
	    				 angularElement.css(css);
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
	    				 
	    				 angularElement = $scope.getEditorContentRootScope().createComponent('<div><'+tagName+' svy-model=\'model\' svy-api=\'api\' svy-handlers=\'handlers\'/></div>',model);
	    				 var elWidth = model.size ? model.size.width : 100;
	    				 var elHeight = model.size ? model.size.height : 100;
	    				 var css = $scope.convertToContentPoint({
			    			 position: 'absolute',
			    			 top: event.pageY,
			    			 left: event.pageX,
			    			 width: (elWidth +'px'),
			    			 height: (elHeight +'px'),
			    			 'z-index': 4,
			    			 opacity: 0,
			    			 transition: 'opacity .5s ease-in-out 0'
			    		 });
	    				 angularElement.css(css)
	    			 }	 
	    		 });
	    		 mouseentercallback = $scope.registerDOMEvent("mouseenter","CONTENTFRAME_OVERLAY", function(ev){
	    			 dragClone.css('opacity', '0');
	    			 angularElement.css('opacity', '1');
	    		 });
	    		 mouseleavecallback = $scope.registerDOMEvent("mouseenter","PALETTE", function(ev){
	    			 dragClone.css('opacity', '1');
	    			 angularElement.css('opacity', '0');
	    		 });
	    		 mouseupcallback = $scope.registerDOMEvent("mouseup","EDITOR", function(ev){
	    			 if (mousemovecallback) $scope.unregisterDOMEvent("mousemove","EDITOR",mousemovecallback);
	    			 if (mouseupcallback)  $scope.unregisterDOMEvent("mouseup","EDITOR",mouseupcallback);
	    			 if (mouseentercallback) $scope.unregisterDOMEvent("mouseenter","CONTENTFRAME_OVERLAY",mouseentercallback);
	    			 if (mouseleavecallback) $scope.unregisterDOMEvent("mouseenter","PALETTE",mouseleavecallback);
	    			 if (angularElement)
	    			 {
	    				 angularElement.remove();
	    			 }
	    			 if (dragClone)
	    			 {
	    				 dragClone.remove();
	    				 var component = {};
		    			 component.name = componentName;
		    			 component.packageName = packageName;
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
