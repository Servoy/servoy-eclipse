/*
 * Marquee Select logic
 * 
 * TODO: nice addition would be to show 'selection' on elements while still dragging the marquee: should remove 'selected' indicators again if  is falls outside marquee againIn
 */
(function() {

	function MarqueePlugin() {
		var instance = this
		
		var marquee = $('<div class="marquee-select" style="position: absolute; display: none">')
		instance.parts.DECORATORS.append(marquee)

		var startPosition

		function startMarquee(e) {
			startPosition = {
				x: e.pageX,
				y: e.pageY
			}

			marquee.css({
				top: e.pageY,
				left: e.pageX,
				display: 'block',
				width: 0,
				height: 0
			})

			instance.setContentGlasspaneVisibility(true)
			instance.parts.GLASSPANE.css({
				cursor: 'crosshair'
			})
			//Workaround: binding directly to the window here, so the event fires properly cross windows
			$(instance.contentWindow).on({'mousemove.marquee': updateMarquee,
				'mouseup.marquee': endMarquee})
				
			e.preventDefault()
			e.stopPropagation()	
		}
		
		function updateMarquee(e) {
			marquee.css({
				height: Math.abs(e.pageY - startPosition.y),
				width: Math.abs(e.pageX - startPosition.x),
				top: e.pageY < startPosition.y ? e.pageY : startPosition.y,
				left: e.pageX < startPosition.x ? e.pageX : startPosition.x
			})
		}

		function endMarquee(e) {
			$(instance.contentWindow).off('mousemove.marquee mouseup.marquee')
			marquee.css({ display: 'none' })

			instance.setContentGlasspaneVisibility(false)
			instance.parts.GLASSPANE.css({
				cursor: ''
			})
			var p1 = {top: Math.min(startPosition.y, e.pageY), left: Math.min(startPosition.x, e.pageX)}
			var p2 = {left: Math.max(startPosition.x, e.pageX), top: Math.max(startPosition.y, e.pageY)}
			var elements = instance.getElementsByRectangle(p1, p2)
			instance.plugins.selection.setSelection(elements)
		}

//	Disabled, as conflicts with mousedown for select
//		this.registerBehavior({
//			context: Editor.ACTION_CONTEXT.CONTENT,
//			event: 'mousedown',
//			sequence: 'SHIFT',
//			action: function(e) { //TODO: modifier checks should be handled inside Editor Action dispatch
//				if (e.shiftKey) {
//					startMarquee(e)
//				}
//			}
//		})
	}
	
	Editor.registerPlugin('marquee', MarqueePlugin)
}())