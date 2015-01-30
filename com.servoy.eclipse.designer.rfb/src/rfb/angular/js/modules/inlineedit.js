angular.module('inlineedit', ['editor']).run(function($pluginRegistry, $editorService) {
	
	$pluginRegistry.registerPlugin(function(editorScope) {
	
		editorScope.registerDOMEvent("dblclick","CONTENTFRAME_OVERLAY", function(event){
			var selection = editorScope.getSelection();
			if (selection && selection.length > 0)
			{
				var clickPosition = editorScope.convertToContentPoint({x: event.pageX,y:event.pageY});
				for (var i=0;i<selection.length;i++)
				{
					var node = selection[i];
					var model = editorScope.getBeanModel(node);
					if (model && (clickPosition.x >= model.location.x && clickPosition.x <= (model.location.x + model.size.width))
							&& (clickPosition.y >= model.location.y && clickPosition.y <= (model.location.y + model.size.height)))
					{
						var directEditProperty = model["directEditPropertyName"];
						if (directEditProperty)
						{
							var obj = {};
							var absolutePoint = editorScope.convertToAbsolutePoint({x: model.location.x,y: model.location.y});
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