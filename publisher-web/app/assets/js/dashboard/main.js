require ([
    'dojo/dom',
	'dojo/dom-construct',
	'dojo/query',
	'dojo/topic', 
	'dojo/domReady!'
], function (
	dom,
	domConstruct,
	query,
	topic
) {
	
	// =========================================================================
	// Active tasks:
	// =========================================================================
	var tasksPanel = dom.byId ('dashboard-panel-active-tasks');
	
	topic.subscribe ('publisher/active-tasks', function (activeTasks) {
		domConstruct.empty (tasksPanel);
		tasksPanel.innerHTML = activeTasks.content;
	});
});