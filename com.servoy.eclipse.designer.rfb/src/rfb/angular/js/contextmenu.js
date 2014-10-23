angular.module("contextmenu",[]).directive("contextmenu", function($editorService){
	return {
		restrict: 'E',
		transclude: true,
		controller: function($scope, $element, $attrs) {

			$("body").on("click", function(e) {
				$("#contextMenu").hide();
			})
			$("body").on("contextmenu", function(e) {
				$scope.$digest();
				$("#contextMenu")
					.css({
						display: "block",
						left: e.pageX,
						top: e.pageY
					})
				return false;
			});
			$scope.hasSelection = function()
			{
				var selection = $scope.getSelection();
				if (selection && selection.length == 1)
					return true;
				return false;
			}
			$scope.isAnchored = function(anchor)
			{
				var selection = $scope.getSelection();
				if (selection && selection.length == 1)
				{
					var formState = $scope.getFormState();
					var node = selection[0];
					var name = node.getAttribute("name");
					var beanModel = formState.model[name];
					var beanAnchor = beanModel.anchors;
					if(beanAnchor == 0)
						 beanAnchor = 1 + 8; // top left
					if ((beanAnchor & anchor) == anchor)
					{
						return true;
					}
				}
				return false;
			}
			var setAnchoring = function(anchor){
				var selection = $scope.getSelection();
				if (selection && selection.length == 1)
				{
					var formState = $scope.getFormState();
					var obj = {};
					var node = selection[0];
					var name = node.getAttribute("name");
					var beanModel = formState.model[name];
					var beanAnchor = beanModel.anchors;
					if(beanAnchor == 0)
						 beanAnchor = 1 + 8; // top left
					if ((beanAnchor & anchor) == anchor)
					{
						// already exists, remove it
						beanAnchor = beanAnchor - anchor;
					}
					else
					{
						beanAnchor = beanAnchor + anchor;
					}
					beanModel.anchors = beanAnchor;
					obj[node.getAttribute("svy-id")] = {anchors:beanModel.anchors}
					$editorService.sendChanges(obj);
				}
			}
			var actions = [];
			
			// anchoring
			var anchoringActions = [];
			
			anchoringActions.push(
					{
						text: "Top",
						getIconStyle: function(){
							if($scope.isAnchored(1))
							{
								return {'background-image':"url(images/check.png)"};
							}
							return null;
						},
						shortcut: "Shift+-",
						getItemClass: function() { if (!$scope.hasSelection()) return "disabled";},
						execute:function()
						{
							setAnchoring(1);
						}
					}
				);
			
			anchoringActions.push(
					{
						text: "Right",
						getIconStyle: function(){ if($scope.isAnchored(2)) return {'background-image':"url(images/check.png)"};},
						shortcut: "Shift+*",
						getItemClass: function() { if (!$scope.hasSelection()) return "disabled";},
						execute:function()
						{
							setAnchoring(2);
						}
					}
				);
			
			anchoringActions.push(
					{
						text: "Bottom",
						getIconStyle: function(){ if($scope.isAnchored(4)) return {'background-image':"url(images/check.png)"};},
						shortcut: "Shift++",
						getItemClass: function() {  if (!$scope.hasSelection()) return "disabled";},
						execute:function()
						{
							setAnchoring(4);
						}
					}
				);
			
			anchoringActions.push(
					{
						text: "Left",
						getIconStyle: function(){ if($scope.isAnchored(8)) return {'background-image':"url(images/check.png)"};},
						shortcut: "Shift+/",
						getItemClass: function() { if (!$scope.hasSelection()) return "disabled";},
						execute:function()
						{
							setAnchoring(8);
						}
					}
				);
			
			actions.push(
					{
						text: "Anchoring",
						subMenu: anchoringActions,
						getItemClass: function() { return "dropdown-submenu";}
					}
				);
			
			//arrange
			var arrangeActions = [];
			
			arrangeActions.push(
					{
						text: "Bring forward",
						getIconStyle: function(){ return {'background-image':"url(images/bring_forward.png)"}},
						shortcut: "[",
						getItemClass: function() { if (!$scope.hasSelection()) return "disabled";},
						execute:function()
						{
							$("#contextMenu").hide();
							$editorService.executeAction('openScript');
						}
					}
				);
			
			arrangeActions.push(
					{
						text: "Send backward",
						getIconStyle: function(){ return {'background-image':"url(images/send_backward.png)"}},
						shortcut: "]",
						getItemClass: function() { if (!$scope.hasSelection()) return "disabled";},
						execute:function()
						{
							$("#contextMenu").hide();
							$editorService.executeAction('openScript');
						}
					}
				);
			
			arrangeActions.push(
					{
						text: "Bring to front",
						getIconStyle: function(){ return {'background-image':"url(images/bring_to_front.png)"}},
						shortcut: "Ctrl+[",
						getItemClass: function() { if (!$scope.hasSelection()) return "disabled";},
						execute:function()
						{
							$("#contextMenu").hide();
							$editorService.executeAction('openScript');
						}
					}
				);
			
			arrangeActions.push(
					{
						text: "Send to back",
						getIconStyle: function(){ return {'background-image':"url(images/send_to_back.png)"}},
						shortcut: "Ctrl+]",
						getItemClass: function() { if (!$scope.hasSelection()) return "disabled";},
						execute:function()
						{
							$("#contextMenu").hide();
							$editorService.executeAction('openScript');
						}
					}
				);
			
			actions.push(
					{
						text: "Arrange",
						subMenu: arrangeActions,
						getItemClass: function() { return "dropdown-submenu"}
					}
				);
			
			actions.push(
					{
						getItemClass: function() { return "divider"}
					}
				);
			
			actions.push(
				{
					text: "Open in Script Editor",
					getIconStyle: function(){ return {'background-image':"url(images/js.gif)"}},
					shortcut: "Ctrl+Shift+Z",
					execute:function()
					{
						$("#contextMenu").hide();
						$editorService.executeAction('openScript');
					}
				}
			);
			$scope.actions = actions;
		},
		templateUrl: 'templates/contextmenu.html',
		replace: true
	};
})