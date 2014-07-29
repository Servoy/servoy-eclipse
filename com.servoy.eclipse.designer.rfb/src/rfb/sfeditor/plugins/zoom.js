//======Zoom logic=========================================
//TODO: binding with input field should be done nicer
(function() {
	function Zoom () {
		var instance = this
		var zoomFactors = [.25, .33, .5, .67, .75, .9, 1, 1.1, 1.25, 1.5, 1.75, 2, 2.5, 3, 4, 5]

//		$('#zoom input').change(function() {
//			setZoom(parseInt($(this).val()) / 100 || 1)
//		})

		this.setZoom = function(level) {
			Object.getPrototypeOf(this).zoom = level
			//$('#zoom input').val(Math.floor(zoom * 100) + '%')
			this.parts.CONTENT.css({ transform: 'scale(' + level + ')' })
			this.fire(Editor.EVENT_TYPES.ZOOM_CHANGED, level)
		}

		function zoomInOut(up) {
			var index = 0
			while (zoomFactors[index] < instance.zoom && index < zoomFactors.length) {
				index++
			}

			if (up) {
				if (zoomFactors.length == index + 1) {
					return
				}
				instance.setZoom(zoomFactors[index + 1])
			} else if (index != 0) {
				instance.setZoom(zoomFactors[index] == instance.zoom ? zoomFactors[index - 1] : zoomFactors[index])
			}
		}

		//TODO: test with different keyboard(s)
		function onKeyDown(event) {
			if (event.ctrlKey) {
				var handled = true
				switch (event.keyCode) {
					case 48: //0
						instance.setZoom(1)
						break
					case 187: //+
					case 61: //FF reports wrong values when in iframe
						zoomInOut(true)
						break;
					case 189: //-
					case 173: //FF reports wrong values when in iframe
						zoomInOut(false)
						break;
					default:
						handled = false
				}
				if (handled) {
					event.preventDefault()
					event.stopPropagation();
				}
			}
		}
		
		//TODO: specific keycodes should be registered as separate actions, not just register the onKeyDown method
		this.registerBehavior({
			event: 'keydown',
			action: onKeyDown
		})
		this.registerBehavior({
			context: Editor.ACTION_CONTEXT.EDITOR,
			event: 'keydown',
			action: onKeyDown
		})

		//FIXME: scrollwheel zoom doesn't work. Event doesn't seem to get triggered
		function onMouseWheel(e) {
			console.log('onMouseWheel')
			if (e.ctrlKey) {
				if (e.originalEvent.wheelDelta / 120 > 0) {
					zoomInOut(true)
				} else {
					zoomInOut()
				}
				e.preventDefault()
				e.stopPropagation();
			}
		}

		this.registerBehavior({
			event: 'mousewheel',
			action: onMouseWheel
		})

		this.registerBehavior({
			context: Editor.ACTION_CONTEXT.EDITOR,
			event: 'mousewheel',
			action: onMouseWheel
		})
	}
	
	Editor.registerPlugin('zoom', Zoom)		
}())