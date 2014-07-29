//TODO: select element in editor, support multiselect, range select, control-select
//TODO: while hovering over a LI in the Outline, highlight element in editor as well
//TODO: Support DND-ing elements within the Outline View
//TODO: Allow DND-ing new and existing elements into the Outline View in correct position: Highlight both drop location and encompassing container
//TODO: hookup initialization code for Outline through event fired by Editor 
//TODO: Hide/Show Outline: toogle view with Palette, auto-show while DND-ing
//TODO: highlight selected elements in outline view
//TODO: Icons
//TODO: Fix display name: for example, now shows a the wrapper div for replaced elements
//TODO: Make display of ID's/classes optional

function Outline(selector){
	this.editor = null
	this.outline = $(selector)
}

Outline.prototype = Object.create(null)
Outline.prototype.constructor = Outline.prototype

Outline.prototype.setEditor = function(editor) {
	var instance = this
	this.editor = editor

	var $editor = editor.editor
	
	$editor.on(Editor.EVENT_TYPES.CONTENT_READY, function() {
		instance.initTree()
		$(instance.editor.contentDocument).find('.sfcontent').on('hover', '.svelement', function(e){
			console.log('Outline should update, as element is hovered')
			//Render hover decorator, but delayed
			//instance.editor.getDecoratorFor(this)
		})
		
	}).on(Editor.EVENT_TYPES.CONTENT_CHANGED, function(e, changes) {
		console.log('Outline should update, as content has changed', changes)
		//TODO: optimize: instead of full render, try updating existing content
		//Only trigger reinitializing the tree if nodes were added/removed
		for (var i = 0; i < changes.length; i++) {
			if (changes[i].addedNodes.length || changes[i].removedNodes.length) {
				instance.initTree()
				break;
			}
		}

	}).on('selection_changed.editor', function(e, changes) {
		console.log('Outline should update, as selection has changed')
	}).on(Editor.EVENT_TYPES.DRAG_START, function(e){
		instance.editor.parts.GLASSPANE.on('mouseenter', function() {
			instance.setVisibility(true)
		})
	}).on(Editor.EVENT_TYPES.DRAG_END, function() {
		instance.setVisibility(false)
	})
}

Outline.prototype.initTree = function(){
	var structure = []
	var stack = [{ node: $(this.editor.contentDocument).find('.sfcontent').children(), children: structure }]

	while (stack.length > 0) {
		var stackEntry = stack.pop()

		stackEntry.node.children(Editor.ACTION_CONTEXT.ELEMENT).each(function(i, el) {
			var childDOMNode = $(el)
			var node = { node: childDOMNode, children: [] }
			stack.push(node)
			stackEntry.children.push(node)
		})
	}

	//Convert structure into string in correct order
	stack = structure.slice(0).reverse()
	var retval = '<ul class="tree">',
		level = [stack.length]

	while (stack.length) {
		while (level[level.length - 1] === 0) {
			level.pop()
			retval += '</ul>'
		}

		stackEntry = stack.pop()
		level[level.length - 1]--

		//TODO: need to make sure each element always has an ID or some other mechanism to target individual elements
		var id = stackEntry.node.attr('id')
		retval += '<li svy-id="' + id + '">'
		retval += stackEntry.node.get(0).tagName.toLowerCase()
		if (id != null) {
			retval += '&nbsp;id=' + id
		} else {
			var clazz = stackEntry.node.attr('class')
			if (clazz) {
				retval += '&nbsp;class=' + clazz.replace(/ /g, '&nbsp;')
			}
		}
		retval += '</li>'

		level.push(0)
		retval += '<ul>'
		for (var i = stackEntry.children.length - 1; i >= 0; i--) {
			var node = stackEntry.children[i]
			stack.push(node)
			level[level.length - 1]++
		}
	}
	retval += '</ul>'
	
	this.outline.empty()
	$(retval).appendTo(this.outline)
}

Outline.prototype.setVisibility = function(state){
	this.outline.css('visibility', state ? 'visible' : 'hidden')
}
