define ([
	'dojo/dom'
], function (dom) {
	
	function DeleteWarning (element) {
		var deletePreModalTitle = element.dataset.warningPreTitle;
		var deleteLink = element.dataset.warningDeleteLink;
		var deleteModalTitle = element.dataset.warningTitle;
		var deleteModalBody = element.dataset.warningDeleteBody;
		
		var modalForm = dom.byId('modal-form');
		modalForm.action = deleteLink;
		
		var modalTitle = dom.byId('modal-title');
		modalTitle.innerHTML = deletePreModalTitle + ": " + deleteModalTitle;
		
		var modalBody = dom.byId('modal-body');
		modalBody.innerHTML = deleteModalBody;
	}
	
	return DeleteWarning;
});