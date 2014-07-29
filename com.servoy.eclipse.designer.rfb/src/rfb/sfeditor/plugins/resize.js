//======Resizing logic=========================================
(function() {
	function ResizePlugin() {
		var instance = this
		//TODO: support original size/positioning metrics
		//TODO: set proper resize cursor over elements
		var elements, resizeStartPosition, additionalElementInfo
		var RESIZE_DIRECTIONS = {
			TOP: 1,
			RIGHT: 2,
			BOTTOM: 4,
			LEFT: 8
		}

		function endResize(event) {
			elements = additionalElementInfo = resizeStartPosition = null;
			instance.parts.GLASSPANE.off('mousemove.resize keydown.resize').css({
				display: 'none',
				cursor: ''
			})
			instance.disableContentChangeListener(false)
		}

		function doResize(event) {
			if (!resizeStartPosition) {
				return
			}

			var yDiff = (event.pageY - resizeStartPosition.y) / zoom
			var xDiff = (event.pageX - resizeStartPosition.x) / zoom

			for (var i = 0; i < elements.length; i++) {
				var extraInfo = additionalElementInfo[i]
				if (extraInfo.enabled) {
					var node = $(elements[i])
					var css = {}

					if (extraInfo.resizeDirections & RESIZE_DIRECTIONS.TOP) {
						if (extraInfo.top + yDiff >= 0 && extraInfo.top + yDiff <= extraInfo.top + extraInfo.height) {
							var topChanged = false

							if (extraInfo.css.top !== 'auto') {
								css.top = extraInfo.top + yDiff - parseFloat(0 + node.css('marginTop'))
								topChanged = true
							}

							if (extraInfo.css.bottom === 'auto' || !topChanged) {
								css.height = extraInfo.height - yDiff
							}
						}
					}

					if (extraInfo.resizeDirections & RESIZE_DIRECTIONS.RIGHT) {
						if (true) { //TODO: needs checks here to prevent negative size etc.
							var rightChanged = false
							if (extraInfo.css.right && extraInfo.css.right !== 'auto') {
								css.right = parseFloat(0 + extraInfo.css.right) - xDiff
//								css.right = node.parent().outerWidth() - (extraInfo.left + extraInfo.width + xDiff + parseFloat(0 + node.css('marginLeft')) + parseFloat(0 + node.css('marginRight')))
								rightChanged = true
							}

							if (extraInfo.css.left === 'auto' || !rightChanged) {
								css.width = extraInfo.width + xDiff
							}
						}
					}

					if (extraInfo.resizeDirections & RESIZE_DIRECTIONS.BOTTOM) {
						var bottomChanged = false

						if (extraInfo.css.bottom && extraInfo.css.bottom !== 'auto') {
							css.bottom = parseFloat(0 + extraInfo.css.bottom) - yDiff
							//css.bottom = node.parent().outerHeight() - (extraInfo.top + extraInfo.height + yDiff + parseFloat(0 + node.css('marginTop')) + parseFloat(0 + node.css('marginBottom')))
							bottomChanged = true
						}

						if (extraInfo.css.top === 'auto' || !bottomChanged) {
							css.height = extraInfo.height + yDiff
						}
					}

					if (extraInfo.resizeDirections & RESIZE_DIRECTIONS.LEFT) {
						if (extraInfo.left + xDiff >= 0 && extraInfo.left + xDiff <= extraInfo.left + extraInfo.width) {
							var leftChanged = false

							if (extraInfo.css.left !== 'auto') {
								css.left = extraInfo.left + xDiff - parseFloat(0 + node.css('marginLeft'))
								leftChanged = true
							}

							if (extraInfo.css.right === 'auto' || !leftChanged) {
								css.width = extraInfo.width - xDiff
							}
						}
					}

					node.css(css)
					extraInfo.decoratorOverlay.css(css)
				}
			}
		}

		function cancelResize(e) {
			if (e.keyCode == 27) {
				for (var i = 0; i < elements.length; i++) {
					var extra = additionalElementInfo[i]
					var css = {
						width: extra.w,
						height: extra.h
					}
					$(this).css(css)
					extra.d.css(css)					
				}
				instance.fire('mouseup.resize')
			}
		}

		this.parts.CONTENT.on('mousedown', ".knob", function(event) {
			var knob = $(this)
			if (knob.hasClass('disabled')) {
				return
			}

			//Add overlay to always show correct resize cursor while resizing
			instance.parts.GLASSPANE.css({
				display: 'block',
				cursor: knob.css('cursor')
			})
			instance.disableContentChangeListener(true)

			//Get the class of the knob through which the resize takes place,
			//used to lookup the matching knob on each element to resize, to check restrictions
			var knobClass = knob.attr('class')

			elements = instance.selection
			
			//Cache needed info for all selected elements
			additionalElementInfo = new Array(elements.length)
			for (var i = 0; i < elements.length; i++) {
				var el = elements[i], $el = $(elements[i])
				
				var decorator = instance.parts.DECORATORS.find('div[svy-decorator-for="' + el.id + '"]')

				//Find the knob through which the resize takes place on each selected element
				var resizeKnob = decorator.children('[class="' + knobClass + '"]')
				var resizeRestrictions = 0
				if (resizeKnob.length) {
					resizeRestrictions |= resizeKnob.hasClass('t') ? RESIZE_DIRECTIONS.TOP : 0
					resizeRestrictions |= resizeKnob.hasClass('r') ? RESIZE_DIRECTIONS.RIGHT : 0
					resizeRestrictions |= resizeKnob.hasClass('b') ? RESIZE_DIRECTIONS.BOTTOM : 0
					resizeRestrictions |= resizeKnob.hasClass('l') ? RESIZE_DIRECTIONS.LEFT : 0
				}

				//Store original dimensions + info on available resize options against selection
				//FIXME: t & l use .position, which is the pos rel. to first  absolute container parent. Should use .offset, minus #content offset.
				//Workaround to get the real CSS value of top/right/bottom/left: by hiding the element the auto value gets returned is set, instead of a pixel value
				var currentDisplay = $el.css('display')
				el.style.display = 'none'
				var currentCSS = $el.css(['width',
				'height',
				'top',
				'right',
				'bottom',
				'left',
				'position',
				'float',
				'display',
				'margin-top',
				'margin-right',
				'margin-bottom',
				'margin-left'])
				el.style.display = currentDisplay
				
				additionalElementInfo[i] = {
					width: $el.outerWidth(),
					height: $el.outerHeight(),
					top: $el.position().top,
					left: $el.position().left,
					enabled: resizeKnob.length > 0,
					resizeDirections: resizeRestrictions,
					decoratorOverlay: decorator,
					css: currentCSS 
				}
			}

			//Store original position where resize originated (needed for delta calculation)
			resizeStartPosition = {
				x: event.pageX,
				y: event.pageY
			}
			
			//Bind needed events, to be removed again on MouseUp
			//TODO: cancelResize keybinding not working as glasspane is active I guess
			instance.parts.GLASSPANE.on({
				'mousemove.resize': doResize,
				'keydown.resize': cancelResize,
				'mouseup.resize': endResize
			})
		})
	}
	
	Editor.registerPlugin('resize', ResizePlugin)
}())

