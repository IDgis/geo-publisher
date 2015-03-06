require ([
    'dojo/query',
    'dojo/dom-class',
    'dojo/topic',
    'dojo/on',
    'dojo/request/xhr',
    
	'dojo/domReady!'
], function (
	query,
	domClass,
	topic,
	on,
	xhr
) {
	
	var refreshButtons = query ('.js-datasource-refresh'),
		currentHarvesting = false;

	topic.subscribe ('publisher/active-tasks', function (tasks) {
		var isHarvesting = false;
		
		for (var i = 0; i < tasks.list.length; ++ i) {
			var task = tasks.list[i];
			if (task.message.type == 'HARVEST') {
				isHarvesting = true;
				break;
			}
		}
		
		if (isHarvesting === currentHarvesting) {
			return;
		}
		
		currentHarvesting = isHarvesting;
		
		refreshButtons.forEach (function (button) {
			domClass[isHarvesting ? 'add' : 'remove'] (button, 'disabled');
		});
	});
	
	refreshButtons.on ('click', function (e) {
		e.preventDefault ();
		e.stopPropagation ();
		
		if (domClass.contains (this, 'disabled')) {
			return;
		}
	
		xhr.post (jsRoutes.controllers.DataSources.refreshDatasources ().url, {
			handleAs: 'json'
		}).then (function () {
			topic.publish ('publisher/notification', 'info', 'Verversen van brongegevens is ingepland');
		}, function () {
			topic.publish ('publisher/notification', 'danger', 'Verversen van brongegevens kon niet worden ingepland');
		});
	});
});