angular.module('designsize',['toolbar','editor']).run(function($rootScope, $toolbar, TOOLBAR_CATEGORIES, $pluginRegistry, $editorService){
	var setSize = function(width, height,fixedSize) {
		editor.setContentSize(width, height, fixedSize);
		lastWidth = btnCustomWidth.text =  width;
		lastHeight = btnCustomHeight.text = height;
		$editorService.setFormFixedSize({"width": lastWidth, "height" : lastHeight});
	}
	
	var btnDesktopSize = {
			text: "Desktop",
			icon: "designsize/desktop_preview.png",
			enabled: true,
			onclick: function() { 
				editor.setContentSizeFull(true);
				lastWidth = btnCustomWidth.text = editor.getFormInitialWidth();
				lastHeight = btnCustomHeight.text = "auto";
				$editorService.setFormFixedSize({"width": lastWidth, "height" : lastHeight});
			},
		};

	var isPortrait = true;
	var lastClicked;
	var btnTabletSize = {
			text: "Tablet",
			icon: "designsize/tablet_preview.png",
			enabled: true,
			onclick: function() {
				if(lastClicked == "Tablet") isPortrait = !isPortrait;
				if(isPortrait)  {
					setSize("768px", "1024px",true);
				}
				else {
					setSize("1024px", "768px",true);
				}
				lastClicked = "Tablet";
			},
		};	

	var btnMobileSize = {
			text: "Phone",
			icon: "designsize/mobile_preview.png",
			enabled: true,
			onclick: function() {
				if(lastClicked == "Phone") isPortrait = !isPortrait;
				if(isPortrait) {
					setSize("320px", "568px",false);
				}
				else {
					setSize("568px", "320px",false);
				}
				lastClicked = "Phone";
			},
		};
	
	var btnCustomWidth;// will be defined when we have the editor scope	
	var lastWidth;
	var lastHeight = "auto";
	var btnCustomHeight = {
			text: "auto",
			tooltip: "Fixed design height",
			enabled: true,
			onclick: function(selection) {
				setSize(lastWidth, selection,true);
			},
			list: [{"text": "auto"}, {"text": "480px"}, {"text": "640px"}, {"text": "1024px"}, {"text": "2048px"}],
			onselection: function(selection) {
				this.onclick(selection);
				return selection;
			}
		};

//	var btnRotate = {
//			text: "Rotate",
//			icon: "designsize/gear-1-64x64.png",
//			enabled: true,
//			onclick: function() {
//				isPortrait = !isPortrait;
//				var size = editor.getContentSize();
//				editor.setContentSize(size.height, size.width);
//			},
//		};

	var editor;
	$pluginRegistry.registerPlugin(function(editorScope) {
		editor = editorScope;
		if(!editor.isAbsoluteFormLayout()) {
			lastWidth = editor.getFormInitialWidth();
			btnCustomWidth = {
				text: lastWidth,
				tooltip: "Fixed design width",
				enabled: true,
				onclick: function(selection) {
					editor.setContentSize(selection, lastHeight, true);
					lastWidth = selection;
					$editorService.setFormFixedSize({"width" : lastWidth});
				},
				list: [{"text": "320px"}, {"text": "568px"}, {"text": "640px"}, {"text": "768px"}, {"text" : lastWidth}],
				onselection: function(selection) {
					this.onclick(selection);
					return selection;
				},
				faIcon: "fa fa-times fa-lg"
			};
			
			$toolbar.add(btnDesktopSize, TOOLBAR_CATEGORIES.STICKY);
			$toolbar.add(btnTabletSize, TOOLBAR_CATEGORIES.STICKY);
			$toolbar.add(btnMobileSize, TOOLBAR_CATEGORIES.STICKY);
			$toolbar.add(btnCustomWidth, TOOLBAR_CATEGORIES.STICKY);
			$toolbar.add(btnCustomHeight, TOOLBAR_CATEGORIES.STICKY);
			//$toolbar.add(btnRotate, TOOLBAR_CATEGORIES.STICKY);
			
			
			var formSizePromise = $editorService.getFormFixedSize();
			formSizePromise.then(function(result) {
				lastHeight = result.height ? result.height : lastHeight;
				btnCustomHeight.text = lastHeight;
				lastWidth = result.width ? result.width : lastWidth;
				btnCustomWidth.text = lastWidth;
				if (result.height || result.width) {
					editor.contentLoaded.then(function() {
						setSize(lastWidth, lastHeight, true);
					});
				}
			});
		}
	});
	
	
});