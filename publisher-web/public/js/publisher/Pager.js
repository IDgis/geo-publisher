define ([
	'dojo/_base/lang',
	'dojo/query',
	'dojo/on',
	'dojo/dom-attr',
	'dojo/request/xhr',
	
	'put-selector/put'
], function (
	lang,
	query,
	on,
	domAttr,
	xhr,
	
	put
) {
	
	function Pager (nodeOrSelector) {
		this.node = query (nodeOrSelector)[0];
		
		var self = this;
		
		on (this.node, '*[data-pager-link]:click', function (e) {
			e.preventDefault ();
			e.stopPropagation ();
			
			self._doPage (domAttr.get (this, 'data-pager-link'));
		});
	}
	
	lang.mixin (Pager.prototype, {
		_doPage: function (url) {
			console.log ('Paging to: ', url);
			xhr.get (url, {
				handleAs: 'json'
			}).then (function (data) {
				
			});
		}
	});
	
	return Pager;
});