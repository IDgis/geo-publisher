require ([
    'dojo/dom',
    'dojo/dom-class',
    'dojo/topic'
], function (
	dom,
	domClass,
	topic
) {
	topic.subscribe ('publisher/issues', function (issues) {
		if (issues.list.length > 0) {
			var hintNode = dom.byId ('new-issues-hint');
			console.log ('New issues');
			if (hintNode) {
				domClass.remove (hintNode, 'hidden');
			}
		}
	});
});