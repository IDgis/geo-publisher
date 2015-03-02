define ([
	'dojo/_base/lang',
	'dojo/query',
	'dojo/on',
	'dojo/dom-attr',
	'dojo/dom-construct',
	'dojo/request/xhr',
	
	'put-selector/put'
], function (
	lang,
	query,
	on,
	domAttr,
	domConstruct,
	xhr,
	
	put
) {
	
	function Pager (nodeOrSelector) {
		this.node = query (nodeOrSelector)[0];
		
		var self = this;
		
		on (this.node, '.pagination a[href]:click', function (e) {
			e.preventDefault ();
			e.stopPropagation ();
			
			var url = domAttr.get (this, 'href');
			if (!url.contains ('#')) {
				self._doPage (url);
			}
		});
	}
	
	lang.mixin (Pager.prototype, {
		_doPage: function (url) {
			console.log ('Paging to: ', url);
			xhr.get (url, {
				handleAs: 'json'
			}).then (lang.hitch (this, function (data) {
				domConstruct.empty (this.node);
				this.node.innerHTML = data.htmlContent;
			}));
		}
	});
	
	return Pager;
});