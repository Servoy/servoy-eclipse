Challenges
Done: making empty containers visible
Handling multiple actions on the same keyboard + mouse events, based on actions inbetween
Smooth DND visuals: see http://jsfiddle.net/CKVAX/ for attempt
DND into a container that is fully occupied by another container: ideas:
 - show enlarged container structure while hovering over element that has non-visible parents (if modifier key is pressed?)
 - Allow selecting parent/child containers using keyboard
 - Pop out outline
 - Decorator that lets the use navigate parent hierarchy
Compatibility with existing editor:
- Explicit form width and height
- Parts
- 

Things to make work before demo
- Dispatching of (keyboard) events to plugins
- drag/drop elements:
  - drag/drop new element from palette, taking into account auto, default and dynamic dimensions 
  - drag/drop existing element to new location, cross container
  - tools to easily move elements into parent container if current container is 100% width/height
  - ideally element animates into location, if other elements get relocated due to this, they also animate:
      This could only be done by rendering the form twice: one version where everything is layout out normally in CSS, but this layout is made invisible 
      and another rendering where all elements are forced absolutely positioned at 0,0 and then use 2d tranform to position them.
      If changes are made, then the invisible layer is updated, after which the position of each element is determined and in the visible layer the transform is updated. 
      Downside is that 
  - delay suggestion drop location untill it looks like user wants to drop (delta of mouseMove decreases)
  - Post Drop event, so you can DND a grid and once drop a dialog appears to configure it
  - Ability to do drop restriction (Form elements only within a Form container for example) //Not applicabe within Servoy through
- Responsive Design preview (with breakpoints). Requires running editor content in iframe. See http://www.coffeecup.com/responsive-layout-maker-pro/ video for interaction. Should it be possible to also switch to iphone/ipad layout preview modes, displaying the chrome of such devices?
- Figure out interactions when dealing with selections in different (type of) containers, like DND, resize etc
- Figure out how some interactions behave:
  - Dropping elements from abs container into flow container: should remain abs positioned or go flow?
- Keyboard navigation in/out of containers
- contenteditable & resize element + decorator & single vs. doubleclick: http://stackoverflow.com/questions/4764902/jquery-suppress-click-when-dblclick-is-imminent
- Redo/undo
- Fire event with changes made
- base edited UI on model (ideally read in .frm files!)
- Generate layout HTML (with inline styling or styling in style node)
- Form Parts + Ctrl-reszing part will resize anchored elements
- Ability to create "webpages" with a footer on the bottom, but more content that fits in the viewport and then the default browser window scrollbar to scroll through content

To Fix
- Resizing anchored elements needs some more restriction checks
- left/top resize doesn't work properly: due to editor offset (padding)
- Resizing elements in (vert.) flow layout with margin left/right auto
- Margin decorator pattern does not line up between top, middle and bottom decorator (Mostly only visible when zoomed in)
- position calculations for absolute positioned elements whos positioning parent has padding goes wrong

TODOS
- Angular elements
	- http://css-tricks.com/custom-scrollbars-in-webkit/
	- http://jsfiddle.net/ZJSz4/5/
	- http://stackoverflow.com/questions/11699635/dealing-with-dom-manipulations-angularjs
	- https://docs.angularjs.org/guide/directive
	- http://www.benlesh.com/2013/08/angular-compile-how-it-works-how-to-use.html
- keyboard resize
- range select
- tab sequence
- Allowing dragging elements outside the 'form', as can be done in current editor
- panning and editor overflow (especially needed for zooming)
- overflow on containers with width/height set
- Properly handle resize of elements who's height changes when resizing their width: resize knob should stay with cursor, editor needs to be panned for that. On end of action maybe animate back to original pan position? 

Things to check
- elements can be resized so that they flow out of the editor on the left
- Layout mode of the main container (currently flow, but should be absolute by default?)
- selection decoration behavior when going out of container
- behavior of horizontally resizing centered elements
- behavior of simulataniously resizing multiple elements, where there are resizing element above the main one being resized that push the main one down as well: idea: scroll the entire viewport down, so resizeDecorator keeps sticking to the cursor (maybe force editor scrollbars to always be visible) 
- Make sure multiple instances of the editor could live in one webpage

Architectural improvements to be made
- Make containers an svelement as well, with an additional class to identify it as container, to avoid double selectors
- auto generate wrappers around replaced elements
- run Editor in an iframe to encapsulate styling and make responsive design breakpoints work
- figure out how to make sure editor is "focused" so mouse/keyboard event handlers are fired. Maybe force focus the editor div?
  - See http://stackoverflow.com/questions/497094/how-do-i-find-out-which-dom-element-has-the-focus
  - Need to focus a hidden input field, so we can also trap the input event fired when content changes due to  undo/redo 
