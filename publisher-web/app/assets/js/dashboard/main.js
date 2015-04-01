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
	var tasksPanel = dom.byId ('dashboard-panel-active-tasks'),
		notificationsPanel = dom.byId ('dashboard-panel-notifications'),
		issuesPanel = dom.byId ('dashboard-panel-issues');
	
	topic.subscribe ('publisher/active-tasks', function (activeTasks) {
		domConstruct.empty (tasksPanel);
		tasksPanel.innerHTML = activeTasks.content;
	});
	topic.subscribe ('publisher/notifications', function (notifications) {
		domConstruct.empty (notificationsPanel);
		notificationsPanel.innerHTML = notifications.content;
	});
});