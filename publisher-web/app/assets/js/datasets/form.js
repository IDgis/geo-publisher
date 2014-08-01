require ([
	'dojo/dom', 
	'dojo/dom-construct',
	'dojo/dom-attr',
	'dojo/dom-class',
	'dojo/query', 
	'dojo/on', 
	'dojo/_base/xhr', 
	
	'dojo/domReady!'], 
	
	function(dom, domConstruct, domAttr, domClass, query, on, xhr) {
	
	var dataSource = '';
	var category = '';	
	
	var datasetSelect = dom.byId('input-source-dataset');
		
	function onFormChange() {
		query('option', datasetSelect)
			.filter(function(option) { return domAttr.get(option, 'value') !== ''; })
			.forEach(domConstruct.destroy);			
			
		if(category !== '' && dataSource !== '') {
			var route = jsRoutes.controllers.DataSources.listByDataSourceAndCategoryJson(dataSource, category, 1);
			xhr.get({
				url: route.url, handleAs: 'json',
				load: function(data) {
					for(var i = 0; i < data.sourceDatasets.length; i++) {
						var dataset = data.sourceDatasets[i];
						
						domConstruct.create('option', {
							value: dataset.id,
							innerHTML: dataset.name
						}, datasetSelect);	
					}
					
					domAttr.remove(datasetSelect, 'disabled');
				}
			});
		} else {
			domAttr.set(datasetSelect, 'disabled', 'disabled');
		}
	}
		
	on(dom.byId('input-datasource'), 'change', function(event) {
		dataSource = event.target.value;		
		onFormChange();
	});
	
	on(dom.byId('input-category'), 'change', function(event) {
		category = event.target.value;
		onFormChange();
	});
	
	var columnList = dom.byId('column-list');
	
	function updateColumnCount() {
		query('#tab-columns span')[0].innerHTML =
			query('input', columnList)
				.filter(function(item) {
					return item.checked;
				}).length;
	}
	
	on(columnList, 'change', function(event) {
		updateColumnCount();
	});
	
	on(datasetSelect, 'change', function(event) {
		var id = event.target.value;
		
		var tabs = query('#tab-columns, #tab-filters');		
		domConstruct.empty(columnList);
		
		if(id !== '') {
			var route = jsRoutes.controllers.DataSources.listColumns(dataSource, id);
			xhr.get({
				url: route.url, handleAs: 'text',
				load: function(data) {					
					
					columnList.innerHTML = data;
					
					updateColumnCount();
				}
			});
		
			tabs.forEach(function(tab) {
				domClass.remove(tab, 'disabled');
				query('a', tab)
					.forEach(function(link) {
						domAttr.set(link, 'data-toggle', 'tab'); 
				});
			});
		} else {
			updateColumnCount();
		
			tabs.forEach(function(tab) {
				domClass.add(tab, 'disabled');
				query('a', tab)
					.forEach(function(link) {
						domAttr.remove(link, 'data-toggle'); 
				});
			});
		}
	});
});