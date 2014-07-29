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
		EDITOR: this.editor.find('.content-area'),
		CONTENT: this.editor.find('.content'),
		DECORATORS: this.editor.find('.decorators'),
		GLASSPANE: this.editor.find('.contentframe-overlay')
	}
	Object.freeze(this.parts)

	this.zoom = 1

	this.contentWindow = this.editor.find('.contentframe')[0].contentWindow

	this.contentDocument = null

	this.selection = []
	
	this.fire = function() {
		this.editor.trigger.apply(this.editor, arguments)
	}
	
	var intermediateChanges = []
	var contentChangeListenerEnabled = true
	this.disableContentChangeListener = function(state) {
		if (!contentChangeListenerEnabled && 
			!state && 
			intermediateChanges.length) {
			instance.fire(Editor.EVENT_TYPES.CONTENT_CHANGED, [intermediateChanges])
		}
		contentChangeListenerEnabled = !state
	}
	
	$(this.editor).on('documentReady.content', function(event, contentDocument) {
		instance.contentDocument = contentDocument
		
		//Call all plugin function in the context of the editor instance
		instance.plugins = {}
		
		var pluginNames = Object.keys(Editor.pluginRegistry)
		for (var i = 0; i < pluginNames.length; i++) {
			var pluginName = pluginNames[i]
			
			var scope = Object.create(instance)
			instance.plugins[pluginName] = scope
			Editor.pluginRegistry[pluginName].apply(scope);
			
			//Check if plugin exposed public API. If not, remove the plugin
			if (!Object.keys(instance.plugins[pluginName]).length) {
				delete instance.plugins[pluginName]
			}
		}
		
		/* 
		 * Monitoring content changes using MutationObserver: http://addyosmani.com/blog/mutation-observers
		 *
		 * Not all changes ought to be monitored, for example:
		 * - Intermediate changes while moving or resizing (operation might even ne canceled completely)
		 * - Dummy insertion while DND-ing
		 */
		function contentChangeObserver(changes, observer) {
			instance.renderDecorators()
			
			if (contentChangeListenerEnabled) {
				instance.fire(Editor.EVENT_TYPES.CONTENT_CHANGED, [changes])
			} else {
				//TODO: change implementation to just store all arrays with MutationRecords while ContentChangeListener is disabled 
				//and then do all processing when it gets enabled again, to bring down performance overhead
				return
				//record the changes internally, to trigger Editor.EVENT_TYPES.CONTENT_CHANGED when the CHangeListener gets enabled again
				var intermediateRecord, handled
				var MutationRecordProto = {
					type: null,
					target: null,
					removedNodes: [],
					previousSibling: null,
					oldValue: null,
					nextSibling: null,
					attributeNamespace: null,
					attributeName: null,
					addedNodes: []
				}
				changes: for (var i = 0; i < changes.length; i++) {
					handled = false
					var mutationRecord = changes[i]
					
					switch (mutationRecord.type) {
						case 'attributes':
							/* Attribute changes always cause one MutationRecord per attribute change per element
							 * 
							 * If an attribute change comes in, find if there's already an intermediate change for the node and that attribute.
							 * If there is not, store the node/attribute combination and record the old value for final comparison and store the MutationRecord
							 * If there is, compare the new value against the previously recorded old value. If equal, remove node, if not update current value
							 */
							for (var j = 0; j < intermediateChanges.length; j++) {
								intermediateRecord = intermediateChanges[j]
								if(intermediateRecord.type === 'attributes' && 
									intermediateRecord.target === mutationRecord.target &&
								 	intermediateRecord.attributeName === mutationRecord.attributeName) {
									if (mutationRecord.target.getAttribute(mutationRecord.attributeName) === intermediateRecord.oldValue) {
										intermediateChanges.splice(j)
									} else {
										intermediateChanges[j].oldValue = intermediateRecord.oldValue
									}
									continue changes
								 }
							}
							
							var rec = Object.create(MutationRecordProto)
							rec.type = 'attributes'
							rec.target = mutationRecord.target
							rec.oldValue = mutationRecord.oldValue
							rec.attributeName = mutationRecord.attributeName
							
							intermediateChanges[intermediateChanges.length] = rec
							break
						case 'childList':
							//TODO: test this logic with DND implementation, when inserting placeholders during the Drag
							/* Use case of this branch: filtering out the placeholder nodes added during DND
							 * 
							 * Mutation Records are distinct based on:
							 * 1: target
							 * 2: previousSibling
							 * 
							 * 
							 */
							for (var j = 0; j < intermediateChanges.length; j++) {
								 intermediateRecord = intermediateChanges[j]
								 if (intermediateRecord.type === 'childList' && 
								 	intermediateRecord.target === mutationRecord.target &&
								 	intermediateRecord.previousSibling === mutationRecord.previousSibling) {
								 	
							 		/* For added/removedNodes, the intermediateChanges need to be checked and updated
							 		 * 
							 		 * Previous adds and removes need to be removed from intermediate history
							 		 * The add/remove needs to be added to the intermediateChanges
							 		 */	
								 	if (mutationRecord.addedNodes.length) {
								 		if (!intermediateRecord.hasOwnProperty('addedNodes')) {
									 		intermediateRecord.addedNodes = []
								 		}
								 		for (var k = 0; k < mutationRecord.addedNodes.length; k++) {
								 			var addedNode = mutationRecord.addedNodes[k]
								 			for (var l = 0; l < intermediateRecord.addedNodes.length; l++) {
								 				if (intermediateRecord.addedNodes[l] === addedNode) {
								 					intermediateRecord.addedNodes.slice(l)
								 				}
								 			}
								 			for (l = 0; l < intermediateRecord.removedNodes.length; l++) {
								 				if (intermediateRecord.removedNodes[l] === addedNode) {
								 					intermediateRecord.removedNodes.slice(l)
								 				}
								 			}
								 			intermediateRecord.addedNodes[intermediateRecord.addedNodes.length] = addedNode
									 	}
								 	} else { //Must be removedNodes
								 		if (!intermediateRecord.hasOwnProperty('removedNodes')) {
									 		intermediateRecord.removedNodes = []
								 		}
								 		for (var k = 0; k < mutationRecord.removedNodes.length; k++) {
								 			var removedNode = mutationRecord.removedNodes[k]
								 			for (var l = 0; l < intermediateRecord.removedNodes.length; l++) {
								 				if (intermediateRecord.removedNodes[l] === removedNode) {
								 					intermediateRecord.removedNodes.slice(l)
								 				}
								 			}
								 			for (var l = 0; l < intermediateRecord.addedNodes.length; l++) {
								 				if (intermediateRecord.addedNodes[l] === removedNode) {
								 					intermediateRecord.addedNodes.slice(l)
								 				}
								 			}
								 			intermediateRecord.removedNodes[intermediateRecord.removedNodes.length] = removedNode
									 	}
								 	}
								 	continue changes
								 }
							}
							
							var rec = Object.create(MutationRecordProto)
							rec.type = 'childList'
							
							intermediateChanges[intermediateChanges.length] = rec
							
							break
						default:
					}
				}
			}
		}
		
		new MutationObserver(contentChangeObserver).observe(instance.contentDocument.querySelector('.sfcontent'), {
			subtree: true,
			childList: true,
			attributes: true,
			attributeOldValue: true
		});
			
		//TODO: this should be externalized into plugin
		var hoverDecorator = $('<div style="position: absolute; display: none; outline: 1px solid black; pointer-events: none"></div>')
		instance.parts.DECORATORS.append(hoverDecorator)
		
		//Setup to fire hoverIntent event
		$(instance.contentDocument).find('.sfcontent').hoverIntent({
		    over: function() {
				var rect = this.getBoundingClientRect()
				var css = {
					top: rect.top,
					left: rect.left,
					width: this.offsetWidth,
					height: this.offsetHeight,
					display: 'block'
				}
				hoverDecorator.css(css)
		    },
		    out: function() {
		    	hoverDecorator.css('display', 'none')
		    },
		    selector: '.svelement',
			interval: 100,
			timeout: 100
		});

		instance.fire(Editor.EVENT_TYPES.CONTENT_READY)
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

Editor.prototype.getElementsByRectangle = function(p1, p2, percentage) {
	var i,
		element,
		rect,
		overlapX,
		overlapY,
		nodes = this.contentDocument.querySelectorAll(Editor.ACTION_CONTEXT.ELEMENT),
		matchedElements = []
		
	for (i = 0; i < nodes.length; i++) {
		element = nodes[i]
		rect = element.getBoundingClientRect()
		
		if (percentage == undefined || percentage == 100) { //Element must be fully enclosed
			if (p1.top <= rect.top && p1.left <= rect.left && p2.top >= rect.top + element.clientHeight && p2.left >= rect.left + element.clientWidth) {
				matchedElements.push(element)
			}
		} else {
			overlapX = Math.max(0, Math.min(p2.left, rect.left + element.clientWidth) - Math.max(p1.left, rect.left))
			overlapY = Math.max(0, Math.min(p2.top, rect.top + element.clientHeight) - Math.max(p1.top, rect.top))

			if ( ( (element.clientWidth * element.clientHeight) / 100) * percentage < (overlapX * overlapY)) {
				matchedElements.push(element)
			}
		}
	}
	return matchedElements
}

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
	this.selection.forEach(function(value, index, array) {
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

Editor.prototype.registerDecorator = function(context, markup) {
	//TODO: store the markup, for renderDecorators to use
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

Editor.pluginRegistry = {}

Editor.registerPlugin = function(name, plugin){
	if (Editor.pluginRegistry.hasOwnProperty(name)) {
		console.log('Duplicate plugin name used: ' + name + '\nIgnoring registration')
		return
	}
	Editor.pluginRegistry[name] = plugin
}

//------------Old code, yet to be made obsolete--------------
var randomId = 0
var resizeDecoratorIncludeMargin = false
var editorId = 'editor'
var zoom = 1
//TODO: Remove this cached editorOffset: offset changes when editor is scrolled
var editorOffset

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

	editorOffset = $('#' + editorId + ' .content').offset()

	//======Event Dispatcher logic=========================================
	//TODO: implement
	//TODO: events on decorators should not "bubble""
	//TODO: provide help option showing all available interaction commands (see JSFiddle's display for idea's)
	//   Global:
	//   Ctrl-Z == Undo
	//   Ctrl-Y == Redo
	//   SpaceBar-MouseDown > MouseMove == Pan Editor
	//   Ctrl-+ == Zoom In
	//   Ctrl-- == Zoom Out
	//   Ctrl-0 == Reset Zoom
	//   Alt-Mousewheel == Zoom
	//   Alt-MouseDown > MouseMove == Forced Marquee Select

	//   On main canvas
	//   MouseDown > MouseMove == Marquee Select

	//   On containers:

	//   On elements:
	//   MouseDown == Select element
	//   Ctrl-MouseDown == Toggle selected state
	//   Shift-MouseDown == Range Select

	//   On DecoratorOverlays

	//   On decorators: (maybe these can just be handled directly on the decorator)
	//   knobs: MouseDown > MouseMove == Resize

	//   TODO: DND from Palette/Move

	var actions = [{ category: 'history', description: 'Undo', event: 'undo.history', modifier: 'ctrl', action: 'z' }, { category: 'history', description: 'Redo', event: 'redo.history', modifier: 'ctrl', action: 'y' }, { category: 'zoom', description: 'Zoom In', event: 'zoom_in.zoom', modifier: 'ctrl', action: '+' }, { category: 'zoom', description: 'Zoom Out', event: 'zoom_out.zoom', modifier: 'ctrl', action: '-' }, { category: 'zoom', description: 'Reset Zoom', event: 'reset_zoom.zoom', modifier: 'ctrl', action: '0' }, { category: 'selection', description: 'Select Element', event: 'select_element.selection', modifier: 'ctrl', action: 'MouseDown' }, { category: 'move', description: 'Move Up Small Step', event: 'move_up_small_step.move', modifier: 'ctrl', action: 'up' }, { category: 'move', description: 'Move Right Small Step', event: 'move_right_small_step.move', modifier: 'ctrl', action: 'right' }, { category: 'move', description: 'Move Down Small Step', event: 'move_down_small_step.move', modifier: 'ctrl', action: 'down' }, { category: 'move', description: 'Move Left Small Step', event: 'move_left_small_step.move', modifier: 'ctrl', action: 'left' }, { category: 'layering', description: 'Bring 1 Level Forward', event: 'bring_1_level_forward.layering', modifier: 'ctrl', action: '[' }, { category: 'layering', description: 'Send 1 Level Backwards', event: 'send_1_level_backwards.layering', modifier: 'ctrl', action: ']' }, { category: 'selection', description: 'Select All Elements', event: 'select_all_elements.selection', modifier: 'ctrl', action: 'a' }, { category: 'selection', description: 'Deselect Last Selected Element', event: 'deselect_last_selected_element.selection', modifier: 'ctrl', action: 'space' }, { category: '', description: 'Paste', event: 'paste.', modifier: 'ctrl', action: 'v' }, { category: '', description: 'Copy', event: 'copy.', modifier: 'ctrl', action: 'c' }, { category: '', description: 'Cut', event: 'cut.', modifier: 'ctrl', action: 'x' }, { category: 'grouping', description: 'Group Selected Elements', event: 'group_selected_elements.grouping', modifier: 'ctrl', action: 'g' }, { category: 'grouping', description: 'Ungroup Selected Elements', event: 'ungroup_selected_elements.grouping', modifier: 'ctrl', action: 'u' }, { category: 'tabbing', description: 'Set Tab Sequence', event: 'set_tab_sequence.tabbing', modifier: 'ctrl', action: 't' }, { category: '', description: '', event: '.', modifier: '', action: '' }, { category: 'selection', description: 'Range Select', event: 'range_select.selection', modifier: 'shift', action: 'MouseDown' }, { category: 'move', description: 'Move Up 1px', event: 'move_up_1px.move', modifier: 'shift', action: 'up' }, { category: 'move', description: 'Move Right 1px', event: 'move_right_1px.move', modifier: 'shift', action: 'right' }, { category: 'move', description: 'Move Down 1px', event: 'move_down_1px.move', modifier: 'shift', action: 'down' }, { category: 'move', description: 'Move Left 1px', event: 'move_left_1px.move', modifier: 'shift', action: 'left' }, { category: 'sizing', description: 'Same Width', event: 'same_width.sizing', modifier: 'shift', action: 'w' }, { category: 'sizing', description: 'Same Height', event: 'same_height.sizing', modifier: 'shift', action: 'y' }, { category: 'anchoring', description: 'Anchor Top', event: 'anchor_top.anchoring', modifier: 'shift', action: '-' }, { category: 'anchoring', description: 'Anchor Right', event: 'anchor_right.anchoring', modifier: 'shift', action: '*' }, { category: 'anchoring', description: 'Anchor Bottom', event: 'anchor_bottom.anchoring', modifier: 'shift', action: '+' }, { category: 'anchoring', description: 'Anchor Left', event: 'anchor_left.anchoring', modifier: 'shift', action: '/' }, { category: 'tabbing', description: 'Previous Element', event: 'previous_element.tabbing', modifier: 'shift', action: 'tab' }, { category: '', description: '', event: '.', modifier: '', action: '' }, { category: 'tabbing', description: 'Next Element', event: 'next_element.tabbing', modifier: '', action: 'tab' }, { category: 'layering', description: 'Bring to Front', event: 'bring_to_front.layering', modifier: '', action: '[' }, { category: 'layering', description: 'Send to Back', event: 'send_to_back.layering', modifier: '', action: ']' }, { category: 'delete', description: 'Delete Selected Elements', event: 'delete_selected_elements.delete', modifier: '', action: 'del' }, { category: 'move', description: 'Move Up 1px', event: 'move_up_1px.move', modifier: '', action: 'up' }, { category: 'move', description: 'Move Right 1px', event: 'move_right_1px.move', modifier: '', action: 'right' }, { category: 'move', description: 'Move Down 1px', event: 'move_down_1px.move', modifier: '', action: 'down' }, { category: 'move', description: 'Move Left 1px', event: 'move_left_1px.move', modifier: '', action: 'left' }, { category: 'selection', description: 'Select Element', event: 'select_element.selection', modifier: '', action: 'MouseDown' }, { category: 'selection', description: 'Forced Marquee Select', event: 'forced_marquee_select.selection', modifier: 'alt', action: 'MouseDown' }, { category: 'selection', description: 'Marquee Select', event: 'marquee_select.selection', modifier: '', action: 'MouseDown' }, { category: 'zoom', description: 'Zoom In/Out', event: 'zoom_in/out.zoom', modifier: 'ctrl', action: 'MouseWheel' }, { category: 'pan', description: 'Pan Editor', event: 'pan_editor.pan', modifier: 'space', action: 'MouseDown' },
		]

	function codeFor(text) {
		var code
		//TODO: extend with more codes
		switch (text) {
			case 'up':
				code = 38;
				break;
			case 'right':
				code = 39;
				break;
			case 'down':
				code = 40;
				break;
			case 'left':
				code = 37;
				break;
			case 'space':
				code = 32;
				break;
			case 'tab':
				code = 9;
				break;
			case 'del':
				code = 46;
				break;
			case 'esc':
				code = 27;
				break;
			default:
				code = text.toUpperCase().charCodeAt(0)
		}
		return code
	}

	//TODO: this data below should be filled based on the config
	var customModifierKeyCodes = [32]
	var actionMap = {}
	for (var i = 0; i < actions.length; i++) {
		var action = actions[i]
		var keyCode = codeFor(action.action)
		if (!actionMap.hasOwnProperty(keyCode)) {
			actionMap[keyCode] = []
		}
		actionMap[keyCode].push(action)
	}

	var currentModifiers = []
	$(document).keydown(function(e) {
		if (e.keyCode == 16 || e.keyCode == 17 || e.keyCode == 18) {
			return; //Not interested in default modifier keys
		}
		if (customModifierKeyCodes.indexOf(e.keyCode)) {
			currentModifiers.push(e.keyCode)
		}

		if (actionMap.hasOwnProperty(e.keyCode)) {
			var actions = actionMap[e.keyCode]
			for (var i = 0; i < actions.length; i++) {
				if (actions[i].modifier) {
					//TODO: figure out how to properly handle all interaction when clicking on an element (forced lasso, duplicate, move, (de)select)
					//lasso/move/duplicate happen only after mousemove threshold
				}
			}
			console.log('action detected: ' + JSON.stringify(actionMap[e.keyCode]))
		}
	}).keyup(function(e) {
		currentModifiers.slice(currentModifiers.indexOf(e.keyCode), 1)
	})
	//.mousedown(function(e){}).mousewheel(function(e){})

//	$(document).on('mousedown.marquee', function(e) {
//			console.log('start marquee')
//			if (e.ctrlKey && e.shiftKey) {
//				startMarquee(e)
//			}
//		})
})