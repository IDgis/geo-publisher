$(function () {
	$('[data-toggle="popover"]').popover();
});

require ([
	'dojo/dom',
	'dojo/on',
	'dojo/request/xhr',
	
	'put-selector/put',

	'dojo/domReady!'
], function (
	dom,
	on,
	xhr,
	
	put
) {
	var styleEditorElement = dom.byId ('input-definition');
	
	window._geopublisherFileUploadCallback = function (content) {
		styleEditorElement.value = content;
	};
	
	function sendFile (file) {
		console.log ('Sending file: ', file);
		var reader = new FileReader (file);
		
		reader.onload = function (e) {
			xhr.post ('/', {
				handleAs: 'json',
				data: e.target.result,
				headers: {
					'Content-Type': 'text/plain; charset=x-user-defined-binary'
				}
			});
		};
		
		reader.readAsBinaryString (file);
	}
	
	on (styleEditorElement, 'dragenter', function (e) {
		// Do nothing if the transfer doesn't include files:
		if (!e.dataTransfer || !e.dataTransfer.types) {
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
		
		var overlay = put (styleEditorElement.parentNode, 'div[style="position: absolute; left: 0px; top: 0px; right: 0px; bottom: 0px; background-color: red;"]');
		
		function endDrag () {
			put ('!', overlay);
		}
		
		on (overlay, 'dragenter', function (e) {
			e.stopPropagation ();
			e.preventDefault ();
			
			console.log ('dragenter overlay', e);
		});
		on (overlay, 'dragover', function (e) {
			e.stopPropagation ();
			e.preventDefault ();
		});
		on (overlay, 'dragleave', function (e) {
			e.stopPropagation ();
			e.preventDefault ();
			
			console.log ('leave', e);
			endDrag ();
		});
		on (overlay, 'drop', function (e) {
			e.preventDefault ();
			e.stopPropagation ();
			
			console.log ('drop', e, e.dataTransfer, e.dataTransfer.files);
			endDrag ();
			
			if (e.dataTransfer && e.dataTransfer.files && e.dataTransfer.files.length >= 1) {
				sendFile (e.dataTransfer.files[0]);
			}
		});
	});
});