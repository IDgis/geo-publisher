$(function () {
	$('[data-toggle="popover"]').popover();
});

require ([
	'dojo/dom',

	'dojo/domReady!'
], function (
	dom
) {
	var styleEditorElement = dom.byId ('input-definition');
	
	window._geopublisherFileUploadCallback = function (content) {
		console.log ('Setting content: ', content);
		styleEditorElement.value = content;
	};
});