var SFComponentFactory = function() {
	function Factory() {
		this.idCounter = 0
	}

	Factory.prototype = Object.create(null)
	Factory.prototype.constructor = Factory

	Factory.prototype.getInstance = function(type) {
		if (Factory.prototype.types.hasOwnProperty(type)) {
			return Factory.prototype.types[type].apply(this)
		} else {
			console.log('Unknown type ' + type)
		}
	}

	Factory.prototype.types = {}

	Factory.prototype.register = function(type, method) {
		this.types[type] = method
	}

	Factory.prototype.getNewID = function() {
		return 'x' + this.idCounter++
	}

	return new Factory()
}()

SFComponentFactory.register('JUSTGAGE', function() {
		var id = this.getNewID()
		var el = $('<div id="' + id + '"></div>')
		el.css({
			width: 100,
			height: 100
		})
		$('body').append(el)
		new JustGage({ id: id, value: 34 })
		return el
	})

SFComponentFactory.register('BOOTSTRAP.GRID', function() {
		var template = '<div id="' + this.getNewID() + '" class="row svelement svcontainer"><div id="' + this.getNewID() + '" class="col-md-4 svelement svcontainer"></div><div id="' + this.getNewID() + '" class="col-md-4 svelement svcontainer"></div><div id="' + this.getNewID() + '" class="col-md-4 svelement svcontainer"></div></div>'
		var el = $(template)
		$('body').append(el)
		return el
	})
