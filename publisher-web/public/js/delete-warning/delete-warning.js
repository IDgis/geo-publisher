define ([
	'dojo/dom',
	'dojo/dom-attr',
	
	'put-selector/put'
], function (dom, domAttr, put) {
	
	function DeleteWarning (element) {
		var deletePreModalTitle = domAttr.get(element, 'data-warning-pre-title');
		var deleteLink = domAttr.get(element, 'data-warning-delete-link');
		var deleteModalTitle = domAttr.get(element, 'data-warning-title');
		var deleteModalBody = domAttr.get(element, 'data-warning-delete-body');
		
		var modalForm = dom.byId('js-modal-form');
		put(modalForm, "[action=$]", deleteLink);
		
		var modalTitle = dom.byId('js-modal-title');
		put(modalTitle, "$ $ $", deletePreModalTitle, ": ", deleteModalTitle);
		
		var modalBody = dom.byId('js-modal-body');
		put(modalBody, "$", deleteModalBody);
	}
	return DeleteWarning;
});