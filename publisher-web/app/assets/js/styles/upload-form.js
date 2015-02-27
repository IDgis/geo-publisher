require ([
	'dojo/dom',
	'dojo/domReady!'
], function (
	dom
) {
	var textarea = dom.byId ('text-content');
	
	if (textarea) {
		window.parent._geopublisherFileUploadCallback (textarea.value);
	}
});