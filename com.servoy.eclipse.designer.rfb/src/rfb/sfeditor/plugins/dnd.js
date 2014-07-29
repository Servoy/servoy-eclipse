//======Drag from Palette logic=========================================
//$('.palette .element').on('mousedown', function(event) {
//		var elementOffset = $(this).offset()
//		var clickOffset = {
//			top: event.pageY - elementOffset.top,
//			left: event.pageX - elementOffset.left
//		}
//
//		var clone = $(this).clone()
//		clone.css({
//			top: elementOffset.top,
//			left: elementOffset.left,
//			display: 'block',
//			position: 'absolute',
//			'pointer-events': 'none'
//		})
//		$('body').append(clone)
//
//		function endDND() {
//			//TODO: this should also get called on pressing escape
//			$(document).off('mousemove.dnd')
//			$('.palette').off('mouseout.dnd')
//			//TODO: clone should be inserted or deleted
//			clone.css({
//				display: 'none'
//			})
//		}
//
//		function cancelDND(e) {
//			if (e.keyCode == 27) {
//				$(document).trigger('mouseup:dnd')
//			}
//		}
//
//		//TODO: all added handlers should be removed once the DND is finished
//		$(document).on('mousemove.dnd', function(e) {
//				clone.css({
//					top: e.pageY - clickOffset.top,
//					left: e.pageX - clickOffset.left,
//				}).one('mouseup.dnd', endDND).bind("keydown.dnd", cancelResize)
//			})
//	})
//
//$('.palette').on('mouseout.drag', function(e) {
//		//Show Outline
//	})