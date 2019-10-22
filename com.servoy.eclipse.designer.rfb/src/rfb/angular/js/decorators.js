angular.module("decorators",['editor','margin','resizeknobs','menubar']).directive("decorator", function($rootScope,EDITOR_EVENTS,EDITOR_CONSTANTS,$timeout,$editorService){
	return {
	      restrict: 'E',
	      transclude: true,
	      controller: function($scope, $element, $attrs) {
			$scope.nodes = [];
			function adjustForPadding(mousePosition) {
				mousePosition.left += parseInt(angular.element($scope.glasspane.parentElement).css("padding-left").replace("px",""));
				mousePosition.top  += parseInt(angular.element($scope.glasspane.parentElement).css("padding-top").replace("px",""));
				return mousePosition;
			}
			function hasClass(element, cls) {
				return (' ' + element.className + ' ').indexOf(' ' + cls + ' ') > -1;
			}
			
			function hideDecorators() {
			    for (var i = 0; i < $scope.nodes.length; i++) {
			    	$scope.nodes[i].style.display = 'none';
			    }
			}

			function renderDecorators(selection, doNotScrollIntoView) {
				$timeout(function() {
					if (selection.length == 1) {
						// when resizing the form, the server sends a refreshGhosts message that updates the form ghost div => the selection references a stale form ghost,
						// we need to search for the real form ghost div
						var formDiv = angular.element($scope.glasspane).find("[svy-id='"+selection[0].getAttribute("svy-id")+"']");
						if (formDiv[0] && formDiv[0].getAttribute("svy-id") == selection[0].getAttribute("svy-id"))
							selection[0] = formDiv[0];
					}

					selection.forEach(function(value, index, array) {
						var currentNode = $scope.nodes[index];
						if (!currentNode) {
							currentNode = {name:'',style:{},node:value}
							$scope.nodes[index] = currentNode;
						}
						var node = $(value)

						// this node could be the angular tag (replace is false, or dynamic template) with a 0 size
						// try to look if there is a child element that is better suited.
						if (value.clientHeight == 0 && value.clientWidth == 0) {
							var children = node.children();
							if (children.length == 1 && children[0].clientHeight > 0 && children[0].clientWidth > 0) {
								node = $(children[0]);
							}
						}
						var height = node.outerHeight();
						var width = node.outerWidth();

						currentNode.name =  node.attr('name');
						currentNode.node = node;
						var ghost = $scope.getGhost(node.attr("svy-id"));
						if ($scope.isAbsoluteFormLayout()) {
							if (ghost) {			
								if (ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_COMPONENT) {
									currentNode.isResizable = {t:true, l:true, b:true, r:true};
								} else if (ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_FORM) {
									currentNode.isResizable = {t:false, l:false, b:true, r:true};
								} else {
									currentNode.isResizable = false;
								}
								
								// TODO aren't height and width already set to these? can we remove these 2 lines?
								height = node.outerHeight();
								width = node.outerWidth();
							}
							else if ($editorService.isShowingContainer() && node.attr('svy-layoutname') != undefined)
							{
								currentNode.isResizable = {t:false, l:false, b:false, r:false};
							}	
							else {
								currentNode.isResizable = {t:true, l:true, b:true, r:true};
							}
							currentNode.isContainer = false;
						} else {
							currentNode.isContainer = node.attr('svy-layoutname') != undefined;
						}

						var offset = node.offset();
						var display = 'block';
						
						if (ghost && ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_CONFIGURATION) {
							display = 'none'; // config ghosts cannot be resized; and they are already highlighted by a change in their color when selected; besides in the 'else' code below offset will not be computed correctly for them when not visible
						} else {
							if (!node.is(":visible")) {
								var beanModel = $scope.getBeanModel(node[0]);
								if (beanModel) {
									offset.top = beanModel.location.y;
									offset.left = beanModel.location.x;
								} else if (ghost) {
									offset.top = ghost.location.y;
									offset.left = ghost.location.x;
									offset = adjustForPadding(offset);
									height = ghost.size.height;
									width = ghost.size.width;
								}
							}

							// this is so that ghost elements decorators are positioned correctly
							if (node.parent().hasClass("ghostcontainer") && node.parent().parent().offset() != undefined) {
								offset.top -= node.parent().parent().offset().top;
								offset.left -= node.parent().parent().offset().left;
							}

							if (!hasClass(node.get(0),"ghost")) offset = adjustForPadding(offset);
						}

						currentNode.style = {
							height: height,
							width: width,
							top: offset.top,
							left: offset.left,
							display: display
						};
						
						currentNode.cls = "";
						if (currentNode.isContainer && $editorService.getEditor().getEditorContentRootScope().showWireframe && node.attr('svy-layoutname') && !node.attr('data-maincontainer') && node.width() > 0 && node.height() > 0)
						{
							currentNode.svytitle = node.attr('svy-title');
							currentNode.cls = "showWireframe";
							currentNode.style['--svyBackgroundColor'] = node.css('backgroundColor');	
							if (node.hasClass('maxLevelDesign'))
							{	
								//fix for IE container background, the one above is still needed for the ::before pseudoelement
								currentNode.style['backgroundColor'] = node.css('backgroundColor');		
								currentNode.cls +=" maxLevelDesign";
							}			
						}
					}); // end of foreach

					for (var i = selection.length; i<$scope.nodes.length; i++) {
						$scope.nodes[i].style.display = 'none';
					}

					if (!doNotScrollIntoView && $scope.nodes.length > 0 && selection.length > 0) {
						var ghost = $scope.getGhost($scope.nodes[0].node.attr("svy-id"));

						if (!ghost || (ghost.type != EDITOR_CONSTANTS.GHOST_TYPE_FORM && ghost.type != EDITOR_CONSTANTS.GHOST_TYPE_PART)) {
							var target = $scope.nodes[0].node.get(0);
							var targetRect = target.getBoundingClientRect();
							var toolbarBottom = $(".toolbar-area").get(0).getBoundingClientRect().bottom;
							var statusBarHeight = $(".statusbar-area").get(0).getBoundingClientRect().height;
							var resizerRight = $(".sidebar-resizer").get(0).getBoundingClientRect().right;
							var iframeOffsetTopIfPresent = 0, iframeOffsetLeftIfPresent = 0;
							if (target.ownerDocument != window.document) {
								var contentIFrameRect = $(".contentframe").get(0).getBoundingClientRect();
								iframeOffsetTopIfPresent = contentIFrameRect.top;
								iframeOffsetLeftIfPresent = contentIFrameRect.left;
							}
							
							if ((targetRect.bottom + iframeOffsetTopIfPresent < toolbarBottom + 5) || (targetRect.top + iframeOffsetTopIfPresent > window.innerHeight - statusBarHeight - 5)
									|| (targetRect.right + iframeOffsetLeftIfPresent < resizerRight + 5) || (targetRect.left + iframeOffsetLeftIfPresent > window.innerWidth - 5)) 
							{
								//target.scrollIntoView();
								// scrollintoview behaves strange in IE, it will scroll inside iframe as well, set some values manually for now
								$scope.glasspane.parentElement.scrollTop = targetRect.top-50;
								$scope.glasspane.parentElement.scrollLeft = targetRect.left-50;
								
							}
						}
					}
				});
			}
			
			$rootScope.$on(EDITOR_EVENTS.HIDE_DECORATORS, function(event, selection) {
				hideDecorators();
			})
	    	  
			$rootScope.$on(EDITOR_EVENTS.SELECTION_CHANGED, function(event, selection) {
				renderDecorators(selection);
			})
			$rootScope.$on(EDITOR_EVENTS.SELECTION_MOVED, function(event, selection) {
				renderDecorators(selection, true);
			})
			$rootScope.$on(EDITOR_EVENTS.RENDER_DECORATORS, function(event, selection) {
				renderDecorators(selection);
			})
	      },
	      templateUrl: 'templates/decorators.html',
	      replace: true
	    };
	
})
