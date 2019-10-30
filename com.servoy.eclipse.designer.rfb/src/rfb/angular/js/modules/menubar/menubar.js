angular.module("menubar",[]).directive("menubar", function($editorService, $rootScope, EDITOR_EVENTS)
{
	return {
        restrict: 'E',
        transclude: true,
		link: function($scope, $element, $attrs) {

		$scope.selection = $editorService.getEditor().getSelection();
		$rootScope.$on(EDITOR_EVENTS.SELECTION_CHANGED, function(event, sel) {
			$scope.selection = sel;
		});
		if ($scope.selection.length != 1) return;		
		
		function addBefore(e)
		{
			e.stopPropagation();
			var component = {}
			var layoutPackage = $scope.node.node.attr("svy-layoutname").split(".");
			component.packageName = layoutPackage[0];
			component.name = layoutPackage[1];
			var droptarget = $scope.node.node[0].parentNode.getAttribute("svy-id");
			if (droptarget)  component.dropTargetUUID = droptarget;
			component.rightSibling = $scope.node.node.attr("svy-id");
			component.keepOldSelection = true;
			$editorService.createComponent(component);
		}
		
		function addAfter(e)
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
			component.keepOldSelection = true;
			$editorService.createComponent(component);
		}
		
		function copy(e) {
			e.stopPropagation();
			$editorService.executeAction('copy');
			$scope.actions[2].style.backgroundImage = 'url("images/copy_edit_disabled.png")';
			setTimeout(function() {
				$scope.actions[2].style.backgroundImage = 'url("images/copy_edit.png")';
			}, 100);
		}
		
		function zoomIn(e) {
			 e.stopPropagation();
			$editorService.executeAction('zoomIn');
		}

			$scope.menu={};
            $scope.menu.actions = [
                {
                    label: "",
                    style: {
                    	backgroundColor: "#606060",
                        backgroundRepeat: "no-repeat",
                        backgroundPosition: "center",
                        backgroundImage: 'url("images/insert_before.png")',
                        cursor: "pointer",
                        width: "20px",
                        height: "18px"
                    },
                    execute: function(e)
                    {
                       	addBefore(e);
                    }
                },
                 {
                    label: "",
                    style: {
                    	backgroundColor: "#606060",
                        backgroundRepeat: "no-repeat",
                        backgroundPosition: "center",
                        backgroundImage: 'url("images/insert_after.png")',
                        cursor: "pointer",
                        width: "20px",
                        height: "18px"
                    },
                    execute: function(e)
                    {
                       addAdter(e);
                    }
                },
                {
                    label: "",
                    style: {
                        backgroundRepeat: "no-repeat",
                        backgroundColor: "#606060",
                        backgroundPosition: "center",
                        backgroundImage: 'url("images/copy_edit.png")',
                        cursor: "pointer",
                        width: "20px",
                        height: "18px"
                    },
                    execute: function(e)
                    {
                        copy(e);
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
                        cursor: "pointer",
                        width: "20px",
                        height: "18px"
                    },
                    execute: function(e)
                    {
                       zoomIn(e);
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
                        cursor: "pointer",
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

			$scope.dropdownMenu = [];
			$scope.dropdownMenu.actions = [
				 {
                    label: "",
                    style: {
                        backgroundImage: 'url("images/more.png")',
						cursor: 'default'
                    }
                 },
				 {
                    label: "Add before",
                    style: {
                        backgroundImage: 'url("images/insert_before_dropdown.png")',
						color: '#000000'
                    },
                    execute: function(e)
                    {
                       	addBefore(e);
                    }
                },
				{
                    label: "Add after",
                    style: {
                        backgroundImage: 'url("images/insert_after_dropdown.png")',
						color: '#000000'
                    },
                    execute: function(e)
                    {
                       	addAfter(e);
                    }
                },
				{
                    label: "Copy",
                    style: {
                        backgroundImage: 'url("images/copy_in_dropdown.png")',
						color: '#000000'
                    },
                    execute: function(e)
                    {
                       	copy(e);
                    }
                },
				{
                    label: "Zoom in",
                    style: {
                        backgroundImage: 'url("images/zoom_in_dropdown.png")',
						color: '#000000'
                    },
                    execute: function(e)
                    {
                       	zoomIn(e);
                    }
                },
				{
                    label: "Delete",
                    style: {
                        backgroundImage: 'url("images/delete_in_dropdown.png")',
						color: '#A80000'
                    },
                    execute: function(e)
                    {
                       	zoomIn(e);
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
			$scope.adjustMoreButtonPosition = function() {
				if (parseInt(window.getComputedStyle($scope.node.node[0], ":before").height) > 0) {
					var computedStyle = window.getComputedStyle($scope.node.node[0], ":before");
					var left = parseInt($scope.node.node.css('paddingLeft')) + parseInt(computedStyle.marginLeft);
					var right = $scope.node.node[0].getBoundingClientRect().width - left - parseInt(computedStyle.width);
					$(document.getElementsByClassName('moreMenuItem')[0]).css('margin-right', right+"px");
				}
				return true;
			}
		},
		templateUrl: "js/modules/menubar/menubar.html"
	    };
	
});