/* jshint -W099 */
require ([
    'dojo/_base/array',
    'dojo/on',
    'dojo/_base/window',
    'dojo/query',
    'dojo/dom',
    'dojo/dom-attr',
    'dojo/dom-class',
    'dojo/dom-style',
    'delete-warning/delete-warning',

    'dojo/topic',
    
    'dojo/request/xhr',
    
    'dojo/NodeList-traverse',
    'dojo/NodeList-dom',
    
	'dojo/domReady!'
], function (
	array,
	on,
	win,
	query,
	dom,
	domAttr,
	domClass,
	domStyle,
	DeleteWarning,
	topic,
	xhr
) {
	if (dom.byId ('dataset-list')) {
		on (dom.byId ('dataset-list'), '.js-dataset-refresh:click', function (e) {
			e.preventDefault ();
			e.stopPropagation ();
	
			var datasetItem = query (this).closest ('.js-dataset-item'), 
				datasetId = datasetItem.attr ('data-dataset-id')[0],
				datasetName = datasetItem.attr ('data-dataset-name')[0];
		
			domClass.add (query ('.js-icon', this)[0], 'rotating');
			
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
	}
	
	// =========================================================================
	// Jobs:
	// =========================================================================
	var previousProgress = { };
	
	topic.subscribe ('publisher/active-tasks', function (tasks) {
		var taskProgress =  { };
		
		array.forEach (tasks.list, function (task) {
			if (!task.message.properties || task.message.type != 'IMPORT' || task.message.properties.entityType != 'DATASET') {
				return;
			}
			
			var identification = task.message.properties.identification,
				progress = task.progress;
			
			taskProgress[identification] = progress;
		});
		
		query ('.js-dataset-item').forEach (function (rowNode) {
			var identification = domAttr.get (rowNode, 'data-dataset-id'),
				statusNode = query ('.js-status', rowNode)[0],
				progressNode = query ('.js-progress', rowNode)[0],
				linkNode = query ('.js-dataset-refresh', rowNode)[0],
				iconNode = query ('.js-icon', linkNode)[0];
			
			if (!(identification in taskProgress)) {
				var showStatus = function() {
					domClass.remove (statusNode, 'hidden');
					domClass.add (progressNode, 'hidden');
					domClass.remove (linkNode, 'disabled');
				};
				
				if (identification in previousProgress) {			
					var route = jsRoutes.controllers.Datasets.status(identification);
					xhr.get(route.url, {
						handleAs: 'text'
					}).then (function(data) {
						statusNode.innerHTML = data;
						
						showStatus();
												
						domClass.remove (iconNode, 'rotating');
					});				
				} else {
					showStatus();
				}
				
				return;
			}
			
			domClass.add (statusNode, 'hidden');
			domClass.remove (progressNode, 'hidden');
			
			var bar = query ('.progress-bar', progressNode)[0];
			
			domAttr.set (bar, 'aria-valuenow', taskProgress[identification]);
			domStyle.set (bar, 'width', taskProgress[identification] + '%');
			domClass.add (linkNode, 'disabled');
			domClass.add (iconNode, 'rotating');
		});
		
		previousProgress = taskProgress;
	});
	
	on(win.doc, ".deleteButton:click", function(event) {
		var itemToDel = query(this).parents(".list-group-item")[0];
		
		var deleteWarning = new DeleteWarning (itemToDel);
	});
});