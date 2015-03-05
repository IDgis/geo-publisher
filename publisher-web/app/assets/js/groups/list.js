require ([
	'dojo/on',
	'dojo/_base/window',
	'dojo/query',
	'delete-warning/delete-warning',
	
	'dojo/NodeList-traverse',
	'dojo/domReady!'
], function (on, win, query, DeleteWarning) {
	
	on(win.doc, ".deleteButton:click", function(event) {
		var itemToDel = query(this).parents(".list-group-item")[0];
		
		var deleteWarning = new DeleteWarning (itemToDel);
	});
});