angular.module('editor', ['palette','toolbar'])
  .factory("$editor",function($rootScope, EDITOR_EVENTS) {
	var selection = [];
	
	function fireSelectionChanged(){
		$rootScope.$broadcast(EDITOR_EVENTS.SELECTION_CHANGED,selection)
	}

	return {
		
		getSelection: function() {
			//Returning a copy so selection can't be changed my modifying the selection array
			return this.selection.slice(0)
		},

		extendSelection: function(nodes) {
			var ar = Array.isArray(nodes) ? nodes : [nodes]
			var dirty = false
			
			for (var i = 0; i < ar.length; i++) {
				if (this.selection.indexOf(ar[i]) === -1) {
					this.selection.push(ar[i])
				}
			}
			if (dirty) {
				fireSelectionChanged()
			}
		},

		reduceSelection: function(nodes) {
			var ar = Array.isArray(nodes) ? nodes : [nodes]
			var dirty = false
			for (var i = 0; i < ar.length; i++) {
				var idx = this.selection.indexOf(ar[i])
				if (idx !== -1) {
					this.selection.splice(idx)
				}
			}
			if (dirty) {
				fireSelectionChanged()
			}
		},

		setSelection:function(node) {
			Array.prototype.push.apply(delta.removedNodes, this.selection)
			this.selection.length = 0
			
			Array.prototype.push.call(delta.addedNodes, node)
			Array.prototype.push.call(this.selection, node)
			
			fireSelectionChanged()
		},
		
		registerDOMEvent:function(eventType, target,callback) {
			
			if (target == "FORM") {
				// $(iframeDoc) == internal iframe document.
				$(iframeDoc).on(eventType, context, callback.bind(this))
			} else if (target == "EDITOR") {
				// $(doc) is the document of the editor (or a div)
				$(doc).on(eventType, context, callback.bind(this))
			}
		}
	}
}).value("EDITOR_EVENTS", {
    SELECTION_CHANGED : "SELECTION_CHANGED"
}).controller("MainController", function($scope,$window) {
	function getURLParameter(name) {
		return decodeURIComponent((new RegExp('[?|&]' + name + '=' + '([^&;]+?)(&|#|;|$)').exec($window.location.search)||[,""])[1].replace(/\+/g, '%20'))||null
	}
	$scope.contentframe = "editor-content.html?id=%23editor&f=" + getURLParameter("f") +"&s=" + getURLParameter("s");
});