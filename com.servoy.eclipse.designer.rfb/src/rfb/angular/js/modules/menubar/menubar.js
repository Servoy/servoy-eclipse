angular.module("menubar",[]).directive("menubar", function($editorService)
{
	return {
        restrict: 'E',
        transclude: true,
		link: function($scope, $element, $attrs) {

			$scope.menu={};
            $scope.menu.actions = [
                {
                    label: "",
                    style: {
                    	backgroundColor: "#606060",
                        backgroundRepeat: "no-repeat",
                        backgroundPosition: "center",
                        backgroundImage: 'url("images/insert_before.png")',
                        width: "20px",
                        height: "18px"
                    },
                    execute: function(e)
                    {
                        e.stopPropagation();
                        var component = {}
                        var layoutPackage = $scope.node.node.attr("svy-layoutname").split(".");
                        component.packageName = layoutPackage[0];
                        component.name = layoutPackage[1];
                        var droptarget = $scope.node.node[0].parentNode.getAttribute("svy-id");
                        if (droptarget)  component.dropTargetUUID = droptarget;
                        component.rightSibling = $scope.node.node.attr("svy-id");
                        $editorService.createComponent(component);
                    }
                },
                 {
                    label: "",
                    style: {
                    	backgroundColor: "#606060",
                        backgroundRepeat: "no-repeat",
                        backgroundPosition: "center",
                        backgroundImage: 'url("images/insert_after.png")',
                        width: "20px",
                        height: "18px"
                    },
                    execute: function(e)
                    {
                        e.stopPropagation();
                        var component = {}
                        var layoutPackage = $scope.node.node.attr("svy-layoutname").split(".");
                        component.packageName = layoutPackage[0];
                        component.name = layoutPackage[1];
                       	var droptarget = $scope.node.node[0].parentNode.getAttribute("svy-id");
                        if (droptarget)  component.dropTargetUUID = droptarget;
                        if ($scope.node.node[0].nextSibling)
                        {
                        	component.rightSibling = $scope.node.node[0].nextSibling.getAttribute("svy-id");
                        }
                        $editorService.createComponent(component);
                    }
                },
                {
                    label: "",
                    style: {
                        backgroundRepeat: "no-repeat",
                        backgroundColor: "#606060",
                        backgroundPosition: "center",
                        backgroundImage: 'url("images/copy_edit.png")',
                        width: "20px",
                        height: "18px"
                    },
                    execute: function(e)
                    {
                        e.stopPropagation();
                        $editorService.executeAction('copy');
                        $scope.actions[2].style.backgroundImage = 'url("images/copy_edit_disabled.png")';
                        setTimeout(function() {
                            $scope.actions[2].style.backgroundImage = 'url("images/copy_edit.png")';
						}, 100);
                    }
                },
                {
                    label: "",
                    style: {
                    	marginRight: "1px",
                    	backgroundColor: "#606060",
                        backgroundRepeat: "no-repeat",
                        backgroundPosition: "center",
                        backgroundImage: 'url("images/zoom_in_menu.png")',
                        width: "20px",
                        height: "18px"
                    },
                    execute: function(e)
                    {
                        e.stopPropagation();
                        $editorService.executeAction('zoomIn');
                    }
                }
            ];
            
            $scope.deleteMenu = {}
            $scope.deleteMenu.actions = [
            	{
                    label: "",
                    style: {
                    	marginRight: "10px",
                    	backgroundColor: "#A80000",
                        backgroundRepeat: "no-repeat",
                        backgroundPosition: "center",
                        backgroundImage: 'url("images/delete_in_menu.png")',
                        width: "20px",
                        height: "18px"
                    },
                    execute: function(e)
                    {
                        e.stopPropagation();
						$editorService.keyPressed({"keyCode":46});
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