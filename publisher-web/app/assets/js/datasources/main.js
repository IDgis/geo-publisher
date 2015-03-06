require ([
    'dojo/query',
    'dojo/dom-class',
    'dojo/topic',
    'dojo/on',
    
	'dojo/domReady!'
], function (
	query,
	domClass,
	topic,
	on
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
		
		domClass.add (this, 'disabled');
	});
});