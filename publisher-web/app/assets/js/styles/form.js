$(function () {
	$('[data-toggle="popover"]').popover();
});

require ([
	'dojo/dom',
	'dojo/on',
	'dojo/request/xhr',
	'dojo/json',
	
	'put-selector/put',

	'dojo/domReady!'
], function (
	dom,
	on,
	xhr,
	json,
	
	put
) {
	var styleEditorElement = dom.byId ('input-definition');
	
	window._geopublisherFileUploadCallback = function (content) {
		styleEditorElement.value = content;
	};
	
	function sendFile (file) {
		if (!window.XMLHttpRequest) {
			return;
		}
		
		var xmlRequest = new XMLHttpRequest ();
		xmlRequest.open ('POST', jsRoutes.controllers.Styles.handleFileUploadRaw ().url, true);
		xmlRequest.setRequestHeader ("Content-Type", file.type);
		xmlRequest.send (file);
		
		xmlRequest.onload = function () {
			var data = json.parse (this.responseText);
			if (data.valid) {
				styleEditorElement.value = data.textContent;
			}
		};
	}
	
	on (styleEditorElement, 'dragenter', function (e) {
		// Do nothing if the transfer doesn't include files or if XMLHttpRequest is not supported by the browser:
		if (!e.dataTransfer || !e.dataTransfer.types || !window.XMLHttpRequest) {
			return;
		}
		var hasFiles = false;
		for (var i = 0; i < e.dataTransfer.types.length; ++ i) {
			if (e.dataTransfer.types[i] == 'Files') {
				hasFiles = true;
				break;
			}
		}
		if (!hasFiles) {
			return;
		}
		
		e.stopPropagation ();
		e.preventDefault ();
		
		var overlay = put (styleEditorElement.parentNode, 'div[class="dnd-overlay"]');
		
		function endDrag () {
			put ('!', overlay);
		}
		
		on (overlay, 'dragenter', function (e) {
			e.stopPropagation ();
			e.preventDefault ();
		});
		on (overlay, 'dragover', function (e) {
			e.stopPropagation ();
			e.preventDefault ();
		});
		on (overlay, 'dragleave', function (e) {
			e.stopPropagation ();
			e.preventDefault ();
			
			endDrag ();
		});
		on (overlay, 'drop', function (e) {
			e.preventDefault ();
			e.stopPropagation ();
			
			endDrag ();
			
			if (e.dataTransfer && e.dataTransfer.files && e.dataTransfer.files.length >= 1) {
				sendFile (e.dataTransfer.files[0]);
			}
		});
	});
});