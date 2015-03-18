require ([
    'dojo/query',
    'dojo/dom-class',
    'dojo/dom-attr',
    'dojo/topic',
    'dojo/on',
    'dojo/request/xhr',
    
	'dojo/domReady!'
], function (
	query,
	domClass,
	domAttr,
	topic,
	on,
	xhr
) {
	
	console.log('hello world!');
	
	var cvsOptions = query('#csv-options');	
	var csvOptionsShow = query ('#cvs-options-show');
	var csvOptionsHide = query ('#cvs-options-hide');
	
	console.log(cvsOptions);
	console.log(csvOptionsShow);
	console.log(csvOptionsHide);
	
	csvOptionsShow.on ('click', function (e) {
		e.preventDefault ();
		
		console.log('show clicked');
		
		domClass.add(csvOptionsShow[0], "hide");		
		domClass.remove(cvsOptions[0], "hide");
	});
	
	csvOptionsHide.on ('click', function (e) {
		e.preventDefault ();
		
		console.log('hide clicked');
		
		domClass.remove(csvOptionsShow[0], "hide");		
		domClass.add(cvsOptions[0], "hide");
	});
	
	var refreshButtons = query ('.js-datasource-refresh'),
		currentHarvesting = false;

	topic.subscribe ('publisher/active-tasks', function (tasks) {
		var isHarvesting = false,
			identifications = { };
		
		for (var i = 0; i < tasks.list.length; ++ i) {
			var task = tasks.list[i];
			if (task.message.type == 'HARVEST') {
				isHarvesting = true;
				identifications[task.message.properties.identification] = true;
				break;
			}
		}

		refreshButtons.forEach (function (button) {
			var id = domAttr.get (button, 'data-datasource-id');
			if (id) {
				domClass[id in identifications ? 'add' : 'remove'] (button, 'spinning');
			}
		});
		
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
		
		var datasourceId = domAttr.get (this, 'data-datasource-id'),
			url;
		if (datasourceId) {
			url = jsRoutes.controllers.DataSources.refreshDatasource (datasourceId).url;
		} else {
			url = jsRoutes.controllers.DataSources.refreshDatasources ().url;
		}
	
		xhr.post (url, {
			handleAs: 'json'
		}).then (function () {
			topic.publish ('publisher/notification', 'info', 'Verversen van brongegevens is ingepland');
		}, function () {
			topic.publish ('publisher/notification', 'danger', 'Verversen van brongegevens kon niet worden ingepland');
		});
	});
});