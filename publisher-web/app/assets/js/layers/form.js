require ([
	'tree-select/tree-select',
	'publisher/Pager',
	
	'dojo/domReady!'
], function (TreeSelect, Pager) {
	var treeSelect = new TreeSelect ('.gp-tree-select', '.gp-tree-values'),
		pager = new Pager ('.gp-tree-values .js-pager');
	
	
});

$(function () {
	$('[data-toggle="popover"]').popover();
});