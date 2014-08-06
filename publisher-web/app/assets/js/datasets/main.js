/* jshint -W099 */
require ([
    'dojo/on',
    'dojo/query',
    'dojo/dom',
    'dojo/dom-attr',

    'dojo/topic',
    
    'dojo/request/xhr',
    
    'dojo/NodeList-traverse',
    'dojo/NodeList-dom',
    
	'dojo/domReady!'
], function (
	on,
	query,
	dom,
	domAttr,
	topic,
	xhr
) {
	on (dom.byId ('dataset-list'), '.js-dataset-refresh:click', function (e) {
		e.preventDefault ();
		e.stopPropagation ();

		var datasetItem = query (this).closest ('.js-dataset-item'), 
			datasetId = datasetItem.attr ('data-dataset-id')[0],
			datasetName = datasetItem.attr ('data-dataset-name')[0];
	
		xhr.post (jsRoutes.controllers.Datasets.scheduleRefresh (datasetId).url, {
			handleAs: 'json'
		}).then (function (data) {
			if (data.result && data.result == 'ok') {
				topic.publish ('publisher/notification', 'success', 'Verversen van dataset ' + datasetName + ' ingepland.');
			} else {
				topic.publish ('publisher/notification', 'danger', 'Kan verversen van dataset ' + datasetName + ' niet inplannen.');
			}
		}, function () {
			topic.publish ('publisher/notification', 'danger', 'Kan verversen van dataset ' + datasetName + ' niet inplannen.');
		});
	});
});