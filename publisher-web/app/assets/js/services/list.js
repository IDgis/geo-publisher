require ([
	'dojo/on',
	'dojo/_base/window',
	'dojo/query',
	'delete-warning/delete-warning',
	
	'dojo/NodeList-traverse',
	'dojo/domReady!'
], function (on, win, query, DeleteWarning) {
	
	on(win.doc, ".js-delete-button:click", function(event) {
		event.preventDefault();
		event.stopPropagation();
		var itemToDel = query(this).parents(".list-group-item")[0];
		
		var deleteWarning = new DeleteWarning (itemToDel);
	});
});