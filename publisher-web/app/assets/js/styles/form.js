$(function () {
	$('[data-toggle="popover"]').popover();
});

require ([
	'dojo/dom',
	'dojo/dom-class',
	'dojo/on',
	'dojo/request/xhr',
	'dojo/json',
	
	'put-selector/put',
	
	'ace/ace',

	'dojo/domReady!'
], function (
	dom,
	domClass,
	on,
	xhr,
	json,
	
	put,
	
	ace
) {
	var textarea = dom.byId ('input-definition'),
		styleForm = dom.byId ('style-form'),
		editorElement = put (textarea.parentNode, 'div[style="position: relative; width: 100%; height: 400px;"] div.form-control[style="width: 100%; height: 400px;"]');
	
	domClass.add (textarea, 'hidden');
	
	var editor = window.ace.edit (editorElement);
	
	editor.getSession ().setMode ('ace/mode/xml');
	editor.setValue (textarea.value, -1);
	editor.getSession ().addGutterDecoration (1, 'text-danger');
	
	on (styleForm, 'submit', function (e) {
		textarea.value = editor.getValue ();
	});
	
	var styleEditorElement = editorElement;
	
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
				editor.setValue (data.textContent, -1);
			}
		};
	}
	
	var overlay = null;
	
	on (styleEditorElement, 'dragenter', function (e) {
		// Do nothing if the transfer doesn't include files or if XMLHttpRequest is not supported by the browser:
		if (overlay !== null || !e.dataTransfer || !e.dataTransfer.types || !window.XMLHttpRequest) {
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
		
		overlay = put (styleEditorElement.parentNode, 'div[class="dnd-overlay"]');
		
		function endDrag () {
			put ('!', overlay);
			overlay = null;
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