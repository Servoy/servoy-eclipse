
(function() {
  tinymce.create('tinymce.plugins.ResizeToContainer', {
    init : function(ed, url) {
      var totalTime = 0;

      if (ed.getParam('fullscreen_is_enabled'))
        return;

      /**
       * This method gets executed each time the editor needs to resize.
       */
      function resize() {
        var iframe = tinymce.DOM.get(ed.id + '_ifr');
        if (iframe)
        {
          var tinymceToolbar = iframe.parentNode.parentNode.childNodes[0];
          var svyWrapper = tinymce.DOM.get(ed.id).parentNode.parentNode;
          var resizeHeight = ( svyWrapper.offsetHeight - tinymceToolbar.offsetHeight - 5 ) + 'px';
          var oldSize = tinymce.DOM.getStyle(iframe,'height');
          // Resize content element
          if (resizeHeight !== oldSize){
            svyWrapper.children[0].style.paddingRight = '5px'
            tinymce.DOM.setStyle(iframe, 'height', resizeHeight);
          };
        }
      };

      // Add appropriate listeners for resizing
      ed.on('change', resize);
      ed.on('show', resize);

      var MAX_VISIBILITY_CHECK_NR = 10;
      function callWhenVisible(jqElement, callback, tryNumber) {
        if (!jqElement.hidden) {
          callback();
        }
        else {
          if(tryNumber < MAX_VISIBILITY_CHECK_NR) {
            setTimeout(function() {
              callWhenVisible(jqElement, callback, tryNumber + 1);
            }, 200);
          }
        }
      }

      ed.on('init', function() {
        var iframe = tinymce.DOM.get(ed.id + '_ifr');
        if (iframe)
        {
          var mainDiv = iframe.parentNode.parentNode.parentNode;
          callWhenVisible(mainDiv, resize, 1);
        }
      });
      function loadContent(){
        resize();
      };
      ed.on('LoadContent', resize);
      // also recalculate on window resize
      window.addEventListener('resize', resize);
      // Register the command so that it can be invoked by using tinyMCE.activeEditor.execCommand('mceResizeToContainer');
      ed.addCommand('mceResizeToContainer', resize);
    },

    getInfo : function() {
      return {
        longname : 'Resize to Container',
        author : 'Laurian Vostinar',
        authorurl : 'http://www.servoy.com',
        version : tinymce.majorVersion + "." + tinymce.minorVersion
      };
    }
  });

  // Register plugin
  tinymce.PluginManager.add('resizetocontainer', tinymce.plugins.ResizeToContainer);
})();
