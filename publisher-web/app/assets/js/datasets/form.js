/* jshint expr:true */
require ([
    'dojo/_base/lang',
    'dojo/_base/array',
	'dojo/dom', 
	'dojo/dom-construct',
	'dojo/dom-attr',
	'dojo/dom-class',
	'dojo/query', 
	'dojo/on', 
	'dojo/request/xhr',
	'dojo/json',
	
	'put-selector/put',
	
	'dojo/domReady!'
], function(
	lang, 
	array,
	
	dom, 
	domConstruct, 
	domAttr, 
	domClass, 
	query, 
	on, 
	xhr, 
	json, 
	
	put
) {
	
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
	
	var currentId = null,
		updateIdTimeout = null,
		updateIdPromise = null;
	
	function updateId () {
		var id = idInput.value;
		
		// Do nothing if the value didn't change:
		if (id === currentId) {
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
			
			iconNode && domClass.remove (iconNode, ['glyphicon-remove', 'glyphicon-ok', 'rotating', 'glyphicon-warning-sign']);
			domClass.remove (idInput.parentNode.parentNode, ['has-error', 'has-success', 'has-warning']);
			
			if (currentId.length < 3) {
				iconNode && domClass.add (iconNode, 'glyphicon-warning-sign');
				domClass.add (idInput.parentNode.parentNode, 'has-warning');
				return;
			}
			
			iconNode && domClass.add (iconNode, ['glyphicon-refresh', 'rotating']);
	
			if (updateIdPromise) {
				updateIdPromise.cancel ();
			}
			
			updateIdPromise = xhr.get (jsRoutes.controllers.Datasets.getDatasetJson (currentId).url, {
				handleAs: 'json'
			}).then (function (data) {
				updateIdPromise = null;
				iconNode && domClass.remove (iconNode, ['glyphicon-refresh', 'rotating']);
				
				if (!data.status || data.status != 'notfound') {
					iconNode && domClass.add (iconNode, 'glyphicon-remove');
					domClass.add (idInput.parentNode.parentNode, 'has-error');
				} else {
					iconNode && domClass.add (iconNode, 'glyphicon-ok');
					domClass.add (idInput.parentNode.parentNode, 'has-success');
				}
			}, function () {
				updateIdPromise = null;
				iconNode && domClass.remove (iconNode, ['glyphicon-refresh', 'rotating']);
				iconNode && domClass.add (iconNode, 'glyphicon-remove');
				domClass.add (idInput.parentNode.parentNode, 'has-error');
			});
		}, 300);
	}
	
	on (idInput, 'keyup,change', updateId);
	
	updateId ();
	
	// =========================================================================
	// Filter editor:
	// =========================================================================
	var filterTextarea = query ('textarea[name="filterConditions"]')[0],
		filterEditorNode = dom.byId ('filter-editor');
	
	domClass.add (filterTextarea, 'hidden');

	function listColumns () {
		return array.map (query ('.js-column', dom.byId ('column-list')), function (columnNode) {
			return { 
				name: domAttr.get (columnNode, 'data-name'),
				type: domAttr.get (columnNode, 'data-type')
			};
		});
	}
	
	function buildOperatorExpression (expression) {
		var container = put ('div.list-group-item.js-operator'),
			row = put (container, 'div.row');
		
		var columnSelect = put (row, 'div.col-lg-4 select.form-control');
		put (row, 'div.col-lg-2 select.form-control');
		put (row, 'div.col-lg-4 input[type="text"].form-control');
		
		put (columnSelect, 'option[value=""] $', 'Kies een kolom ...');
		array.forEach (listColumns (), function (column) {
			put (columnSelect, 'option[value=$] $', column.name + ':' + column.type, column.name + ' (' + column.type + ')');
		});
			
		put (row, 'dov.col-lg-2 button.btn.btn-warning span.glyphicon.glyphicon-remove');
		
		return container;
	}
	
	function buildLogicalExpression (expression) {
		var type = expression.operatorType.toLowerCase (),
			isAnd = type == 'and',
			container = put ('div.js-operator.panel.panel-default.filter-operator.filter-operator-' + type),
			header = put (container, 'div.panel-heading $', isAnd ? 'Alle onderstaande condities zijn waar' : 'Tenminste één van onderstaande condities is waar'),
			body = put (container, 'div.panel-body'),
			list = isAnd ? put (body, 'div.list-group') : body,
			children = array.map (expression.inputs || [ { } ], buildExpression);
	
		domAttr.set (container, 'data-operator-type', type);
		domClass.add (list, 'js-list');
		
		for (var i = 0; i < children.length; ++ i) {
			if (!isAnd && i > 0) {
				put (list, 'div.filter-separator.filter-separator-or' + type + ' $', 'Of');
			}
			put (list, children[i]);
		}
		
		put (container, 'div.panel-footer button[type="button"].btn.btn-success.filter-add.filter-add-or.js-add-expression span.glyphicon.glyphicon-plus');
		
		return container;
	}
	
	function buildExpression (expression) {
		switch ((expression.operatorType || '').toLowerCase ()) {
		case "or":
		case "and":
			return buildLogicalExpression (expression);
		default:
			return buildOperatorExpression (expression);
		}
	}
	
	function buildFilterEditor (filter) {
		console.log ('Creating filter: ', filter);
		domConstruct.empty (filterEditorNode);
		
		put (filterEditorNode, buildExpression (filter.expression));
	}
	
	function listOperators (listNode) {
		return array.map (query ('> .js-operator', listNode), function (operatorNode) {
			var operatorType = domAttr.get (operatorNode, 'data-operator-type'),
				inputListNode = query ('.js-list', operatorNode)[0];
			
			if (operatorType) {
				// This is a container: AND or OR.
				return {
					type: 'operator',
					operatorType: operatorType.toUpperCase (),
					inputs: listOperators (inputListNode)
				};
			} else {
				// This is a binary or unary operator:
				return { };
			}
		});
	}

	function syncTextarea () {
		var filterObject = { expression: listOperators (filterEditorNode)[0] },
			value = json.stringify (filterObject);
		
		console.log ('Syncing filter: ', filterObject);
		filterTextarea.value = value;
	}
	
	buildFilterEditor (json.parse (filterTextarea.value));
	
	on (filterEditorNode, 'button.js-add-expression:click', function (e) {
		var containerNode = this.parentNode.parentNode,
			listNode = query ('.js-list', containerNode)[0],
			rootOperatorType = domAttr.get (containerNode, 'data-operator-type').toLowerCase (),
			expression = { };
		
		if (rootOperatorType == 'or') {
			expression = {
				type: 'operator',
				operatorType: 'AND'
			};
			put (listNode, 'div.filter-separator.filter-separator-' + rootOperatorType + ' $', rootOperatorType);
		}

		put (listNode, buildExpression (expression));
		
		syncTextarea ();
	});
});