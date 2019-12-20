require ([
	'dojo/dom',
	'dojo/on',
	'dojo/dom-attr',
	'dojo/request/xhr',
	'dojo/topic',
	
	'dojo/domReady!'
], function (dom, on, domAttr, xhr, topic) {
	on(dom.byId('js-usergroups-cleanup'), 'click', function(e) {
		var url = jsRoutes.controllers.LdapUserGroups.cleanupUserGroups().url;
		
		var cleanupSuccess = domAttr.get(this, 'data-cleanup-success'),
			cleanupFailure = domAttr.get(this, 'data-cleanup-failure');
		
		xhr.post (url, {
			handleAs: 'json'
		}).then (function () {
			topic.publish ('publisher/notification', 'info', cleanupSuccess);
		}, function () {
			topic.publish ('publisher/notification', 'danger', cleanupFailure);
		});
	});
});
