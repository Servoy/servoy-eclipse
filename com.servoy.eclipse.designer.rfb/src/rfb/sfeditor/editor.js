/**
 * @constructor
 * @param {String} selector
 * @param {Object} [options]
 */
function Editor(selector, options) {
	var instance = this

	this.selector = selector
	
	this.options = $.extend({
			resizeDecoratorIncludeMargin: false
		}, options)

	this.editor = $(selector)

	//TODO: dynamically insert the markup needed for the editor into the node provided through the selector
	
	this.parts = {
		DECORATORS: this.editor.find('.decorators'),
		GLASSPANE: this.editor.find('.contentframe-overlay'),
		EDITOR: this.editor.find('.content-area'),
		CONTENT: this.editor.find('.content')
	}
	Object.freeze(this.parts)

	this.zoom = 1

	this.contentWindow = this.editor.find('.contentframe')[0].contentWindow

	this.contentDocument = null

	this.selection = []
	
	$(this.editor).on('documentReady.content', function(event, contentDocument) {
		instance.contentDocument = contentDocument

		//Call all plugin function in the context of the editor instance
		for (var i = 0; i < Editor.pluginRegistry.length; i++) {
			Editor.pluginRegistry[i].apply(instance);
		}
		
		//Using MutationObserver to trigger renderDecorators when content changes
		//http://addyosmani.com/blog/mutation-observers
		var observer = new MutationObserver(function() {
			instance.renderDecorators()
			instance.editor.trigger(Editor.EVENT_TYPES.CONTENT_CHANGED)
		});
		observer.observe(instance.contentDocument.querySelector('.sfcontent'), {
			subtree: true,
			childList: true,
			attributes: true
		});

		/* Mouse is moved, startposition is recorded, timeout started, while mouse is moving current position is stored
		 * also need timeout to trigger hover is mouse stopped moving?
		 * on timeout, check if delta start and current position meets distance threshold.
		 * if yes: trigger hover
		 * if no: update startposition, set timeout
		 * 
		 * Also need to take care not to trigger hover on the same element
		 * Also need mouseleave 
		 */
		
		var hoverIntentInfo = {
			startPosition: {
				x: null,
				y: null
			},
		
		}
		
		function check(){
			
		}
		
		$(instance.contentDocument).find('.sfcontent').on('mousemove', '.svelement', function(e) {
			if (hoverIntentInfo.startPosition.x === null) {
				hoverIntentInfo.startPosition.x = e.pageX
				hoverIntentInfo.startPosition.y = e.pageY
			}
			setTimeout(check, 100)
			
			
			
			if (currentElement !== e.currentTarget) {
				nextElement = e.currentTarget
				
				var dX = prevPos.x - e.pageX,
				dY = prevPos.y - e.pageY
				if (Math.sqrt(dX * dX + dY * dY) < 6) {
					triggerHover()
				} else {
					if (timeOut) {
						clearTimeout(timeOut)
					}
					timeOut = setTimeout(triggerHover, 100)					
				}
			}
			prevPos.x = e.pageX
			prevPos.y = e.pageY
			e.stopPropagation()
		})
		
//------------		
		var prevPos = {
			x: null,
			y: null
		}
		var timeOut,
			currentElement,
			nextElement
			
		function triggerHover() {
//			console.log('Hover triggered')
//			console.log(nextElement)
			currentElement = nextElement
			instance.editor.trigger('hover')
			clearTimeout(timeOut)
			timeOut = null
		}
		
//		$(instance.contentDocument).find('.sfcontent').on('mousemove', '.svelement', function(e) {
//			if (currentElement !== e.currentTarget) {
//				nextElement = e.currentTarget
//				
//				var dX = prevPos.x - e.pageX,
//				dY = prevPos.y - e.pageY
//				if (Math.sqrt(dX * dX + dY * dY) < 6) {
//					triggerHover()
//				} else {
//					if (timeOut) {
//						clearTimeout(timeOut)
//					}
//					timeOut = setTimeout(triggerHover, 100)					
//				}
//
//			}
//			prevPos.x = e.pageX
//			prevPos.y = e.pageY
//			e.stopPropagation()
//		})
//		.on('mouseenter', '.svelement', function(e) {
//			if (hoveredElement !== e.currentTarget)
//				hoveredElement = e.currentTarget
//				dirty = true
//		})
		
		instance.editor.trigger(Editor.EVENT_TYPES.CONTENT_READY)
	}).on('renderDecorators.content', function() {
		instance.renderDecorators()
	}).on(Editor.EVENT_TYPES.SELECTION_CHANGED, function(e, mutations){
		//TODO: optimize: only render the decorators for the changes
		instance.renderDecorators()
	})

	//TODO: remove observer when destroying the editor
	//observer.disconnect();

	//Code for manually getting the changes
	//observer.takeRecords();

	//Making sure that the toolbar-area height is adjusted to all toolbars fit properly
	this.rerender()
}

