angular.module('editor', ['palette','toolbar','mouseselection','decorators']).factory("$pluginRegistry",function($rootScope) {
	var plugins = [];

	return {
		registerEditor: function(editorScope) {
			for(var i=0;i<plugins.length;i++) {
				plugins[i](editorScope);
			}
		},
		
		registerPlugin: function(plugin) {
			plugins[plugins.length] = plugin;
		},
	}
}).value("EDITOR_EVENTS", {
    SELECTION_CHANGED : "SELECTION_CHANGED"
}).directive("editor", function( $window, $pluginRegistry,$rootScope,EDITOR_EVENTS){
	return {
	      restrict: 'E',
	      transclude: true,
	      scope: {},
	      link: function($scope, $element, $attrs) {
			var iframeDoc;  
			var timeout;
			var delta = {
				addedNodes: [],
				removedNodes: []
			}
			var selection = [];
			function markDirty() {
				if (timeout) {
					clearTimeout(timeout)
				}
				timeout = setTimeout(fireSelectionChanged, 1)
			}
			
			function fireSelectionChanged(){
				console.log(selection)
				//Reference to editor should be gotten from Editor instance somehow
				//instance.fire(Editor.EVENT_TYPES.SELECTION_CHANGED, delta)
				$rootScope.$broadcast(EDITOR_EVENTS.SELECTION_CHANGED,selection)
				delta.addedNodes.length = delta.removedNodes.length = 0
				timeout = null
			}
			
			function getURLParameter(name) {
				return decodeURIComponent((new RegExp('[?|&]' + name + '=' + '([^&;]+?)(&|#|;|$)').exec($window.location.search)||[,""])[1].replace(/\+/g, '%20'))||null
			}
			$scope.contentframe = "editor-content.html?id=%23" + $element.attr("id") + "&f=" + getURLParameter("f") +"&s=" + getURLParameter("s");
			$scope.contentWindow = $element.find('.contentframe')[0].contentWindow;
			console.log($scope.contentWindow)
			$scope.registerDOMEvent = function(eventType, target,callback) {
				if (target == "FORM") {
				$(iframeDoc).on(eventType, null, callback.bind(this))
			} else if (target == "EDITOR") {
				console.log("registering dom event: " + eventType)
				// $(doc) is the document of the editor (or a div)
				//	$(doc).on(eventType, context, callback.bind(this))
				}
			}
			$scope.getSelection = function() {
				//Returning a copy so selection can't be changed my modifying the selection array
				return selection.slice(0)
			}
		
			$scope.extendSelection = function(nodes) {
				var ar = Array.isArray(nodes) ? nodes : [nodes]
				var dirty = false
				
				for (var i = 0; i < ar.length; i++) {
					if (selection.indexOf(ar[i]) === -1) {
						dirty = true
						delta.addedNodes.push(ar[i])
						selection.push(ar[i])
					}
				}
				if (dirty) {
					markDirty()
				}
			}
		
			$scope.reduceSelection = function(nodes) {
				var ar = Array.isArray(nodes) ? nodes : [nodes]
				var dirty = false
				for (var i = 0; i < ar.length; i++) {
					var idx = selection.indexOf(ar[i])
					if (idx !== -1) {
						dirty = true
						delta.removedNodes.push(ar[i])
						selection.splice(idx)
					}
				}
				if (dirty) {
					markDirty()
				}
			}
			$scope.setSelection = function(node) {
				var ar = Array.isArray(node) ? node : node ? [node] : []
				var dirty = ar.length||selection.length
				Array.prototype.push.apply(delta.removedNodes, selection)
				selection.length = 0
				
				Array.prototype.push.apply(delta.addedNodes, ar)
				Array.prototype.push.apply(selection, ar)
				
				if (dirty) {
					markDirty()
				}
			}
			
			$element.on('documentReady.content', function(event, contentDocument) {
				iframeDoc = contentDocument;
				$pluginRegistry.registerEditor($scope);
			});
	      },
	      templateUrl: 'templates/editor.html',
	      replace: true
	    };
	
});