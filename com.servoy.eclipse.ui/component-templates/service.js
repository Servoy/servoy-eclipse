angular.module('${NAME}',['servoy'])
.factory("${NAME}",function($services) 
{
	var scope = $services.getServiceScope('${NAME}');
	return {
		helloworld: function(text) {
			alert("helloworld: " + scope.model.text + text);
		}
	}
})
.run(function($rootScope,$services)
{
	var scope = $services.getServiceScope('${NAME}')
	scope.$watch('model', function(newvalue,oldvalue) {
	// handle state changes
		console.log(newvalue)
}, true);
})