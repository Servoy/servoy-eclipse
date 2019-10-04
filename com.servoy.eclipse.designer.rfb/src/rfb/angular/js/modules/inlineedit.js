angular.module('inlineedit', ['editor']).run(['$pluginRegistry', '$editorService', '$selectionUtils', function($pluginRegistry, $editorService, $selectionUtils) {
	$pluginRegistry.registerPlugin(function(editorScope) {

		var utils = $selectionUtils.getUtilsForScope(editorScope);

		function handleDirectEdit(nodeId, absolutePoint, property, propertyValue) {
			var obj = {};
			var applyValue = function() {
				angular.element("#directEdit").hide();
				var newValue = angular.element("#directEdit").text();
				var oldValue = propertyValue;
				if (oldValue != newValue && !(oldValue === null && newValue === "")) {
					var value = {};
					value[property] = newValue;
					obj[nodeId] = value;
					$editorService.sendChanges(obj);
				}
				$editorService.setInlineEditMode(false);
			}
			$editorService.setInlineEditMode(true);
			// double click on element
			angular.element("#directEdit")
				.unbind('blur')
				.unbind('keyup')
				.unbind('keydown')
				.html(propertyValue)
				.css({
					display: "block",
					left: absolutePoint.x,
					top: absolutePoint.y,
					width: absolutePoint.width + "px",
					height: absolutePoint.height + "px"
				})
				.bind('keyup', function(event) {
					if (event.keyCode == 27) {
						angular.element("#directEdit").html(propertyValue).hide();
						$editorService.setInlineEditMode(false);
					}
					if (event.keyCode == 13) {
						applyValue();
					}
					if (event.keyCode == 46) {
						return false;
					}
				})
				.bind('keydown', function(event) {
					if (event.keyCode == 8) {
						event.stopPropagation();
					}
					if (event.keyCode == 65 && event.ctrlKey) {
						document.execCommand('selectAll', false, null);
					}
				})
				.bind('blur', function() {
					applyValue();
				})
				.focus();
		}

		editorScope.registerDOMEvent("dblclick", "CONTENTFRAME_OVERLAY", function(event) {
			var selection = editorScope.getSelection();
			if (selection && selection.length > 0) {
				var eventNode = utils.getNode(event, true);
				if (eventNode) {
					for (var i = 0; i < selection.length; i++) {
						var node = selection[i];
						if (eventNode === node) {
							var directEditProperty = node.getAttribute("directEditPropertyName");
							if (directEditProperty) {
								var nodeId = node.getAttribute("svy-id");
								$editorService.getComponentPropertyWithTags(nodeId, directEditProperty).then(function(propertyValue) {
									if (node.clientHeight == 0 && node.clientWidth == 0 && node.firstElementChild) {
										node = node.firstElementChild;
									}
									var absolutePoint = editorScope.convertToAbsolutePoint({
										x: node.getBoundingClientRect().left,
										y: node.getBoundingClientRect().top
									});
									absolutePoint.width = node.getBoundingClientRect().right - node.getBoundingClientRect().left;
									absolutePoint.height = node.getBoundingClientRect().bottom - node.getBoundingClientRect().top;
									handleDirectEdit(nodeId, absolutePoint, directEditProperty, propertyValue);
								});
								break;
							}
						}
					}
				}
			}
		});

	});
}]);
