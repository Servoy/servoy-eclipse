angular.module("palette",['ui.bootstrap']).directive("palette", function(){
	return {
	      restrict: 'E',
	      transclude: true,
	      scope: {},
	      controller: function($scope, $element, $attrs) {
	    	 $scope.categories = [];
	    	  
	    	 function addCategory(name, isOpen) {
	    		 for(var c in $scope.categories) {
	    			 if($scope.categories[c].name == name) {
	    				 return $scope.categories[c];
	    			 }
	    		 }
	    		 var category = {name : name, components: [], isOpen: isOpen};
	    		 $scope.categories.push(category);
	    		 
	    		 return category;
	    	 }
	    	 
	    	 function addComponent(categoryName, name, icon) {
	    		 var category = addCategory(categoryName);
	    		 category.components.push({name : name, icon : icon});
	    	 }
	    	 
	    	 addCategory("Form Components", true);
	    	 addCategory("Containers");
	    	 addCategory("Charts");
	    	 addCategory("HTML");
	    	 
	    	 addComponent("Form Components", "Button", "images/old/button.gif");
	    	 addComponent("Form Components", "Radio", "images/old/RADIO16.png");
	    	 addComponent("Form Components", "Checkbox", "images/old/CHECKBOX16.png");
	    	 addComponent("Form Components", "Label", "images/old/text.gif");
	    	 addComponent("Form Components", "Text Field", "images/old/textinput.png");
	    	 addComponent("Form Components", "Text Area", "images/old/TEXTAREA16.png");
	    	 addComponent("Form Components", "HTML Area", "images/old/H1_C16.png");
	    	 addComponent("Form Components", "RTF Area", "images/old/doc_rtf.png");
	    	 addComponent("Form Components", "Password Field", "images/old/password_field_16.png");
	    	 addComponent("Form Components", "Calendar Field", "images/old/Calendar_C16.png");
	    	 addComponent("Form Components", "Media Field", "images/old/IMG16.png");
	    	 addComponent("Form Components", "Combobox", "images/old/SELECT16.png");
	    	 addComponent("Form Components", "Listbox", "images/old/listbox.png");
	    	 addComponent("Form Components", "Multiselect Listbox", "images/old/listbox.png");
	    	 addComponent("Form Components", "TypeAhead", "images/old/bhdropdownlisticon.gif");
	    	 addComponent("Form Components", "Spinner", "images/old/spinner.png");
	    	 addComponent("Form Components", "Portal", "images/old/portal.gif");
	    	 
	    	 addComponent("Containers", "Horizontal Flow", "");
	    	 addComponent("Containers", "Vertical Flow", "");
	    	 addComponent("Containers", "Absolute Layout", "");
	    	 addComponent("Containers", "Grid Row", "");
	    	 addComponent("Containers", "Tabbed Pane", "");
	    	 addComponent("Containers", "Split Pane", "");
	    	 addComponent("Containers", "Tabless Pane", "");
	    	 
	    	 addComponent("Charts", "Gauge", "");
	    	 
	    	 addComponent("HTML", "DIV", "");
	    	 addComponent("HTML", "SPAN", "");
	    	 addComponent("HTML", "H1", "");
	    	 addComponent("HTML", "P", "");
	      },
	      templateUrl: 'templates/palette.html',
	      replace: true
	    };
	
})