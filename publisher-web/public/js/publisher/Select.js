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
		});
	}
	
	lang.mixin (Select.prototype, {
		open: function () {
			query ('.dropdown', this.node).addClass ('open');
		},
		
		close: function () {
		},
		
		onSelect: function (item) {
		}
	});
	
	return Select;
});