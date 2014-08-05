require ([
    'dojo/_base/lang',
	'dojo/dom', 
	'dojo/dom-construct',
	'dojo/dom-attr',
	'dojo/dom-class',
	'dojo/query', 
	'dojo/on', 
	'dojo/request/xhr', 
	
	'dojo/domReady!'], 
	
function(lang, dom, domConstruct, domAttr, domClass, query, on, xhr) {
	
	var dataSourceSelect = dom.byId('input-datasource'),
		categorySelect = dom.byId('input-category'),
		datasetSelect = dom.byId('input-source-dataset'),
		idInput = dom.byId ('input-id'),
		dataSource = dataSourceSelect.value,
		category = categorySelect.value;	
	
	function updateSourceDataset () {
		var id = datasetSelect.value;
		
		var tabs = query('#tab-columns, #tab-filters');		
		domConstruct.empty(columnList);
		
		if(id !== '') {
			var route = jsRoutes.controllers.Datasets.listColumnsAction(dataSource, id);
			xhr.get(route.url, {
				handleAs: 'text'
			}).then (function(data) {					
				columnList.innerHTML = data;
				
				updateColumnCount();
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
	}
		
	function onFormChange() {
		query('option', datasetSelect)
			.filter(function(option) { return domAttr.get(option, 'value') !== ''; })
			.forEach(domConstruct.destroy);			
			
		if(category !== '' && dataSource !== '') {
			var route = jsRoutes.controllers.DataSources.listByDataSourceAndCategoryJson(dataSource, category, 1);
			xhr.get(route.url, {
				handleAs: 'json'
			}).then (function(data) {
				var previousValue = domAttr.get (datasetSelect, 'data-original-value') || '';
				
				for(var i = 0; i < data.sourceDatasets.length; i++) {
					var dataset = data.sourceDatasets[i];
					
					domConstruct.create('option', lang.mixin ({
						value: dataset.id,
						innerHTML: dataset.name
					}, dataset.id == previousValue ? { selected: 'selected' } : { }), datasetSelect);	
				}
				
				domAttr.remove(datasetSelect, 'disabled');
				domAttr.remove (datasetSelect, 'data-original-value');
				
				updateSourceDataset ();
			});
		} else {
			domAttr.set(datasetSelect, 'disabled', 'disabled');
		}
	}
		
	on(dataSourceSelect, 'change', function(event) {
		dataSource = this.value;		
		onFormChange();
	});
	
	on(categorySelect, 'change', function(event) {
		category = this.value;
		onFormChange();
	});
	
	var columnList = dom.byId('column-list');
	
	function updateColumnCount() {
		query('#tab-columns span.badge')[0].innerHTML =
			query('input', columnList)
				.filter(function(item) {
					return item.checked;
				}).length;
	}
	
	on(columnList, 'change', function(event) {
		updateColumnCount();
	});
	
	on(datasetSelect, 'change', function(event) {
		updateSourceDataset ();
	});
	
	updateColumnCount ();
	
	// =========================================================================
	
	var currentId = '',
		updateIdTimeout = null,
		updateIdPromise = null;
	
	function updateId () {
		var id = idInput.value;
		
		// Do nothing if the value didn't change:
		if (id == currentId) {
			return;
		}
		currentId = id;

		// Clear the current timeout:
		if (updateIdTimeout) {
			clearTimeout (updateIdTimeout);
			updateIdTimeout = null;
		}
		
		updateIdTimeout = setTimeout (function () {
			console.log ('updateId: ', id);
			
			updateIdTimeout = null;
			
			var iconNode = query ('span.glyphicon', idInput.parentNode)[0];
			
			domClass.remove (iconNode, ['glyphicon-remove', 'glyphicon-ok', 'rotating', 'glyphicon-warning-sign']);
			domClass.remove (idInput.parentNode.parentNode, ['has-error', 'has-success', 'has-warning']);
			
			if (currentId.length < 3) {
				domClass.add (iconNode, 'glyphicon-warning-sign');
				domClass.add (idInput.parentNode.parentNode, 'has-warning');
				return;
			}
			
			domClass.add (iconNode, ['glyphicon-refresh', 'rotating']);
	
			if (updateIdPromise) {
				updateIdPromise.cancel ();
			}
			
			updateIdPromise = xhr.get (jsRoutes.controllers.Datasets.getDatasetJson (currentId).url, {
				handleAs: 'json'
			}).then (function (data) {
				updateIdPromise = null;
				domClass.remove (iconNode, ['glyphicon-refresh', 'rotating']);
				
				if (!data.result || data.result != 'notfound') {
					domClass.add (iconNode, 'glyphicon-remove');
					domClass.add (idInput.parentNode.parentNode, 'has-error');
				} else {
					domClass.add (iconNode, 'glyphicon-ok');
					domClass.add (idInput.parentNode.parentNode, 'has-success');
				}
			}, function () {
				updateIdPromise = null;
				domClass.remove (iconNode, ['glyphicon-refresh', 'rotating']);
				domClass.add (iconNode, 'glyphicon-remove');
				domClass.add (idInput.parentNode.parentNode, 'has-error');
			});
		}, 300);
	}
	
	on (idInput, 'keyup,change', updateId);
	
	updateId ();
});