angular.module('contextmenuactions',['contextmenu','editor']).run(function($rootScope, $pluginRegistry,$contextmenu, $editorService){

	$pluginRegistry.registerPlugin(function(editorScope) {
		var hasSelection = function()
		{
			var selection = editorScope.getSelection();
			if (selection && selection.length == 1)
				return true;
			return false;
		};
		
		var isAnchored = function(anchor)
		{
			var selection = editorScope.getSelection();
			if (selection && selection.length == 1)
			{
				var formState = editorScope.getFormState();
				var node = selection[0];
				var name = node.getAttribute("name");
				var beanModel = formState.model[name];
				if (beanModel)
				{
					var beanAnchor = beanModel.anchors;
					if(beanAnchor == 0)
						 beanAnchor = 1 + 8; // top left
					if ((beanAnchor & anchor) == anchor)
					{
						return true;
					}
				}
			}
			return false;
		}
		var setAnchoring = function(anchor){
			var selection = editorScope.getSelection();
			if (selection && selection.length == 1)
			{
				var formState = editorScope.getFormState();
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
		
		$contextmenu.add(
				{
					text: "Set Tab Sequence",
					getIconStyle: function(){ return {'background-image':"url(images/th_horizontal.gif)"};},
					shortcut: "Ctrl+T",
					getItemClass: function() { if (!editorScope.getSelection() || editorScope.getSelection().length < 2) return "disabled";},
					execute:function()
					{
						$editorService.executeAction('setTabSequence');
					}
				}
			);
		
		// sizing
		var sizingActions = [];
		
		sizingActions.push(
				{
					text: "Same Width",
					getIconStyle: function(){ return {'background-image':"url(images/same_width.gif)"};},
					shortcut: "Shift+W",
					getItemClass: function() { if (!editorScope.getSelection() || editorScope.getSelection().length < 2) return "disabled";},
					execute:function()
					{
						$editorService.sameSize(true);
					}
				}
			);
		
		sizingActions.push(
				{
					text: "Same Height",
					getIconStyle: function(){ return {'background-image':"url(images/same_height.gif)"};},
					shortcut: "Shift+H",
					getItemClass: function() { if (!editorScope.getSelection() || editorScope.getSelection().length < 2) return "disabled";},
					execute:function()
					{
						$editorService.sameSize(false);
					}
				}
			);
		
		$contextmenu.add(
				{
					text: "Sizing",
					subMenu: sizingActions,
					getItemClass: function() { return "dropdown-submenu";}
				}
			);
		
		// anchoring
		var anchoringActions = [];
		
		anchoringActions.push(
				{
					text: "Top",
					getIconStyle: function(){
						if(isAnchored(1))
						{
							return {'background-image':"url(images/check.png)"};
						}
						return null;
					},
					shortcut: "Shift+-",
					getItemClass: function() { if (!hasSelection() || !editorScope.isAbsoluteFormLayout()) return "disabled";},
					execute:function()
					{
						setAnchoring(1);
					}
				}
			);
		
		anchoringActions.push(
				{
					text: "Right",
					getIconStyle: function(){ if(isAnchored(2)) return {'background-image':"url(images/check.png)"};},
					shortcut: "Shift+*",
					getItemClass: function() { if (!hasSelection() || !editorScope.isAbsoluteFormLayout()) return "disabled";},
					execute:function()
					{
						setAnchoring(2);
					}
				}
			);
		
		anchoringActions.push(
				{
					text: "Bottom",
					getIconStyle: function(){ if(isAnchored(4)) return {'background-image':"url(images/check.png)"};},
					shortcut: "Shift++",
					getItemClass: function() {  if (!hasSelection() || !editorScope.isAbsoluteFormLayout()) return "disabled";},
					execute:function()
					{
						setAnchoring(4);
					}
				}
			);
		
		anchoringActions.push(
				{
					text: "Left",
					getIconStyle: function(){ if(isAnchored(8)) return {'background-image':"url(images/check.png)"};},
					shortcut: "Shift+/",
					getItemClass: function() { if (!hasSelection() || !editorScope.isAbsoluteFormLayout()) return "disabled";},
					execute:function()
					{
						setAnchoring(8);
					}
				}
			);
		
		$contextmenu.add(
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
					getItemClass: function() { if (!hasSelection()) return "disabled";},
					execute:function()
					{
						$("#contextMenu").hide();
						$editorService.executeAction('z_order_bring_to_front_one_step');
					}
				}
			);
		
		arrangeActions.push(
				{
					text: "Send backward",
					getIconStyle: function(){ return {'background-image':"url(images/send_backward.png)"}},
					shortcut: "]",
					getItemClass: function() { if (!hasSelection()) return "disabled";},
					execute:function()
					{
						$("#contextMenu").hide();
						$editorService.executeAction('z_order_send_to_back_one_step');
					}
				}
			);
		
		arrangeActions.push(
				{
					text: "Bring to front",
					getIconStyle: function(){ return {'background-image':"url(images/bring_to_front.png)"}},
					shortcut: "Ctrl+[",
					getItemClass: function() { if (!hasSelection()) return "disabled";},
					execute:function()
					{
						$("#contextMenu").hide();
						$editorService.executeAction('z_order_bring_to_front');
					}
				}
			);
		
		arrangeActions.push(
				{
					text: "Send to back",
					getIconStyle: function(){ return {'background-image':"url(images/send_to_back.png)"}},
					shortcut: "Ctrl+]",
					getItemClass: function() { if (!hasSelection()) return "disabled";},
					execute:function()
					{
						$("#contextMenu").hide();
						$editorService.executeAction('z_order_send_to_back');
					}
				}
			);
		
		$contextmenu.add(
				{
					text: "Arrange",
					subMenu: arrangeActions,
					getItemClass: function() { return "dropdown-submenu"}
				}
			);
		
		$contextmenu.add(
				{
					getItemClass: function() { return "divider"}
				}
			);
		
		$contextmenu.add(
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
	});
	
});