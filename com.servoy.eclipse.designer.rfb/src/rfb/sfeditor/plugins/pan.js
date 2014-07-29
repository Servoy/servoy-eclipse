  //======Pan logic=========================================  
  //TODO: Incorporate MouseDown into the logic as well
  //TODO: see if using CSS Transform to "pan" the editor gives better performance
  //TODO: see if it feels better to pan up straight away after having panned down way past last scrollDown
  var panStarted = false
  $('#' + editorId).on('start.panning', '*', function(e) {
    if (!panStarted && e.keyCode == 32) { //SpaceBar
      panStarted = true
      $('#' + editorId + ' .decorationGlassPane').css({
        display: 'block', 
        cursor: 'move', 
        cursor: 'all-scroll'
      })
      var panStartPosition, scrollStart
      $('#' + editorId).on('mousemove.panning', function(e) {
        console.log('panning..')
        if (!panStartPosition) {
            panStartPosition = {
              top: e.pageY,
              left: e.pageX
            } 
            
            scrollStart = {
              top: $('#' + editorId).scrollTop(),
              left: $('#' + editorId).scrollLeft()
            }
        }
 
        //TODO: make logic below to do threshold checking a helper function
        if (e.pageX > panStartPosition.left + 5 || 
            e.pageX < panStartPosition.left - 5 ||
            e.pageY > panStartPosition.top + 5 || 
            e.pageY < panStartPosition.top - 5) {
          $('#' + editorId).scrollLeft(Math.max(0, scrollStart.left + (panStartPosition.left - e.pageX)))
          $('#' + editorId).scrollTop(Math.max(0, scrollStart.top + (panStartPosition.top - e.pageY)))
        }
      }).one('keyup', function(){
        $('#' + editorId).off('mousemove.panning')
        $('#' + editorId + ' .decorationGlassPane').css('display', 'none')
        panStarted = false
      })
    }
  })