function setupOverlay(selector) {
    //TODO: How will this work with already transformed elements
    //TODO: how to manage z-index
    //TODO: also z-index of container itself
	
	selector = selector||'#container'
    
    var node = $('#container')
    var pos = node.offset()
    pos.top = pos.top + parseFloat(node.css('border-top-width'))||0
    pos.left = pos.left + parseFloat(node.css('border-left-width'))||0
    
    console.log('Origin offset: ' + JSON.stringify(pos))
    
    var clone = node.clone()
    //TODO: positioning the clone top/left doesn't cut it in all scenario's
    clone.css({
        position: 'absolute',
        'transform-style': 'preserve-3d'/*,
        'z-index': -1,
        top: pos.top,
        left: pos.left*/
    })
    //ISSUE: cloned stuff might have nodes with ID's, which means duplicate ID's. 
    //Removing the ID's might cause styling issues
    //Maybe duplcate ID's is OK, as long as the clones appear later in the DOM. Most (all?) API's just return the first match...
    
    //CHALLENGE: how to match a descendant in the original with the descendant in the clone?
    var clonedNodes = clone.find('*')
    node.find('*').each(function(idx, el) {
        //TODO: filter out fixed positioned nodes
        //TODO: tag both node in original and clone with custom attribute, so related can be found
        $(el).attr('data-origin-id', el.id)
        //TODO: set clone descendant nodes position using tranform 
        //Assuming the order of .find(*) is the same for node and clone...
        var clonedNode = clonedNodes[idx]
        $(clonedNode).attr('data-clone-of', el.id).css({
            position: 'absolute',
            top: '0px',
            left: '0px'
        }).addClass('animate')
    })
    clone.appendTo(node.parent())
    
    node.find('*').each(function(idx, value) {
         layoutNode(value)   
    })

    //node.css('opacity', '0')
    
    var observer = new MutationObserver(handleChange);
	observer.observe(document.querySelector('#container'), {
		subtree: true,
		childList: true,
		attributes: true
	});
	
	function handleChange(changes, observer) {
        for (var i = 0; i < changes.length; i++) {
            //TODO: actual position calculation in the clone is not straight forward, as the clone is absolutely positioned and the original might not be
            console.log(changes[i].target.parentNode)
            layoutNode(changes[i].target)
        }
	}
    
    function layoutNode(node) {
        //FIXME: the cumulative top/left margins of all parents needs to be taken into account when transforming the position of nested elements, as .getBoundingClientRect() reports top/left disregarding the margin
        
        var jqmOriginalNode = $(node)
        var id = jqmOriginalNode.attr('data-origin-id')
        var clone = document.querySelector('[data-clone-of="' + id + '"]')

        var rect = node.getBoundingClientRect()   
        var rect = jqmOriginalNode.offset()
        //console.log('Processing '+ id + ' - ' + JSON.stringify(rect))
        //console.log('Top: ' + rect.top + ' - ' + pos.top + ' - ' + parseFloat(jqmOriginalNode.css('margin-top'))||0)
        
		//TODO: need to set the 3rd param of translate3D based on the z-index, see http://katydecorah.com/code/2014/01/01/z-index-and-transform/
		/* rules: https://developer.mozilla.org/en-US/docs/Web/Guide/CSS/Understanding_z_index
		 * 
		 * On layer 0:
		 * background + borders root element
		 * Descendants block elements in normal flow: static positioning && display: block
		 * Floats: float value
		 * Descendant inline-block elements in normal flow: static positioning && display: inline-block
		 * Positioned elements, in DOM order: non-static positioning
		 * transformed elements, in DOM order
		 * 
		 * opacity?
		 * will-change?
		 * 
		 * https://developer.mozilla.org/en-US/docs/Web/Guide/CSS/Understanding_z_index/The_stacking_context
		 * 
		 * Setting z-index to negative will push node below Layer 0, positiva values above layer 0
		 * z-index of children is relative to parent which has z-index set
		 * elements in the same layer abide to the rules for layer 0
		 * 
		 * 
		 */
        var css = {
            width: jqmOriginalNode.css('width'),
            height: jqmOriginalNode.css('height'),
            transform: 'translate3D(' + (rect.left - pos.left - parseFloat(jqmOriginalNode.css('margin-left'))||0) + 'px,' + (rect.top - pos.top - parseFloat(jqmOriginalNode.css('margin-top'))||0) + 'px, 0px)'
        }
        $(clone).css(css)

        //console.log(JSON.stringify(css))
        //console.log('Result: ' + $(clone).outerWidth())
    }
}

$(document).ready(function() {
    $('#flip').click(flip)
    setupOverlay() //$('#setupOverlay').click(setupOverlay)
})

var originalState = true
function flip() {
    var tl, br
    
    if (!originalState) {
        tl = $('#one')
        br = $('#two')
    } else {
        tl = $('#two')
        br = $('#one')        
    }
    
    originalState = !originalState
    
    //Using jQuery.css triggers multiple DOM updates!!!
    /*tl.css({
        top: '20px',
        left: '20px',
        bottom: 'auto',
        right: 'auto'
    })*/
    tl.attr('style',"top: 20px;right: 20px;bottom: auto;")
    
    /*br.css({
        top: 'auto',
        left: 'auto',
        bottom: '20px',
        right: '20px'
    })*/
    br.attr('style',"top: auto;left: auto;bottom: 20px;right: 20px")
    
}