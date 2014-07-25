angular.module('editor', ['palette','toolbar','mouseselection']).run(function($rootScope, $editor) {
	$(document).ready(function() {
		$("#editor").on('documentReady.content', function(event, contentDocument) {
			$editor.setIFrameDoc(contentDocument);
		});
	});
}).factory("$editor",function($rootScope, EDITOR_EVENTS) {
	var selection = [];
	var plugins = [];
	var iframeDoc = null;
	function fireSelectionChanged(){
		$rootScope.$broadcast(EDITOR_EVENTS.SELECTION_CHANGED,selection)
	}

	return {
		setIFrameDoc: function(contentDocument) {
			iframeDoc = contentDocument;
			
			// editor is ready init plugins
			for(var i=0;i<plugins.length;i++) {
				plugins[i]();
			}
		},
		
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
//			Array.prototype.push.apply(delta.removedNodes, this.selection)
//			this.selection.length = 0
			
//			Array.prototype.push.call(delta.addedNodes, node)
//			Array.prototype.push.call(this.selection, node)
			
			fireSelectionChanged()
		},
		
		registerPlugin: function(plugin) {
			plugins[plugins.length] = plugin;
			// if there is already a iframeDoc just init directly.
			if (iframeDoc) plugin();
		},
		
		// can only be called after a plugin gets the callback from the registerPlugin
		registerDOMEvent:function(eventType, target,callback) {
			if (target == "FORM") {
				// $(iframeDoc) == internal iframe document.
				// TODO can we set a selector/class to filter? 
				$(iframeDoc).on(eventType, null, callback.bind(this))
			} else if (target == "EDITOR") {
				console.log("registering dom event: " + eventType)
				// $(doc) is the document of the editor (or a div)
//				$(doc).on(eventType, context, callback.bind(this))
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