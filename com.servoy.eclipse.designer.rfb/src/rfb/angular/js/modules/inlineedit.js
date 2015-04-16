angular.module('inlineedit', ['editor']).run(function($pluginRegistry, $editorService) {
	
	$pluginRegistry.registerPlugin(function(editorScope) {
	
		editorScope.registerDOMEvent("dblclick","CONTENTFRAME_OVERLAY", function(event){
			var selection = editorScope.getSelection();
			if (selection && selection.length > 0)
			{
				var scrollX = (window.pageXOffset !== undefined) ? window.pageXOffset : document.documentElement.scrollLeft;
				var scrollY = (window.pageYOffset !== undefined) ? window.pageYOffset : document.documentElement.scrollTop;
				var clickPosition = editorScope.convertToContentPoint({x: event.pageX,y:event.pageY}); 				
				for (var i=0;i<selection.length;i++)
				{
					var node = selection[i];
					var model = editorScope.getBeanModel(node);
					if (model && (clickPosition.x >= model.location.x + scrollX && clickPosition.x <= (model.location.x + scrollX + model.size.width))
							&& (clickPosition.y >= model.location.y + scrollY && clickPosition.y <= (model.location.y + scrollY + model.size.height)))
					{
						var directEditProperty = node.getAttribute("directEditPropertyName");
						if (directEditProperty)
						{
							var obj = {};
							var absolutePoint = editorScope.convertToAbsolutePoint({x: model.location.x + scrollX,y: model.location.y + scrollY});
							var applyValue = function()
							{
								$("#directEdit").hide();
								var newValue = $("#directEdit").text();
								var oldValue = model[directEditProperty];
								if (oldValue != newValue)
								{
									var value = {};
									value[directEditProperty] = newValue;
									obj[node.getAttribute("svy-id")] = value;
									$editorService.sendChanges(obj);
								}
								$editorService.setInlineEditMode(false);
							}
							$editorService.setInlineEditMode(true);
							// double click on element
							$("#directEdit")
								.unbind('blur')
								.unbind('keyup')
								.unbind('keydown')
								.html(model[directEditProperty])
								.css({
									display: "block",
									left: absolutePoint.x,
									top: absolutePoint.y,
									width: model.size.width+"px",
									height: model.size.height+"px"
								})
								.bind('keyup',function(event)
									{
										if (event.keyCode == 27)
										{
											$("#directEdit").html(model[directEditProperty]).hide();
											$editorService.setInlineEditMode(false);
										}
										if (event.keyCode == 13)
										{
											applyValue();
										}
										if (event.keyCode == 46)
										{
											return false;
										}
									})
								.bind('keydown',function(event)
									{
										if (event.keyCode == 8)
										{
											event.stopPropagation();
										}
										if (event.keyCode == 65 && event.ctrlKey)
										{
											document.execCommand('selectAll',false,null);
										}
									})	
								.bind('blur',function()
										{
											applyValue();
										})
								.focus();
							break;
						}	
					}
				}
			}
		});
		
	});
});