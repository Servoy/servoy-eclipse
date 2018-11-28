angular.module("menubar",[]).directive("menubar", function($editorService)
{
	return {
        restrict: 'E',
        transclude: true,
		link: function($scope, $element, $attrs) {

            $scope.actions = [
                {
                    label: "",
                    style: {
                        float: "right",
                        backgroundRepeat: "no-repeat",
                        backgroundPosition: "center top",
                        backgroundImage: 'url("images/zoom_in.png")',
                        backgroundSize: "contain",
                        width: "20px",
                        height: "16px"
                    },
                    execute: function(e)
                    {
                        e.stopPropagation();
                        $editorService.executeAction('zoomIn');
                    }
                },
                {
                    label: "",
                    style: {
                        float: "right",
                        backgroundRepeat: "no-repeat",
                        backgroundPosition: "center top",
                        backgroundImage: 'url("images/delete.gif")',
                        backgroundSize: "contain",
                        width: "20px",
                        height: "16px"
                    },
                    execute: function(e)
                    {
                        e.stopPropagation();
						$editorService.keyPressed({"keyCode":46});
                    }
                },
                {
                    label: "",
                    style: {
                        float: "right",
                        backgroundRepeat: "no-repeat",
                        backgroundPosition: "center top",
                        backgroundImage: 'url("images/copy_edit.png")',
                        backgroundSize: "contain",
                        width: "20px",
                        height: "16px"
                    },
                    execute: function(e)
                    {
                        e.stopPropagation();
                        $editorService.executeAction('copy');
                    }
                }
            ];
            
            $scope.getContainerType = function() {
                return $scope.node.node.attr("svy-layoutname") + ': ';
            }

            $scope.getContainerName = function() {
                return $scope.node.node.attr("svy-name");
            }

            $scope.onmousedown = function(e) {
                e.stopPropagation();
            }
		},
		templateUrl: "js/modules/menubar/menubar.html"
	    };
	
});