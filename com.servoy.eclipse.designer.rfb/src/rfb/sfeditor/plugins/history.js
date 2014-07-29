  //======Undo/Redo logic=========================================  
  var history = {
    undo: [],
    redo: []
  }
  
  //Binding to document, as that seems the way to be able to capture events normally consumed by the browser
  $(document).keydown(function (e) {
      var evtobj = window.event? event : e
      var handled = false
      if (evtobj.ctrlKey) {
        switch (evtobj.keyCode) {
          case 90: //Ctrl-Z
            console.log('Undo')
            alert('undo')
            //TODO: implement Undo
            handled = true
            break;
          case 89: //Ctrl-Y
            console.log('Redo')
            //TODO: implement Redo
            handled = true
            break;
          default: break;
        }
      } else {
        switch(evtobj.keyCode) {
          case 32:
            
            //TODO: while this seems to work, it is triggered repeatedly while SpaceBar is don and that should be avoided somehow...
            var e = $.extend({}, evtobj)
            e.type = 'start.panning'
            $(evtobj.target).trigger(e)
            handled = true
            break;
          default:
            break
        }
      }
    
      if (handled) {
          evtobj.preventDefault()
          evtobj.stopImmediatePropagation()
      }
  })
  
  $('#' + editorId).keypress(function(event) {
    console.log('snd: ' + event.keyCode)
    if (event.ctrlKey) {
      
      switch(event.keyCode) {
        case 0:
          break;
        default:
          break
      }
    }
  })