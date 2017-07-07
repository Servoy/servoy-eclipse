angular.module('app', ['ngMaterial'])
.config(function($mdIconProvider) {
  $mdIconProvider
    .iconSet('social', 'img/icons/sets/social-icons.svg', 24)
    .iconSet('device', 'img/icons/sets/device-icons.svg', 24)
    .iconSet('communication', 'img/icons/sets/communication-icons.svg', 24)
    .defaultIconSet('img/icons/sets/core-icons.svg', 24);
})
.controller('appPackages', function($scope,$window,$sce,$mdDialog) {
    var loc = $window.location;
    var self = this;
    var addRemove = "Add...";
	var uri = "ws://"+loc.host+"/wpm/angular2/websocket";
//		var uri = "ws://localhost:8080/wpm/angular2/websocket";
	  
	var ws = new WebSocket(uri);
	$scope.websocket = ws;
	$scope.isLoading = true;
    
	$window.addEventListener("beforeunload", function() {
    	ws.close();
    });

	ws.onopen = function (event) {
	      var command = {"method":"requestAllInstalledPackages"};
	      var solutionName = decodeURIComponent((new RegExp('[?|&]solution=' + '([^&;]+?)(&|#|;|$)').exec($window.location.search)||[,""])[1].replace(/\+/g, '%20'))||null;
		  if (solutionName)
		  {
			  command.solution = solutionName;
		  }	
	      ws.send(JSON.stringify(command)); 
	      command = {"method":"getSolutionList"};
	      ws.send(JSON.stringify(command)); 
	      
	      command = {"method":"getRepositories"};
	      ws.send(JSON.stringify(command)); 
	};
	  
	ws.onmessage = function (msg){
		$scope.$apply(function() {
			var receivedJson = JSON.parse(msg.data);
			var method = receivedJson["method"];
			self[method](receivedJson["result"]);
		})
	}
	
	$scope.solutionList = []
	$scope.componentPackages = []
	$scope.servicePackages = []
	$scope.layoutPackages = []
	$scope.solutionPackages = []
	  
	this.requestAllInstalledPackages = function(packagesArray){
		$scope.isLoading = false;
		$scope.componentPackages.length = 0;
		$scope.servicePackages.length = 0;
		$scope.layoutPackages.length = 0;
		$scope.solutionPackages.length = 0;
		for(i=0;i<packagesArray.length;i++) {
			if (packagesArray[i].packageType == "Web-Component") {
				$scope.componentPackages.push(packagesArray[i]);
			}
			else if (packagesArray[i].packageType == "Web-Service") {
				$scope.servicePackages.push(packagesArray[i]);
			}
			else if (packagesArray[i].packageType == "Web-Layout") {
				$scope.layoutPackages.push(packagesArray[i]);
			}
			else if (packagesArray[i].packageType == "Solution") {
				$scope.solutionPackages.push(packagesArray[i]);
			}
		}
	};
	this.getSolutionList = function(solutionList) {
		$scope.solutionList  = solutionList;
	}
    //function called via json "message":"updatePackage"
    this.updatePackage = function (pack) {
	  	var i;
	  	for (i=0; i<this.items.length; i++){
	  	    if (this.items[i].name === pack.name) {
	  		this.items[i].version = pack.version;
	  		this.items[i].latestVersion = pack.latestVersion;
	  	    }
	  	}
    }
    
    this.getRepositories = function(repositories) {
    	$scope.repositories = [];
    	for(var i=0;i<repositories.length;i++) {
    		if (repositories[i].selected) {
    			$scope.activeRepository = repositories[i].name;
    		}
    		$scope.repositories[i] =  repositories[i].name;
    	}
    	$scope.repositories[repositories.length] = addRemove;
    }
    
    this.addRepository = function(newPackagesAndRepositories) {
    	this.getRepositories(newPackagesAndRepositories.repositories)
    	this.requestAllInstalledPackages(newPackagesAndRepositories.packages);
    }
    
    this.removeRepository = function (repositories) {
    	this.getRepositories(repositories);
    }
    
    this.setSelectedRepository = function(newPackages) {
    	this.requestAllInstalledPackages(newPackages);
    }
    

    $scope.tabSelected = 1;

    $scope.getActiveSolution = function() {
    	if ($scope.solutionList && $scope.solutionList.length) {
    		return $scope.solutionList[$scope.solutionList.length -1];
    	}
    	return "";
    }

    // check if there are pending upgrades for the installed packages
    $scope.getUpgradeCount = function(packageList) {
      var count = 0;
      try {
	      for (var i = 0; i < packageList.length; i++) {
	          var item = packageList[i];
	          if (item.installed && item.installed < item.releases[0].version) {
	            count++;
	          }
    	  }
  	  } catch (e) {}
      return count ? "(" + count +")" : "";
    }
    
    $scope.showRemoveRepository = false;
    
    $scope.repositories = ["Servoy Default",addRemove]
    $scope.activeRepository = "Servoy Default";
    
    $scope.removeRepository = function() {
    	$scope.isLoading = true;
		var command = {"method":"removeRepository","name": $scope.activeRepository};
		$scope.websocket.send(JSON.stringify(command));
    }
	   
	$scope.$watch("activeRepository", function(newValue,oldValue) {
		if (newValue === oldValue) return;
		if (newValue == addRemove) {
			showAddDialog();
			$scope.showRemoveRepository = false;
		}
		else {
			$scope.isLoading = true;
			$scope.showRemoveRepository = newValue != "Servoy Default";
			var command = {"method":"setSelectedRepository","name": newValue};
			$scope.websocket.send(JSON.stringify(command));
		}
	});
	
	function showAddDialog() {
		$mdDialog.show({
		      controller: DialogController,
		      templateUrl: 'addrepository.tmpl.html',
		      parent: angular.element(document.body),
		      clickOutsideToClose:true
		    })
		    .then(function(result) {
		    		function showAlert(txt) {
		    			 $mdDialog.show($mdDialog.alert()
					    	        .parent(angular.element(document.body))
					    	        .clickOutsideToClose(true)
					    	        .title("Error creating new repository index")
					    	        .textContent(txt)
					    	        .ariaLabel(txt)
					    	        .ok('OK')).then(function(){
					    	        	 showAddDialog()
					    	        })
		    		}
		    	   if(result.name == addRemove) {
		    		   showAlert("The name can't be " + addRemove);
		    		   return;
		    	   }
		    	   if (result.name == "" || result.url == "") {
		    		   showAlert("The name or url must be filled in");
		    		   return;
		    		   
		    	   }
		    	   for (var i =0;i< $scope.repositories.length;i++) {
		    		   if (result.name ==  $scope.repositories[i]) {
		    			   showAlert("The name is already defined");
			    		   return; 
		    		   }
		    	   }
		    	   $scope.isLoading = true;
				   var command = {"method":"addRepository","values": result};
				   $scope.websocket.send(JSON.stringify(command));
		    }, function() {
		    	 $scope.activeRepository = "Servoy Default"
		    });
	}
	function DialogController($scope, $mdDialog) {
		$scope.name = "";
		$scope.url = ""
	    $scope.cancel = function() {
	      $mdDialog.cancel();
	    };

	    $scope.add = function() {
	      $mdDialog.hide({name:$scope.name, url:$scope.url});
	    };
	  }

}).directive('packages',  function ($sce) {
	return {
		restrict: 'E',
		templateUrl: 'packages.html',
		scope: {
			packages: "=packages",
			solutionList: "=solutionList",
			websocket: "=websocket"
		},
		link:function($scope, $element, $attrs) {

		  $scope.selectedPackage;
		  $scope.descriptionExpanded;
			
		  $scope.getInstallText = function(index) {
		    if ($scope.packages[index].installed) { 
		      return $scope.packages[index].installing ? "Upgrading ..." : "Upgrade";
		    } else if($scope.packages[index].installing) {
			  return "Adding to Solution ...";		      
		    } else {
		      return "Add to Solution";
		    }
		  }

		  $scope.getInstallClass = function(index) {
		  	if ($scope.packages[index].installed) { 
		      return $scope.packages[index].installing ? "fa fa-level-up" : "fa fa-level-up";
		    } else if($scope.packages[index].installing) {
			  return "fa fa-plus-square";		      
		    } else {
		      return "fa fa-plus-square";
		    }
		  }

		  $scope.getInstallTooltip = function(index) {
			    if ($scope.packages[index].installed) { 
			      return $scope.packages[index].installing ? "Upgrading the web package..." : "Upgrade the web package to the selected release version.";
			    } else if($scope.packages[index].installing) {
				  return "Adding the web package...";		      
			    } else {
			      return "Add the web package to solution '" + $scope.packages[index].activeSolution + "'.";
			    }
			  }  
			  
		  $scope.getSolutionTooltip = function(index) {
			    if ($scope.packages[index].installed) { 
			      return $scope.packages[index].installing ? "Solution that will contain this upgrading package..." : "The solution that already contains/references this package.";
			    } else if($scope.packages[index].installing) {
				  return "Solution that will contain this package...";		      
			    } else {
			      return "Solution that this package will be added to if you press the 'Add' button.";
			    }
			  }  
			  
		  $scope.getReleaseTooltip = function(index) {
			    if ($scope.packages[index].installed) { 
			      return "Version to upgrade to...";
			    } else {
			      return "Version to add to the active solution or modules...";
			    }
			  }  
			  
		  $scope.getSelectedRelease = function(index) {
			if (angular.isUndefined($scope.packages[index].selected)) {
				$scope.packages[index].selected = $scope.packages[index].releases[0].version; 
			}
		    if ($scope.packages[index].selected == $scope.packages[index].releases[0].version) {
		      return $scope.packages[index].selected + " Latest";
		    } else {
		      return $scope.packages[index].selected;
		    }
		  }
		   
		   $scope.install = function (pck, index) {
			   $scope.packages[index].installing = true;
			   var command = {"method":"install","package": pck};
			   $scope.websocket.send(JSON.stringify(command));
		  	}
		   
		  $scope.uninstall = function(pck, index) {
			  $scope.packages[index].removing = true;
			   var command = {"method":"remove","package": pck};
			   $scope.websocket.send(JSON.stringify(command));
		  	}

		  $scope.isLatestRelease = function (index) {
		    return $scope.packages[index].installed == $scope.packages[index].releases[0];
		  }

		  /* return true if install is available and there is no pending operation */
		  $scope.installEnabled = function (index) {
		    return !$scope.isInstallingOrRemoving(index) && $scope.installAvailable(index);
		  }

		  /* return true if install or update is available */
		  $scope.installAvailable = function (index) {
		  	return !$scope.packages[index].installed || (!$scope.isLatestRelease(index) && $scope.packages[index].selected > $scope.packages[index].installed);
		  }

		  /* return true if there is installing or removing is pending */
		  $scope.isInstallingOrRemoving = function (index) {
		  	return $scope.packages[index].installing || $scope.packages[index].removing;
		  }

		  $scope.isPackageSelected = function(index) {
        	return $scope.selectedPackage && $scope.selectedPackage === $scope.packages[index].displayName;
          }
		  
		  $scope.isSelectedAndWithDescriptionExpanded = function(index) {
	        return $scope.isPackageSelected(index) && $scope.descriptionExpanded;
          }

	      $scope.togglePackageSelection = function(index, event) {
	    	if ($scope.isPackageSelected(index)) {
	          $scope.descriptionExpanded = !$scope.descriptionExpanded;
	          if ($scope.descriptionExpanded) $scope.descriptionExpanded = !!$scope.packages[index].description; // allow expand only if it has a description
	        } else {
	          $scope.selectedPackage = $scope.packages[index].displayName;
	          $scope.descriptionExpanded = !!$scope.packages[index].description;
	        }
	        event.stopPropagation();
	      }

	      $scope.getPackageDescription = function(index) {
	      	if ($scope.packages[index].description) {
	      		return $sce.trustAsHtml($scope.packages[index].description);
	      	}
	      	return "";
	      }

		  $scope.showUrl = function(value) {
			  var command = {"method":"showurl","url": value};
			   $scope.websocket.send(JSON.stringify(command));
		  }

		}
	}
});