- Have a global store of selected elements: needed to be able to set tabSequence based on selection order
- use attributes instead of classes to identify containers and elements, as to not polute the HTML and prevent CSS conflicts
- Move all different editor behaviors to plugins that register themselves with pointers when to get invoked and work with custom event dispatcher
- Allow 'plugins' to contribute decorators
- Plugins contributing 'actions', like align or distribute, going into context menu AND toolbar (disable action if not applicable to selection)
- See if somehow plugins don't have to call renderDecorators manually. Ideally setting an element as selected should show the decorators, but since the decorator is just an overlay..... Maybe MutationObservers can help with that: http://addyosmani.com/blog/mutation-observers/?utm_content=buffer559a3&utm_medium=social&utm_source=twitter.com&utm_campaign=buffer
- Have decorator plugins that implement a redraw method
- handle zoom factor and editor offset internally, so plugins don't have to bother about it: most likely have to add additional details on event + provide util methods
- use custom event to trigger stuff, for example layout
- move all logic to an Editor constructor
- provide utility functions, like:
  - set editor cursor: to be used by panning http://www.phpied.com/rendering-repaint-reflowrelayout-restyle/ or resize to have a consistent cursor whereever the mouse is
- Do all updates of overlays on a document fragment, to improve performance
- Check code for possible performance improvements: get all info first, then get documentfragment, only apply changes on it and then swap fragments
  - http://www.phpied.com/rendering-repaint-reflowrelayout-restyle/
  - http://www.stubbornella.org/content/2009/03/27/reflows-repaints-css-performance-making-your-javascript-slow/

Possible enhancements
- Option to highlight containers, using .outline-containers class on .editor
- Option to show grid column overlay
- Every element a positioned decorator, so for example margins can be shown on hover
- global array of elements and containers and extra info
- Maybe store extra info on the JQuery node
- use transitions to fade in/out decorators 
- easy support to work with images: DND them in from desktop?
- baseline alignment guides
- ability to cycle through the elements using the keyboard alone, without selecting them, using SpaceBar to select
- Resizing in non-absolute layout: instead of implementing "Resize restrictions", should the margin property be modified instead?
- Center the zoomed in editor (and still have all everything work correctly)
- In Developer allow including or custom plugins
- During lasso select pseudo highlight elements that will match the lasso
- In form part resize, pseudo highlight anchored elements that will be resized
- Ability to configure colors of decorators. Use https://github.com/bgrins/TinyColor to get different shades of a base color

Keyboard shortcut overview
- https://wiki.servoy.com/display/PMM/Servoy+Developer+Keyboard+shortcuts
- http://eclipse-ruminations.blogspot.nl/2009/03/keyboard-shortcuts-and-accessibility-in.html

On performance:
- http://www.phpied.com/rendering-repaint-reflowrelayout-restyle/
- http://www.stubbornella.org/content/2009/03/27/reflows-repaints-css-performance-making-your-javascript-slow/
- https://developer.mozilla.org/en-US/docs/Web/Guide/CSS/Writing_efficient_CSS
- http://www.paulirish.com/2009/perf/

Related technical articles
- https://developer.mozilla.org/en-US/docs/Web/Guide/CSS/Understanding_z_index
- https://developer.mozilla.org/en-US/docs/Web/Guide/CSS/Understanding_z_index/Adding_z-index
- http://reference.sitepoint.com/css/replacedelements
- https://developer.mozilla.org/en-US/docs/Web/CSS/Replaced_element
- http://odyniec.net/articles/turning-lists-into-trees/
- https://developer.mozilla.org/en-US/docs/Web/API/Element.getBoundingClientRect
- https://developer.mozilla.org/en-US/docs/Web/Guide/CSS/Flexible_boxes
- https://www.meteor.com/blog/2013/09/06/browser-events-bubbling-capturing-and-delegation
- http://css-tricks.com/absolute-relative-fixed-positioining-how-do-they-differ/

Easy development:
- Use Serclipse with DLTK JavaScript for editing files
- Commit at least to local Git repo
- In Tomcat add something along these lines to server.xml, under the host tag: <Context  docBase="C:/Users/pbakker/git/FormEditor/FormEditor/"   path="/FormEditor" />
- use a tool like LiveReload to auto refresh the browser when saving a change

Old JSBins/JSFiddles
- http://jsbin.com/fegegaxi/80/edit - Biggest prototype with zoom working
- http://jsbin.com/fifaqeti/17/edit - original resizing prototype
- http://jsbin.com/ganeloga/6/edit - jQuery Sortable explorations
- http://jsbin.com/ganeloga/25/edit - first very basic take on own "sortable" impl.

Exploration for implementing DND ourselves
- Different position values: static & absolute are 'simple', but what about relative and to a lesser extend fixed and sticky?
- Different display values:
	- block: below
	- inline-block: left/right
	- others: https://developer.mozilla.org/en-US/docs/Web/CSS/display
- Floated elements?
- Clear values?
- Collapsed margins?
- Layered elements
- Containers with full width/height content
- logic:
1 Find container element
2: find closest element

