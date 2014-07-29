(function(){
	/**
	 * @type {{
	 * 	context: String=,
	 * 	event: String=,
	 *  sequence: String=,
	 * 	action: function(*)}}
	 * 
	 * @SuppressWarnings(unused)
	 */
	var BEHAVIOR_CONFIG_TYPE
	
	function ActionBindingPlugin(){

		var registeredHandlers = {
			
		}
		
		/**
		 * @param {BEHAVIOR_CONFIG_TYPE} config
		 */
		this.registerBehavior = function(config) {
			var regex = /([^\+]+)|\+(\+)/g
			
			if (!registeredHandlers.hasOwnProperty(config.context)) {
				registeredHandlers[config.context] = {}
			}
			if (!registeredHandlers[config.context].hasOwnProperty(config.event)) {
				var actions = {}
				registeredHandlers[config.context][config.event] = actions
				
				if (config.event === 'keydown') {
					var listener = new window.keypress.Listener()
				} else {
					
				}
				$(document).on(config.event, config.context, function(){
					
				}.bind(actions))
			}
			
			registeredHandlers[config.context][config.event][config.sequence] = config
		}
		
		
		/*
		var doc, context
		if (config.context === Editor.ACTION_CONTEXT.EDITOR) {
			doc = document
			context = this.selector
		} else {
			doc = this.contentDocument
			context = config.context || Editor.ACTION_CONTEXT.CONTENT
		}
		$(doc).on(config.event, context, config.action.bind(this))
		 */
	}
	
	Editor.registerPlugin(ActionBindingPlugin)
}())