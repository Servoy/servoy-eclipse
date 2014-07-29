angular.module("decorators",['editor']).directive("decorator", function($rootScope,EDITOR_EVENTS){
	return {
	      restrict: 'E',
	      transclude: true,
	      controller: function($scope, $element, $attrs) {
	    	  var resizeDecoratorIncludeMargin = false;
	    	  function renderDecorators(selection) {
	    			var decorationOverlays = $element.children('.decorationOverlay')

	    			var i = -1;
	    			selection.forEach(function(value, index, array) {
	    				i = index
	    				var node = $(value)

	    				var decorationOverlay
	    				if (index < decorationOverlays.length) {
	    					decorationOverlay = $(decorationOverlays[index])
	    				} else {
	    					//TODO: each decorator plugin should be able to contribute decorators and then this code should create a documentFragment including all the decorators for performance reasons
	    					var div = ''.concat('<div class="decorationOverlay">',
	    						'<div class="margin t"/>',
	    						'<div class="margin r"/>',
	    						'<div class="margin b"/>',
	    						'<div class="margin l"/>',
	    						'<div class="knob t l"/>',
	    						'<div class="knob t"/>',
	    						'<div class="knob t r"/>',
	    						'<div class="knob r"/>',
	    						'<div class="knob b r"/>',
	    						'<div class="knob b"/>',
	    						'<div class="knob b l"/>',
	    						'<div class="knob l"/>',
	    						'</div>')

	    					decorationOverlay = $(div)
	    					decorationOverlay.children().addClass('decorator') //Tag actual decorators using the decorator class for layering purposes
	    					decorationOverlay.appendTo($element);
	    					decorationOverlays.add(decorationOverlay)
	    				}
	    				decorationOverlay.attr('svy-decorator-for', node.attr('name'))
	    				if (resizeDecoratorIncludeMargin) {
	    					var position = node.offset()
	    					var marginTop = parseFloat('0' + node.css('marginTop'))
	    					var marginLeft = parseFloat('0' + node.css('marginLeft'))

	    					//TODO: zoom factor
	    					decorationOverlay.css({
	    						width: node.outerWidth(true),
	    						height: node.outerHeight(true),
	    						top: position.top - marginTop,
	    						left: position.left - marginLeft,
	    						display: 'block'
	    					})
	    				} else {
	    					var offset = node.offset()
	    					offset.top -= $scope.contentWindow.scrollY
	    					offset.left -= $scope.contentWindow.scrollX
	    					var height = node.outerHeight()
	    					var width = node.outerWidth()
	    					var marginTop = parseFloat('0' + node.css('marginTop'))
	    					var marginRight = parseFloat('0' + node.css('marginRight'))
	    					var marginBottom = parseFloat('0' + node.css('marginBottom'))
	    					var marginLeft = parseFloat('0' + node.css('marginLeft'))

	    					decorationOverlay.css({
	    						height: height,
	    						width: width,
	    						top: offset.top,
	    						left: offset.left,
	    						display: 'block'
	    					})

	    					decorationOverlay.children('.margin.t').css({
	    						height: marginTop,
	    						left: 0,
	    						right: -marginLeft - 1,
	    						marginTop: -marginTop - 1,
	    						marginLeft: -marginLeft - 1
	    					})
	    					decorationOverlay.children('.margin.r').css({
	    						width: marginRight,
	    						top: 0,
	    						bottom: -1,
	    						marginRight: -marginRight - 1,
	    						marginTop: -1
	    					})
	    					decorationOverlay.children('.margin.b').css({
	    						height: marginBottom,
	    						left: 0,
	    						right: -marginLeft - 1,
	    						marginBottom: -marginBottom - 1,
	    						marginLeft: -marginLeft - 1
	    					})
	    					decorationOverlay.children('.margin.l').css({
	    						width: marginLeft,
	    						top: 0,
	    						bottom: -1,
	    						marginLeft: -marginLeft - 1,
	    						marginTop: -1
	    					})
	    				}
	    			})

	    			//Hide unused decorationOverlays
	    			for (i++; i < decorationOverlays.length; i++) {
	    				$(decorationOverlays[i]).css('display', 'none')
	    			}
	    		}
	    	  $rootScope.$on(EDITOR_EVENTS.SELECTION_CHANGED, function(event, selection) {
	    		  renderDecorators(selection);
	    	  })
	      },
	      template: '<div class="decorators"></div>',
	      replace: true
	    };
	
})
