$(function () {
	$('[data-toggle="popover"]').popover();
});
require ([
	'dojo/_base/array',
	
	'dojo/query',
	'dojo/dom-attr',
	'dojo/on',
	
	'dojo/domReady!'
], function (
	array,
	query,
	domAttr,
	on
) {
	var canPublish = domAttr.get (query ('form[data-can-publish]')[0], 'data-can-publish') == 'true';
	
	var inputs = query ('form input[type="checkbox"]'),
		submitButton = query ('form button[type="submit"]')[0];
	
	function updateSubmitButton () {
		submitButton.disabled = !array.every (inputs, function (i) { return !i.checked; });
	}
	
	if (!canPublish) {
		array.forEach (inputs, function (input) {
			on (input, 'change', updateSubmitButton);
		});
		
		updateSubmitButton ();
	}
});