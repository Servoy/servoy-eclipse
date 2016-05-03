(function(app) {
  app.AppComponent =
    ng.core.Component({
      selector: 'my-app',
      providers: [ng.http.HTTP_PROVIDERS],
      templateUrl: 'app/app.component.html',
    })
    .Class({
	      constructor: [ng.http.Http,ng.core.NgZone, function(http,zone) {
		  var loc = window.location;
		  var self = this; 
		  
		  var uri = "ws://"+loc.host+"/wpm/angular2/websocket";
		  this.websocket = new WebSocket(uri);
		  
		  var ws = this.websocket;
		  
		  ws.onopen = function (event) {
		      var command = {"method":"requestAllInstalledPackages"};
		      ws.send(JSON.stringify(command)); 
		  };
		  
		  //http.get("http://servoy.github.io/webpackageindex").map(function (res) { return res.json();}).subscribe(function(data){console.log(data);});
		  
		  ws.onmessage = function (msg){
			  zone.run(function() {
				  var receivedJson = JSON.parse(msg.data);
			      if (receivedJson["requestAllInstalledPackages"]) receivedAllInstalledPackage(self,receivedJson["requestAllInstalledPackages"]);
			      if (receivedJson["message"]) self[receivedJson["message"]](receivedJson["args"]);
			  })
		  }
		  
		  function receivedAllInstalledPackage(self, packagesArray){
				 self.items = packagesArray;
		  };
	  }],
      
      install : function (item) {
        	  var ws = this.websocket;
        	  var command = {"method":"install",
        		  	 "args": item};
        	  ws.send(JSON.stringify(command));
      },
      //function called via json "message":"updatePackage"
      updatePackage: function (pack) {
	  	var i;
	  	for (i=0; i<this.items.length; i++){
	  	    if (this.items[i].name === pack.name) {
	  		this.items[i].version = pack.version;
	  		this.items[i].latestVersion = pack.latestVersion;
	  	    }
	  	}
      }
    });
})(window.app || (window.app = {}));
