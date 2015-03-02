define ([
	'dojo/query',
	'dojo/on',
	
	'put-selector/put'
], function (
	query,
	on,
	
	put
) {
	
	function Pager (nodeOrSelector) {
		this.node = query (nodeOrSelector)[0];
	}
	
	return Pager;
});