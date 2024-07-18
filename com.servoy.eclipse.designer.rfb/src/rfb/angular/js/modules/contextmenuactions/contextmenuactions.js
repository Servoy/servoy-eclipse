angular.module('contextmenuactions',['contextmenu','editor'])
.value("SHORTCUT_IDS",
{	
	SET_TAB_SEQUENCE_ID: "com.servoy.eclipse.designer.rfb.settabseq",
	SAME_WIDTH_ID: "com.servoy.eclipse.designer.rfb.samewidth",
	SAME_HEIGHT_ID: "com.servoy.eclipse.designer.rfb.sameheight",
	TOGGLE_ANCHORING_TOP_ID: "com.servoy.eclipse.designer.rfb.anchorTop",
	TOGGLE_ANCHORING_RIGHT_ID: "com.servoy.eclipse.designer.rfb.anchorRight",
	TOGGLE_ANCHORING_BOTTOM_ID: "com.servoy.eclipse.designer.rfb.anchorBottom",
	TOGGLE_ANCHORING_LEFT_ID: "com.servoy.eclipse.designer.rfb.anchorLeft",
	BRING_TO_FRONT_ONE_STEP_ID: "com.servoy.eclipse.designer.rfb.bringtofrontonestep",
	SEND_TO_BACK_ONE_STEP_ID: "com.servoy.eclipse.designer.rfb.sendtobackonestep",
	BRING_TO_FRONT_ID: "com.servoy.eclipse.designer.rfb.bringtofront", 
	SEND_TO_BACK_ID: "com.servoy.eclipse.designer.rfb.sendtoback",
	OPEN_SCRIPT_ID: "com.servoy.eclipse.ui.OpenFormJsAction",
	OPEN_SUPER_SCRIPT_ID: "com.servoy.eclipse.designer.rfb.openscripteditor",
	GROUP_ID: "com.servoy.eclipse.designer.rfb.group",
	UNGROUP_ID: "com.servoy.eclipse.designer.rfb.ungroup",
	OPEN_FORM_HIERARCHY_ID: "com.servoy.eclipse.ui.OpenFormHierarchyAction"
})
.run(function($rootScope, $pluginRegistry,$contextmenu, $editorService,EDITOR_EVENTS,SHORTCUT_IDS,EDITOR_CONSTANTS,$q){
	$pluginRegistry.registerPlugin(function(editorScope) {
		var selection = null;
		var beanAnchor = 0;
		$rootScope.$on(EDITOR_EVENTS.SELECTION_CHANGED, function(event, sel) {
			selection = sel;
			if (selection && selection.length == 1)
			{
				var node = selection[0];
				var beanModel = editorScope.getBeanModel(node);
				if (beanModel)
				{
					beanAnchor = beanModel.anchors;
				}
			}
		});
		var hasSelection = function(selectionSize)
		{
			if (selection && selection.length > 0 && (selectionSize == undefined || selection.length == selectionSize))
				return true;
			return false;
		};
		
		var isAnchored = function(anchor)
		{
			if (selection && selection.length == 1)
			{
				if(beanAnchor == 0)
					 beanAnchor = 1 + 8; // top left
				if ((beanAnchor & anchor) == anchor)
				{
					return true;
				}
			}
			return false;
		}
		var setAnchoring = function(anchor, opposite){
			var selection = editorScope.getSelection();
			if (selection && selection.length == 1)
			{
				var obj = {};
				var node = selection[0];
				var beanModel = editorScope.getBeanModel(node);
				if (beanModel)
				{
					var beanAnchor = beanModel.anchors;
					if(beanAnchor == 0)
						 beanAnchor = 1 + 8; // top left
					if ((beanAnchor & anchor) == anchor)
					{
						// already exists, remove it
						beanAnchor = beanAnchor - anchor;
						if ((beanAnchor & opposite) != opposite) beanAnchor += opposite;
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
		}
		
		var setCssAnchoring = function(top, right, bottom, left){
			var selection = editorScope.getSelection();
			if (selection && selection.length > 0) 
			{
				var selected = selection.map(function(node){ return node.getAttribute("svy-id")});
				$editorService.setCssAnchoring(selected, {"top":top, "right":right, "bottom":bottom, "left":left});
			}
		}
		
		var promise = $editorService.getShortcuts();
		var pr = $editorService.getSuperForms();
		$q.all([promise, pr]).then(function (result){
		
			var shortcuts = result[0];
			var forms = result[1];
			$contextmenu.add({
				text: "Revert Form",
				getItemClass: function() { 
					if (editorScope.isDirty === true){ 
						return "enabled";
						} else {
							return "disabled";
						}
					}, 
				execute:function()
				{
					$("#contextMenu").hide();
					$editorService.executeAction('revertForm');
				}
			});
			$contextmenu.add({
				text: "Set Tab Sequence",
				getIconStyle: function(){ return {'background-image':"url(images/th_horizontal.png)"};},
				shortcut: shortcuts[SHORTCUT_IDS.SET_TAB_SEQUENCE_ID],
				getItemClass: function() { if (!editorScope.getSelection() || editorScope.getSelection().length < 2) return "disabled";},
				execute:function(){
					$editorService.executeAction('setTabSequence');	
				}
			});
			
			$contextmenu.add({
				text: "Add",
			});
			
			if (editorScope.isAbsoluteFormLayout()){
        			// sizing
        			var sizingActions = [];
        			
        			sizingActions.push(
        					{
        						text: "Same Width",
        						getIconStyle: function(){ return {'background-image':"url(images/same_width.png)"};},
        						shortcut: shortcuts[SHORTCUT_IDS.SAME_WIDTH_ID],
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
        						getIconStyle: function(){ return {'background-image':"url(images/same_height.png)"};},
        						shortcut: shortcuts[SHORTCUT_IDS.SAME_HEIGHT_ID],
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
        			if (!editorScope.isCSSPositionFormLayout()){
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
        						shortcut: shortcuts[SHORTCUT_IDS.TOGGLE_ANCHORING_TOP_ID],
        						getItemClass: function() { if (!hasSelection(1) || !editorScope.isAbsoluteFormLayout()) return "disabled";},
        						execute:function()
        						{
        							setAnchoring(1, 4);
        						}
        					}
        				);
        			
        			anchoringActions.push(
        					{
        						text: "Right",
        						getIconStyle: function(){ if(isAnchored(2)) return {'background-image':"url(images/check.png)"};},
        						shortcut: shortcuts[SHORTCUT_IDS.TOGGLE_ANCHORING_RIGHT_ID],
        						getItemClass: function() { if (!hasSelection(1) || !editorScope.isAbsoluteFormLayout()) return "disabled";},
        						execute:function()
        						{
        							setAnchoring(2, 8);
        						}
        					}
        				);
        			
        			anchoringActions.push(
        					{
        						text: "Bottom",
        						getIconStyle: function(){ if(isAnchored(4)) return {'background-image':"url(images/check.png)"};},
        						shortcut: shortcuts[SHORTCUT_IDS.TOGGLE_ANCHORING_BOTTOM_ID],
        						getItemClass: function() {  if (!hasSelection(1) || !editorScope.isAbsoluteFormLayout()) return "disabled";},
        						execute:function()
        						{
        							setAnchoring(4, 1);
        						}
        					}
        				);			
        			
        			anchoringActions.push(
        					{
        						text: "Left",
        						getIconStyle: function(){ if(isAnchored(8)) return {'background-image':"url(images/check.png)"};},
        						shortcut: shortcuts[SHORTCUT_IDS.TOGGLE_ANCHORING_LEFT_ID],
        						getItemClass: function() { if (!hasSelection(1) || !editorScope.isAbsoluteFormLayout()) return "disabled";},
        						execute:function()
        						{
        							setAnchoring(8, 2);
        						}
        					}
        				);
        			}
        			else
        			{
        				anchoringActions.push(
            					{
            						text: "Top/Left",
            						getIconStyle: function(){ return {'background-image':"url(images/anchor-top-left.png)"}},
            						execute:function()
            						{
            							setCssAnchoring('0', '-1', '-1', '0');
            						}
            					}
            				);
        				anchoringActions.push(
            					{
            						text: "Top/Right",
            						getIconStyle: function(){ return {'background-image':"url(images/anchor-top-right.png)"}},
            						execute:function()
            						{
            							setCssAnchoring('0', '0', '-1', '-1');
            						}
            					}
            				);
        				anchoringActions.push(
            					{
            						text: "Top/Left/Right",
            						getIconStyle: function(){ return {'background-image':"url(images/anchor-top-left-right.png)"}},
            						execute:function()
            						{
            							setCssAnchoring('0', '0', '-1', '0');
            						}
            					}
            				);
        				anchoringActions.push(
            					{
            						text: "Bottom/Left",
            						getIconStyle: function(){ return {'background-image':"url(images/anchor-bottom-left.png)"}},
            						execute:function()
            						{
            							setCssAnchoring('-1', '-1', '0', '0');
            						}
            					}
            				);
        				
        				anchoringActions.push(
            					{
            						text: "Bottom/Right",
            						getIconStyle: function(){ return {'background-image':"url(images/anchor-bottom-right.png)"}},
            						execute:function()
            						{
            							setCssAnchoring('-1', '0', '0', '-1');
            						}
            					}
            				);
        				anchoringActions.push(
            					{
            						text: "Other...",
            						execute:function()
            						{
            							var selection = editorScope.getSelection();
            							if (selection && selection.length > 0) 
            							{
            								var selected = selection.map(function(node){ return node.getAttribute("svy-id")});
            								$editorService.setCssAnchoring(selected);
            							}
            						}
            					}
            				);
        			}
        			
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
        						shortcut: shortcuts[SHORTCUT_IDS.BRING_TO_FRONT_ONE_STEP_ID],
        						getItemClass: function() { if (!hasSelection()) return "disabled";},
        						execute: function() 
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
        						shortcut: shortcuts[SHORTCUT_IDS.SEND_TO_BACK_ONE_STEP_ID],
        						getItemClass: function() { if (!hasSelection()) return "disabled";},
        						execute: function()
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
        						shortcut: shortcuts[SHORTCUT_IDS.BRING_TO_FRONT_ID],
        						getItemClass: function() { if (!hasSelection()) return "disabled";},
        						execute: function() 
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
        						shortcut: shortcuts[SHORTCUT_IDS.SEND_TO_BACK_ID],
        						getItemClass: function() { if (!hasSelection()) return "disabled";},
        						execute: function()
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
        			
        			var groupingActions = [];
        			groupingActions.push(
        					{
        						text: "Group",
        						getIconStyle: function(){ return {'background-image':"url(images/group.png)"}},
        						getItemClass: function() {if (!editorScope.getSelection() || editorScope.getSelection().length < 2) return "disabled";},
        						shortcut: shortcuts[SHORTCUT_IDS.GROUP_ID],
        						execute: function()
        						{
        							$("#contextMenu").hide();
        							$editorService.executeAction('createGroup');
        						}
        					}
        				);
        			
        			groupingActions.push(
        					{
        						text: "Ungroup",
        						getIconStyle: function(){ return {'background-image':"url(images/ungroup.png)"}},
        						getItemClass: function() {
        							if (!hasSelection()) return "disabled";
        							//at least one selected element should be a group
        							var selection = editorScope.getSelection();
        							for (var i = 0; i < selection.length; i++)
        							{
        								var ghost = editorScope.getGhost(selection[i].getAttribute("svy-id"));
        								if (ghost && ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_GROUP)
        								{
        									return;
        								}
        							}
        							return "disabled";
        						},
        						shortcut: shortcuts[SHORTCUT_IDS.UNGROUP_ID],
        						execute: function()
        						{
        							$("#contextMenu").hide();
        							$editorService.executeAction('clearGroup');
        						}
        					}
        				);
        			
        			$contextmenu.add(
        					{
        						text: "Grouping",
        						subMenu: groupingActions,
        						getItemClass: function() { return "dropdown-submenu"}
        					}
        			);
			}
			else {//this is an Responsive Layout 
				$contextmenu.add(
						{
							text: "Zoom in",
							getIconStyle: function(){ return {'background-image':"url(images/zoom_in.png)"}},
							getItemClass: function() { if (hasSelection(1)) return "enabled"; else return "disabled"}, 
							execute:function()
							{
								$("#contextMenu").hide();
								$editorService.executeAction('zoomIn');
							}
						}
				);
			}
			
			if (!editorScope.isAbsoluteFormLayout() || $editorService.isShowingContainer())
			{
				$contextmenu.add(
						{
							text: "Zoom out",
							getIconStyle: function(){ return {'background-image':"url(images/zoom_out.png)"}},
							getItemClass: function() { if ($editorService.isShowingContainer()) return "enabled"; else return "disabled"}, 
							execute:function()
							{
								$("#contextMenu").hide();
								$editorService.executeAction('zoomOut');
							}
						}
				);
			}	
			
			$contextmenu.add(
					{
						getItemClass: function() { return "divider"}
					}
				);
			
			// deprecated
			/*$contextmenu.add(
				{
					text: "Save as template ...",
					getIconStyle: function(){ return {'background-image':"url(toolbaractions/icons/template.png)"}},
					execute:function()
					{
						$("#contextMenu").hide();
						$editorService.openElementWizard('saveastemplate');
					}
				}
			);	*/		
				
			var openAction = {
					text: "Open in Script Editor",
					getIconStyle: function(){ return {'background-image':"url(images/js.png)"}},
					shortcut: forms.length > 1 ? shortcuts[SHORTCUT_IDS.OPEN_SUPER_SCRIPT_ID] : shortcuts[SHORTCUT_IDS.OPEN_SCRIPT_ID],
					execute:function()
					{
						$("#contextMenu").hide();
						$editorService.executeAction('openScript');
					},
					getItemClass: function() { if (!editorScope.isFormComponent()) return "enabled"; else return "disabled"}
				};
				
			if (forms.length > 1 && !editorScope.isFormComponent()){
				var superFormsActions = [];
				for (var i =0; i<forms.length; i++) {
					superFormsActions.push({
						text: forms[i]+".js",
						getIconStyle: function(){ return {'background-image':"url(images/js.png)"}},
						shortcut: i==0 ? shortcuts[SHORTCUT_IDS.OPEN_SCRIPT_ID] : undefined,
						form: forms[i],
						execute:function()
						{
							$("#contextMenu").hide();
							$editorService.executeAction('openScript', {'f': this.form});
						}
					});
				}
				openAction.subMenu = superFormsActions;
				openAction.getItemClass = function() { return "dropdown-submenu";};
			}
			$contextmenu.add(openAction);
			
			$contextmenu.add(
				{
					text: "Delete",
					getIconStyle: function(){ return {'background-image':"url(images/delete.gif)"}},
					execute:function()
					{
						$("#contextMenu").hide();
						//46 == delete key
						$editorService.keyPressed({"keyCode":46});
					}
				}
			);
			
			$contextmenu.add(
					{
						text: "Open Form Hierarchy",
						getIconStyle: function(){ return {'background-image':"url(images/forms.png)"}},
						getItemClass: function() { if (!hasSelection(1)) return "enabled";}, 
						shortcut: shortcuts[SHORTCUT_IDS.OPEN_FORM_HIERARCHY_ID],
						execute:function()
						{
							$("#contextMenu").hide();
							$editorService.executeAction('openFormHierarchy');
						}
					}
				);
		});
	});
	
});