Editor.prototype = Object.create(Object.prototype)
Editor.prototype.constructor = Editor

Editor.prototype.setContentGlasspaneVisibility = function(state) {
	this.parts.GLASSPANE.css('display', state ? 'block' : 'none')
}

/**
 * Modifies the top and left properties of the point object to take into account the 
 * offset of the content iframe and the zoom factor
 * 
 * When the optional element param is specified, the new values for top and left will be relative to the element
 *
 * @param {{top: Number, left: Number}} point
 * @param {Object} [element]
 * @return {{top: Number, left: Number}}
 */
Editor.prototype.convertToContentPoint = function(point, element) {
	var frameRect = this.parts.CONTENT[0].getBoundingClientRect()
	var xOffset = 0,
		yOffset = 0

	if (element) {
		var rect = element.getBoundingClientRect()
		xOffset = rect.left - element.scrollLeft
		yOffset = rect.top - element.scrollTop
	}
		
	point.top = ((point.top - frameRect.top) / this.zoom) - yOffset
	point.left = ((point.left - frameRect.left) / this.zoom) - xOffset
	return point
};

(function(){
	//TODO: selection should update itself when elements get deleted
	//FIXME: this breaks multi-instance support
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
		$('#editor').trigger(Editor.EVENT_TYPES.SELECTION_CHANGED, delta)
		delta.addedNodes.length = delta.removedNodes.length = 0
		timeout = null
	}

	Editor.prototype.getSelection = function() {
		//Returning a copy so selection can't be changed my modifying the selection array
		return this.selection.slice(0)
	}

	Editor.prototype.extendSelection = function(nodes) {
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

	Editor.prototype.reduceSelection = function(nodes) {
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

	Editor.prototype.setSelection = function(node) {
		Array.prototype.push.apply(delta.removedNodes, this.selection)
		this.selection.length = 0
		
		Array.prototype.push.call(delta.addedNodes, node)
		Array.prototype.push.call(this.selection, node)
		
		markDirty()
	}
}())

/**
 * Call to rerender the Editor component if it's container dimensions have changed
 *
 * TODO: rerender shoulnd not be part of the "editor": the outline. palette and toolbar are not the editor
 * TODO: set left border width to 0 if toolbar is the first on a row. ALso to that for the floated right toolbars
 */
Editor.prototype.rerender = function(event) {
	var toolbarArea = this.editor.find('.toolbar-area')
	var lastToolbarNode = toolbarArea.children().last()
	var rect = lastToolbarNode[0].getBoundingClientRect()
	var requiredHeight = rect.top + rect.height + parseFloat(toolbarArea.css('padding-bottom')) || 0 + parseFloat(toolbarArea.css('margin-bottom')) || 0

	if (requiredHeight !== parseFloat(this.editor.find('.palette').css('top'))) {
		//TODO: palette, outline and content should be encapsulated in one container, so only one container needs updating
		this.editor.find('.palette, .content-area, .outline').css('top', requiredHeight)
		this.editor.find('.toolbar-area').css('height', requiredHeight)
	}
}

/*
 * CHECKME: Is this still needed?
 * TODO: ideally, editorOffset is a property that automatically gets updated if the offset of the editor changes
 */
Editor.prototype.editorOffset = { top: 0, left: 0 }

//TODO: add option to layout specific decorators
Editor.prototype.renderDecorators = function renderDecorators() {
	var editorInstance = this

	var decorators = this.parts.DECORATORS
	var decorationOverlays = decorators.children('.decorationOverlay')

	var i = -1;
	this.getSelection().forEach(function(value, index, array) {
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
			decorationOverlay.appendTo(decorators);
			decorationOverlays.add(decorationOverlay)
		}
		decorationOverlay.attr('svy-decorator-for', node.attr('id'))
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
			offset.top -= editorInstance.contentWindow.scrollY
			offset.left -= editorInstance.contentWindow.scrollX
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

			var marginTBWidth = width + marginRight + marginLeft
			var marginRLHeight = height

			decorationOverlay.children('.margin.t').css({
				width: marginTBWidth,
				height: marginTop,
				marginTop: -marginTop - 1,
				marginLeft: -marginLeft - 1
			})
			decorationOverlay.children('.margin.r').css({
				height: marginRLHeight,
				width: marginRight,
				marginRight: -marginRight - 1,
				marginTop: -1
			})
			decorationOverlay.children('.margin.b').css({
				width: marginTBWidth,
				height: marginBottom,
				marginBottom: -marginBottom - 1,
				marginLeft: -marginLeft - 1
			})
			decorationOverlay.children('.margin.l').css({
				height: marginRLHeight,
				width: marginLeft,
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

Editor.prototype.loadContent = function() {
	
	//TODO: implement
}

/**
 * @type {{
 * 	context: String=,
 * 	event: String=,
 * 	action: function(*)}}
 */
var BEHAVIOR_CONFIG_TYPE

/**
 * @param {BEHAVIOR_CONFIG_TYPE} config
 */
Editor.prototype.registerBehavior = function(config) {
	var doc, context
	if (config.context === Editor.ACTION_CONTEXT.EDITOR) {
		doc = document
		context = this.selector
	} else {
		doc = this.contentDocument
		context = config.context || Editor.ACTION_CONTEXT.CONTENT
	}
	$(doc).on(config.event, context, config.action.bind(this))
}

//Static API
Editor.EVENT_TYPES = {
	CONTENT_READY: 'content_ready.editor',
	CONTENT_CHANGED: 'content_changed.editor',
	DRAG_START: 'drag_start.editor',
	DRAG_END: 'drag_end.editor',
	DROP: 'drop.editor',
	DRAG_OVER: 'drag_over.editor',
	ZOOM_CHANGED: 'zoom_changed.editor',
	SELECTION_CHANGED: 'selection_changed.editor'
}

/*
 * TODO: Editor should probably automatically register the even on the editor div and the contentDocument
 */
Editor.ACTION_CONTEXT = {
	EDITOR: '.sfeditor',
	CONTENT: '.sfcontent',
	CONTAINER: '.svcontainer',
	ELEMENT: '.svelement'
}
Editor.pluginRegistry = []

Editor.registerPlugin = function(plugin){
	Editor.pluginRegistry.push(plugin)
}

//------------Old code, yet to be made obsolete--------------
var randomId = 0
var resizeDecoratorIncludeMargin = false

//Bootstrapping
$(document).ready(function() {
	var editor = new Editor('#editor')
	
	var outline = new Outline('#editor .outline')
	outline.setEditor(editor)
	
	var palette = new Palette('#editor .palette')
	palette.setEditor(editor)
	
	$(window).resize(function() {
		editor.rerender()
		editor.renderDecorators()
	})

	/* View Dimensions
	 * TODO: improve main scrollbars: should be positioned outside content area
	 * WYSIWYG CHALLENGE: on mobile devices you don't see actual scrollbars,
	 */
	$('.toolbar.viewing-dimensions .toolbar-action').on('click', function(e) {
			var width = $(this).attr('data-width'),
				height = $(this).attr('data-height')

			var rotate = $('.viewing-dimensions').attr('data-rotate') === 'true'

			if ($(this).attr('data-active') === 'true' || $(this).is('.rotate')) {
				rotate = !rotate
				$('.viewing-dimensions').attr('data-rotate', rotate)
			} else {
				$('.viewing-dimensions .toolbar-action[data-active]').attr('data-active', null)
				$(this).attr('data-active', true)
				$('.toolbar.viewing-dimensions .rotate').attr('data-width', width).attr('data-height', height)
			}

			if (rotate) {
				var tmp = width
				width = height
				height = tmp
			}

			var contentNode = $('.content')
			contentNode.css({
				width: width,
				height: height
			})

			var contentAreaNode = $('.content-area')
			if (height === '100%') {
				contentAreaNode.removeClass('non-full')
			} else {
				contentAreaNode.addClass('non-full')
			}
		})
})
