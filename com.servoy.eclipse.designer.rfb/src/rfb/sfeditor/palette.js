//TODO: Palette should remember expand/collapse state of Categories between sessions
function Palette(selector) {
	var instance  = this
	this.palette = $(selector)
	
	//Handle expand/collapse of Palette sections
	var collapseStateAttrName = 'data-collapsed'
	this.palette.on('click', '.palette-heading', function(event) {
		var contentNode = $(this).next()
		
		if (contentNode.attr(collapseStateAttrName) === 'true') {
			contentNode.attr(collapseStateAttrName, false)
			contentNode.css({
				'max-height': $(contentNode.children()[0]).outerHeight()
			})
		} else {
			contentNode.attr(collapseStateAttrName, true)
			contentNode.css({
				'max-height': '0px'
			})						
		}
		event.stopPropagation()
	})
	
	//Handle DND from Palette to Editor
	//TODO: move to DND plugin, so can be reused for moving elements as well
	var dragClone,
		dragInfo = {
			el: null,
			xOffset: null,
			yOffset: null
		},
		$glassPane,
		elements,
		additionalInfo
	
	function onDragStart(e) {
		if (!instance.editor) {
			console.log('Can\'t start DND operation: Editor is null')
			return
		}
		
		var frame = instance.editor.editor.find('.contentframe')[0]
		var frameRect = frame.getBoundingClientRect()
		dragInfo.xOffset = frameRect.left
		dragInfo.yOffset = frameRect.top
		
		var win = instance.editor.contentWindow
	
		//TODO: The dragClone attached to the cursor should only start to show when the mouse is moved over a certain threshold: currently it also shows when clicking an item in the palette
		dragClone = $(this).clone()
		dragClone.attr('id', 'dragNode')
		dragClone.css({
			position: 'absolute',
			top: e.pageY,
			left: e.pageX,
			'z-index': 4,
			'pointer-events': 'none',
			'list-style-type': 'none'
		})
		$('body').append(dragClone)
	
		instance.editor.setContentGlasspaneVisibility(true)
		
		//TODO: component dependancies need to be managed as well
		var el
		var template = dragClone.attr('data-component-template')
		if (template) {
			var id = win.SFComponentFactory.getNewID()
			el = $(template)
			el.attr('id', id)
			
			$(win.document.querySelector('body')).append(el)
		} else {
			el = win.SFComponentFactory.getInstance(dragClone.attr('data-component-type'))
		}
		dragInfo.el = el

		el.css({
			position: 'absolute',
			top: e.pageY - dragInfo.yOffset,
			left: e.pageX - dragInfo.xOffset,
			'z-index': 2,
			opacity: 0,
			transition: 'opacity .5s ease-in-out'
		})
		
		//START logic to insert element in correct location
		elements = document.querySelectorAll('.svelement')
		additionalInfo = new Array(elements.length)
		for (var i = 0; i < elements.length; i++) {
			additionalInfo[i] = {
				rect: elements[i].getBoundingClientRect()
			}
		}
		
		//END logic
		
		
		$(document).on('mousemove.editor', onMove)
			.on('mouseup.editor', onDrop)
	
		//TODO: the transitions don't seem to work here, needs improving
		//TODO: since the glasspane now covers the entire content-area, the item attached to the cursor while not over the content disappears too soon
		instance.editor.parts.GLASSPANE.on('mouseenter.editor', function(e) {
			dragClone.css('opacity', '0')
			dragInfo.el.css('opacity', 1)
			
		}).on('mouseleave.editor', function(e) {
			dragClone.css('opacity', '1')
			if(dragInfo.el) {
				dragInfo.el.css('opacity', 0)
			}
		})
		
		instance.editor.fire(Editor.EVENT_TYPES.DRAG_START)
	}
		
	function onMove(e) {
		var css = {
			top: e.pageY,
			left: e.pageX
		}
		dragClone.css(css)
		
		instance.editor.convertToContentPoint(css)
		dragInfo.el.css(css)
	}
	
	function onDrop(e) {
		//Hidding the dragged element from under the cursor to prevent it from becoming the target of the mouseup event
		dragInfo.el.css('visibility', 'hidden')
		dragInfo.el.addClass('svelement') //TODO: what if container?
		
		var point = instance.editor.convertToContentPoint({
			top: e.pageY,
			left: e.pageX
		})
		
		var node = instance.editor.contentDocument.elementFromPoint(point.left, point.top)
		var jqNode = $(node)

		if (!jqNode.is('.svelement')) {
			jqNode = jqNode.parent('.svelement')
		}
		var jqParent = jqNode
		if (!jqParent.is('.svcontainer')) {
			jqParent = jqParent.parent('.svcontainer')
		}
		node = jqNode[0]
		
		if (node) {
			var css = {
				position: '',
				visibility: '',
				'z-index': '',
				opacity: '',
				transition: '',
				top: '',
				left: ''
			}
			//TODO: instead of looking at the class, the positioning details of the dropped element should be evaluated within the dropped container
			if (jqParent.is('.abs')) {
				css.top = e.pageY,
				css.left = e.pageX
				instance.editor.convertToContentPoint(css, jqParent[0])
			} else if (jqParent.is('.vert')) {
			} else {
			}
			dragInfo.el.css(css)
			//Start drop logic
			if (jqNode.is('.svcontainer')) {
				jqNode.append(dragInfo.el)				
			} else {
				jqNode.after(dragInfo.el)
			}
			//End drop logic
			
			dragInfo.el = null
			instance.editor.fire(Editor.EVENT_TYPES.DROP)
		} else {
			dragInfo.el.remove()
		}
		instance.editor.setContentGlasspaneVisibility(false)
		dragClone.remove()
		$(document).off('mousemove.editor mouseup.editor')
		$glassPane.off('mouseenter.editor mouseleave.editor')
		instance.editor.fire(Editor.EVENT_TYPES.DRAG_END)
	}
	
	this.palette.on('mousedown.editor', '[data-role="component"]', onDragStart)
}

Palette.prototype = Object.create(null)
Palette.prototype.constructor = Palette

Palette.prototype.setEditor = function(editor) {
	this.editor = editor
}

Palette.prototype.setContent = function(content) {
	//TODO: generate the innerHTML based on provided content. Currently content is hardcoded
	
	//Setting the max-height CSS property, needed to animate the expand/collapse
	this.palette.find('.palette-heading').each(function(){
		var contentNode = $(this).next()
		contentNode.css({
			'max-height': $(contentNode.children()[0]).outerHeight()
		})
	})
}