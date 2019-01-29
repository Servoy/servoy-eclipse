
tinymce.PluginManager.add('tabindex', function(editor) {

  editor.on('PostRender', function() {
    var iframe, tabindex = editor.settings.tabindex || 'element';

    // -1 => don't set a tabindex
    if (tabindex === -1) {
      return;
    }

    // 'element' => grab the tabindex from the element
    if (tabindex === 'element') {
      tabindex = editor.dom.getAttrib(editor.getElement(), 'tabindex', null);
    }

    // make sure we have a tabindex
    if (!tabindex) {
      return;
    }

    // get the iframe so we can set the tabindex
    iframe = document.getElementById(editor.id + "_ifr");

    // make sure we have an iframe
    if (!iframe) {
      return;
    }

    editor.dom.setAttrib(iframe, 'tabindex', tabindex);
  });

});
