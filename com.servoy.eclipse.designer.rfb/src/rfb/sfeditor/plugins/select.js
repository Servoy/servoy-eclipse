/*
 * Handles selecting elements using the mouse.
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
		function handleSelectionEvent(e, node) {
			if (e.ctrlKey) {
				if (instance.getSelection().indexOf(node[0]) !== -1) {
					instance.reduceSelection(node[0])
				} else {
					instance.extendSelection(node[0])
				}
			} else if (e.shiftKey) {
				//TODO: implement
			} else {
				instance.setSelection(node[0])
			}
			e.stopPropagation()
		}
		
		this.registerBehavior({
			context: Editor.ACTION_CONTEXT.ELEMENT,
			event: 'mousedown',
			action: function(e) {
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
	}
	
	Editor.registerPlugin(Selection)
}())
