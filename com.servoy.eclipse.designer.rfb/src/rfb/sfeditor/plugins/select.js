/*
 * Selection plugin
 * - Handles selecting elements using the mouse.
 * - Exposes selection API
 * 
 * Tricky detail is NOT changing selection when clicking on a scrollbar of a container that results in a scroll
 */
(function(){
	//Static property that gets automatically initialized with the native scrollbar width
	Editor.scrollbarWidth = function() {
		//TODO: implement dynamic retrieval of value
		//see http://stackoverflow.com/questions/10045423/determine-whether-user-clicking-scrollbar-or-content-onclick-for-native-scroll
		return 18
	}()
	
	function Selection() {
		var instance = this

		/* Selection Behavior */
		function handleSelectionEvent(e, node) {
			var htmlNode = node[0]
			if (e.ctrlKey) {
				if (instance.getSelection().indexOf(htmlNode) !== -1) {
					instance.reduceSelection(htmlNode)
				} else {
					instance.extendSelection(htmlNode)
				}
			} else if (e.shiftKey) {
				//TODO: implement
				/*
				 * Shift-Click does range select: all elements within the box defined by the uttermost top/left point of selected elements
				 * to the uttermost bottom-right of the clicked element
				 */
			} else {
				instance.setSelection(htmlNode)
			}
			e.stopPropagation()
		}

		this.registerBehavior({
			context: Editor.ACTION_CONTEXT.ELEMENT,
			event: 'mousedown',
			action: function(e) {
				console.log('select start')
				var node = $(e.currentTarget)
				if (!node.is(Editor.ACTION_CONTEXT.ELEMENT)) {
					node = node.parent(Editor.ACTION_CONTEXT.ELEMENT)
				}
				
				//Logic to prevent selection if the node's scrollbars were clicked and the resulted in a scroll 
				//TODO: this should be moved out of the selection logic and into separate action, registered before the select, 
				//so it can be customized when scrollbars get customized with some lib
				var rect = node[0].getBoundingClientRect()
				
				var mustDelay = false
				var cssSettings = node.css(['overflow', 'overflow-x', 'overflow-y'])
	
				var el = node[0]
				
				if (el.clientWidth - el.scrollWidth < 0 && 
					e.pageY > rect.bottom - Editor.scrollbarWidth && 
					(cssSettings['overflow-x'] === 'auto' || cssSettings['overflow-x'] === 'scroll')) {
					mustDelay = true
				}
				if (el.clientHeight - el.scrollHeight < 0 && 
					e.pageX > rect.right - Editor.scrollbarWidth && 
					(cssSettings['overflow-y'] === 'auto' || cssSettings['overflow-y'] === 'scroll')) {
					mustDelay = true
				}
	
				if (mustDelay) { 
					//Scrollable scrollbar is clicked: delay selection action and don't select if a scroll happened
					var ignoreSelect = false
					$(instance.contentDocument).one('mouseup.select', function() {
						//IE fires mouseup before scroll, so timeout needed
						setTimeout(function(){
							if (!ignoreSelect) {
								handleSelectionEvent(instance.editor, e, node)
							}
							node.off('scroll.select')
						}, 10)
					})
	
					node.on('scroll.select', function() {
						ignoreSelect = true
					})
					e.stopPropagation()
					return
				}
				handleSelectionEvent(e, node)
			}
		})

		/* Expose Selection API */
		//TODO: selection should update itself when elements get deleted
		var timeout, 
			delta = {
				addedNodes: [],
				removedNodes: []
			}
		
		function markDirty() {
			if (timeout) {
				clearTimeout(timeout)
			}
			timeout = setTimeout(fireSelectionChanged, 1)
		}
		
		function fireSelectionChanged(){
			//Reference to editor should be gotten from Editor instance somehow
			instance.fire(Editor.EVENT_TYPES.SELECTION_CHANGED, delta)
			delta.addedNodes.length = delta.removedNodes.length = 0
			timeout = null
		}
		
		this.getSelection = function() {
			//Returning a copy so selection can't be changed my modifying the selection array
			return this.selection.slice(0)
		}
	
		this.extendSelection = function(nodes) {
			var ar = Array.isArray(nodes) ? nodes : [nodes]
			var dirty = false
			
			for (var i = 0; i < ar.length; i++) {
				if (this.selection.indexOf(ar[i]) === -1) {
					dirty = true
					delta.addedNodes.push(ar[i])
					this.selection.push(ar[i])
				}
			}
			if (dirty) {
				markDirty()
			}
		}
	
		this.reduceSelection = function(nodes) {
			var ar = Array.isArray(nodes) ? nodes : [nodes]
			var dirty = false
			for (var i = 0; i < ar.length; i++) {
				var idx = this.selection.indexOf(ar[i])
				if (idx !== -1) {
					dirty = true
					delta.removedNodes.push(ar[i])
					this.selection.splice(idx)
				}
			}
			if (dirty) {
				markDirty()
			}
		}
	
		this.setSelection = function(node) {
			var selection = Array.isArray(node) ? node : node ? [node] : []
			var dirty = selection.length||this.selection.length
			Array.prototype.push.apply(delta.removedNodes, this.selection)
			this.selection.length = 0
			
			Array.prototype.push.apply(delta.addedNodes, selection)
			Array.prototype.push.apply(this.selection, selection)
			
			if (dirty) {
				markDirty()
			}
		}
	}
	
	Editor.registerPlugin('selection', Selection)
}())