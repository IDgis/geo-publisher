define ([
	'dojo/_base/lang',
	'dojo/query',
	'dojo/on',
	'dojo/dom-class',
	'dojo/dom-attr',
	
	'dojo/NodeList-dom'
], function (
	lang,
	query,
	on,
	domClass,
	domAttr
) {

	function Select (nodeOrSelector, options) {
		this.node = query (nodeOrSelector)[0];
		
		var self = this;

		if (options) {
			lang.mixin (this, options);
		}
		
		on (this.node, '.js-pager-select-button:click', function (e) {
			e.preventDefault ();
			e.stopPropagation ();
		
			self.open ();
		});
		
		on (this.node, '*[data-id]:click', function (e) {
			e.preventDefault ();
			e.stopPropagation ();
			
			var id = domAttr.get (this, 'data-id'),
				label = domAttr.get (this, 'data-label');
			
			self.onSelect ({
				id: id,
				label: label
			});
			
			self.close ();
		});
	}
	
	lang.mixin (Select.prototype, {
		open: function () {
			query ('.dropdown', this.node).addClass ('open');
			
			var self = this;
			
			this._closeHandle = on (query ('html')[0], 'click', function (e) {
				var node = e.target,
					dropdown = query ('.dropdown', self.node)[0],
					button = query ('.js-pager-select-button')[0];
				
				while (node) {
					if (node == dropdown || node == button) {
						return;
					}
					
					node = node.parentNode;
				}
				
				self.close ();
			});
		},
		
		close: function () {
			query ('.dropdown', this.node).removeClass ('open');
			
			if (this._closeHandle) {
				this._closeHandle.remove ();
				delete this._closeHandle;
			}
		},
		
		onSelect: function (item) {
		}
	});
	
	return Select;
});