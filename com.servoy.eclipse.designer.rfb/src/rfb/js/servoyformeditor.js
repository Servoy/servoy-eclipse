$.servoy = {

	getFormLayoutGrid : function(handler) {
		$.ws.sendMessage("getFormLayoutGrid", handler)
	}
}

$(document).ready(function() {
	$.ws.onConnectedToDeveloper(function() {

		$.servoy.getFormLayoutGrid(function(layoutGrid) {
			console.log('apply form layout grid ' + layoutGrid)
			setLayoutSrc(layoutGrid)
			console.log('applied form layout grid')
		})
	});
});
