$(function () {
	$('[data-toggle="popover"]').popover();
});

require ([
	'dojo/dom',
	'dojo/dom-class',
	'dojo/dom-attr',
	'dojo/on',
	'dojo/request/xhr',
	'dojo/json',
	'dojo/query',
	
	'put-selector/put',
	
	'ace/ace',

	'dojo/domReady!'
], function (
	dom,
	domClass,
	domAttr,
	on,
	xhr,
	json,
	query,
	
	put,
	
	ace
) {
	var MAX_CHUNK_SIZE = 50 * 1024;
	
	var inputs = query ('input.js-style-definition'),
		styleForm = dom.byId ('style-form'),
		editorElement = put (inputs[0].parentNode, 'div.gp-editor-container div.form-control'),
		initialValue = '',
		errorLine = domAttr.get (dom.byId ('style-editor-container'), 'data-error-line'),
		errorMessage = domAttr.get (dom.byId ('style-editor-container'), 'data-error-message');
	
	inputs.forEach (function (input) {
		initialValue += input.value;
	});
	
	var editor = window.ace.edit (editorElement);
	
	editor.getSession ().setMode ('ace/mode/xml');
	editor.setValue (initialValue, -1);
	if (errorLine || errorMessage) {
		editor.getSession ().setAnnotations ([
			{
				row: errorLine ? errorLine - 1 : 0,
				column: 0,
				text: errorMessage || '',
				type: 'error'
			}
		]);
	}
	
	on (styleForm, 'submit', function (e) {
		var parent = inputs[0].parentNode,
			value = editor.getValue (),
			i = 0;
		
		inputs = query ('input.js-style-definition', parent);
		
		while (value.length > 0) {
			var chunk;
			
			if (value.length > MAX_CHUNK_SIZE) {
				chunk = value.substring (0, MAX_CHUNK_SIZE);
				value = value.substring (MAX_CHUNK_SIZE);
			} else {
				chunk = value;
				value = '';
			}
			
			// Create a textarea if required:
			var currentInput;
			if (i < inputs.length) {
				currentInput = inputs[i];
			} else {
				currentInput = put (parent, 'input.js-style-definition[type="hidden"]');
				domAttr.set (currentInput, 'name', 'definition[]');
			}
			
			currentInput.value = chunk;
			
			++ i;
		}
		
		for (; i < inputs.length; ++ i) {
			put (inputs[i], '!');
		}
	});
	
	var styleEditorElement = editorElement;
	
	window._geopublisherFileUploadCallback = function (content) {
		editor.setValue (content, -1);
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
	
	// Validate button:
	var validateButton = dom.byId ('validate-style-button');
	
	on (validateButton, 'click', function (e) {
		e.preventDefault ();
		e.stopPropagation ();
		
		var value = editor.getValue ();
		
		xhr.post (jsRoutes.controllers.Styles.validateSld ().url, {
			data: value,
			handleAs: 'json',
			headers: {
				'Content-Type': 'text/xml'
			}
		});
	});
});