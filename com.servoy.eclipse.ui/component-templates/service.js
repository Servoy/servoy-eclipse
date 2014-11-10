angular.module('${MODULENAME}',['servoy'])
.factory("${MODULENAME}",function($services) 
{
	var scope = $services.getServiceScope('${MODULENAME}');
	return {
		helloworld: function(text) {
			alert("helloworld: " + scope.model.text + text);
		}
	}
})
.run(function($rootScope,$services)
{
	var scope = $services.getServiceScope('${MODULENAME}')
	scope.$watch('model', function(newvalue,oldvalue) {
	// handle state changes
		console.log(newvalue)
}, true);
})