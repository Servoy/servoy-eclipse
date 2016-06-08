angular.module('app', ['ngMaterial'])
.config(function($mdIconProvider) {
  $mdIconProvider
    .iconSet('social', 'img/icons/sets/social-icons.svg', 24)
    .iconSet('device', 'img/icons/sets/device-icons.svg', 24)
    .iconSet('communication', 'img/icons/sets/communication-icons.svg', 24)
    .defaultIconSet('img/icons/sets/core-icons.svg', 24);
})
.controller('appPackages', function($scope,$window) {
    var loc = $window.location;
    var self = this;
		  
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
	      ws.send(JSON.stringify(command)); 
	      command = {"method":"getSolutionList"};
	      ws.send(JSON.stringify(command)); 
	};
	  
	ws.onmessage = function (msg){
		$scope.$apply(function() {
			var receivedJson = JSON.parse(msg.data);
			var method = receivedJson["method"];
			if(method === "requestAllInstalledPackages") $scope.isLoading = false;
			self[method](receivedJson["result"]);
		})
	}
	
	$scope.solutionList = []
	$scope.componentPackages = []
	$scope.servicePackages = []
	$scope.layoutPackages = []
	  
	this.requestAllInstalledPackages = function(packagesArray){
		$scope.componentPackages.length = 0;
		$scope.servicePackages.length = 0;
		$scope.layoutPackages.length = 0;
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

    $scope.tabSelected = 1;
}).directive('packages',  function () {
	return {
		restrict: 'E',
		templateUrl: 'packages.html',
		scope: {
			packages: "=packages",
			solutionList: "=solutionList",
			websocket: "=websocket"
		},
		link:function($scope, $element, $attrs) {
			
		  $scope.getInstallText = function(index) {
		    if ($scope.packages[index].installed) { 
		      return $scope.packages[index].installing ? "Upgrading ..." : "Upgrade";
		    } else if($scope.packages[index].installing) {
			  return "Installing ...";		      
		    } else {
		      return "Add to solution";
		    }
		  }

		  $scope.getRemoveText = function(index) {
			  if($scope.packages[index].removing) {
				  return "Removing ...";		      
			  } else {
			      return "Remove";
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

		  $scope.installEnabled = function (index) {
		    return !$scope.packages[index].installing && !$scope.packages[index].removing && (!$scope.packages[index].installed || (!$scope.isLatestRelease(index) && $scope.packages[index].selected > $scope.packages[index].installed ))
		  }

		  $scope.showUrl = function(value) {
			  var command = {"method":"showurl","url": value};
			   $scope.websocket.send(JSON.stringify(command));
		  }
		  
		  $scope.removeEnabled = function (index) {
			  return !$scope.packages[index].installing && !$scope.packages[index].removing;
		  }
		}
	}
});