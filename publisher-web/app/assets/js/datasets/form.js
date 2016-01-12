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

	'dojo/NodeList-traverse',
	'dojo/NodeList-dom',
	
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
	
	var columnList = dom.byId('column-list'),
		dataSourceSelect = dom.byId('input-datasource'),
		categorySelect = dom.byId('input-category'),
		datasetSelect = dom.byId('input-source-dataset'),		
		buttonSelectAllColumns = dom.byId ('button-select-all-columns'),
		buttonSelectNoColumns = dom.byId ('button-select-no-columns'),
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
				
				if(updateColumnCount() > 0) { // not all datasets have columns (e.g. raster data)
					tabs.forEach(function(tab) {
						domClass.remove(tab, 'hidden');
					});
				}
				updateFilterColumns ();
			});
		} else {
			updateColumnCount();
		
			tabs.forEach(function(tab) {
				domClass.add(tab, 'hidden');				
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
					
					var caption = dataset.name;
					if(dataset.alternateTitle) {
						caption += " / " + dataset.alternateTitle;
					}
					
					domConstruct.create('option', lang.mixin ({
						value: dataset.id,
						innerHTML: caption
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
	
	function selectAllColumns() {
		query('#tab-columns span.badge')[0].innerHTML =
			query('input', columnList)
				.filter(function(item) {
					item.checked=true;
					return item.checked;
				}).length;
	}
	
	on(columnList, '#button-select-all-columns:click', function(event) {
		selectAllColumns();
	});
	
	function selectNoColumns() {
		query('#tab-columns span.badge')[0].innerHTML =
			query('input', columnList)
				.filter(function(item) {
					item.checked=false;
					return item.checked;
				}).length;
	}
	
	on(columnList, '#button-select-no-columns:click', function(event) {
		selectNoColumns();
	});
	
	function getColumnCount() {
		return query('input', columnList)
			.filter(function(item) {
				return item.checked;
			}).length;
	}
	
	function updateColumnCount() {		
		columnCount = getColumnCount();		
		query('#tab-columns span.badge')[0].innerHTML = columnCount;
		return columnCount;
	}
	
	on(columnList, 'change', function(event) {
		updateColumnCount();
	});
	
	on(datasetSelect, 'change', function(event) {
		updateSourceDataset ();
	});
	
	updateColumnCount ();
	
	// =========================================================================
	// Associating data with DOM nodes:
	// =========================================================================
	var lastDataId = 1,
		domNodeData = { };
	
	function data (domNode) {
		var dataId = domAttr.get (domNode, 'data-id');
		if (!dataId) {
			dataId = lastDataId ++;
			domAttr.set (domNode, 'data-id', dataId);
			domNodeData[dataId] = { 
				handlers: [ ]
			};
		}
		
		return domNodeData[dataId];
	}
	
	function registerHandlers (domNode, handlers) {
		var d = data (domNode);
		
		handlers = lang.isArray (handlers) ? handlers : [ handlers ];
		for (var i = 0; i < handlers.length; ++ i) {
			d.handlers.push (handlers[i]);
		}
	}
	
	function clearData (domNode) {
		var dataId = domAttr.get (domNode, 'data-id'),
			d = domNodeData[dataId];
		
		if (!d) {
			return;
		}
		
		if (d.handlers) {
			for (var i = 0; i < d.handlers.length; ++ i) {
				d.handlers[i].remove ();
			}
		}
		
		delete domNodeData[dataId];
	}
	
	function removeNode (domNode) {
		query ('[data-id]', domNode).forEach (clearData);
		if (domAttr.get (domNode, 'data-id')) {
			clearData (domNode);
		}
		put (domNode, '!');
	}
	
	// =========================================================================
	// Filter editor:
	// =========================================================================
	var filterItem = query ('#dataset-filters'),
		dataFiltersEquals = filterItem.attr ('data-filters-equals')[0],
		dataFiltersNotEquals = filterItem.attr ('data-filters-not-equals')[0],
		dataFiltersLessThan = filterItem.attr ('data-filters-less-than')[0],
		dataFiltersLessThanEqual = filterItem.attr ('data-filters-less-than-equal')[0],
		dataFiltersGreaterThan = filterItem.attr ('data-filters-greater-than')[0],
		dataFiltersGreaterThanEqual = filterItem.attr ('data-filters-greater-than-equal')[0],
		dataFiltersLike = filterItem.attr ('data-filters-like')[0],
		dataFiltersIn = filterItem.attr ('data-filters-in')[0],
		dataFiltersNotNull = filterItem.attr ('data-filters-not-null')[0],
		dataFiltersArea = filterItem.attr ('data-filters-area')[0],
		dataFiltersOperation = filterItem.attr ('data-filters-operation')[0],
		dataFiltersColumn = filterItem.attr ('data-filters-column')[0],
		dataFiltersMissingColumn = filterItem.attr ('data-filters-missing-column')[0],
		dataFiltersConditionOne = filterItem.attr ('data-filters-condition-one')[0],
		dataFiltersConditionAll = filterItem.attr ('data-filters-condition-all')[0],
		dataFiltersOr = filterItem.attr ('data-filters-or')[0];
	
	var filterTextarea = query ('textarea[name="filterConditions"]')[0],
		filterEditorNode = dom.byId ('filter-editor'),
		typeOperatorMapping = {
			TEXT: [ 'EQUALS', 'NOT_EQUALS', 'LESS_THAN', 'LESS_THAN_EQUAL', 'GREATER_THAN', 'GREATER_THAN_EQUAL', 'LIKE', 'IN', 'NOT_NULL' ],
			NUMERIC: [ 'EQUALS', 'NOT_EQUALS', 'LESS_THAN', 'LESS_THAN_EQUAL', 'GREATER_THAN', 'GREATER_THAN_EQUAL', 'LIKE', 'IN', 'NOT_NULL' ],
			DATE: [ 'EQUALS', 'NOT_EQUALS', 'LESS_THAN', 'LESS_THAN_EQUAL', 'GREATER_THAN', 'GREATER_THAN_EQUAL', 'LIKE', 'IN', 'NOT_NULL' ],
			GEOMETRY: [ 'NOT_NULL' ]
		},
		operatorProperties = {
			EQUALS: {
				label: dataFiltersEquals,
				arity: 2
			},
			NOT_EQUALS: {
				label: dataFiltersNotEquals,
				arity: 2
			},
			LESS_THAN: {
				label: dataFiltersLessThan,
				arity: 2
			},
			LESS_THAN_EQUAL: {
				label: dataFiltersLessThanEqual,
				arity: 2
			},
			GREATER_THAN: {
				label: dataFiltersGreaterThan,
				arity: 2
			},
			GREATER_THAN_EQUAL: {
				label: dataFiltersGreaterThanEqual,
				arity: 2
			},
			LIKE: {
				label: dataFiltersLike,
				arity: 2
			},
			IN: {
				label: dataFiltersIn,
				arity: 2
			},
			NOT_NULL: {
				label: dataFiltersNotNull,
				arity: 1
			}
		},
		typeValidators = {
			TEXT: function (value) { return true; },
			NUMERIC: function (value) { return /^(\-)?[0-9]+(\.[0-9]+)?$/.test (value); },
			DATE: function (value) { return /^[0-9]{4}\-[0-9]{2}\-[0-9]{2}$/.test (value); },
			GEOMETRY: function (value) { return false; }
		};
	
	// Hide the original textarea, it is only used to submit the data.
	domClass.add (filterTextarea, 'hidden');

	function slowRemoveNode (domNode, callback) {
		domClass.add (domNode, ['fade', 'out']);
		setTimeout (function () {
			removeNode (domNode);
			callback ();
		}, 300);
	}
	
	/**
	 * Returns a list of available columns. 
	 */
	function listColumns () {
		var columns = [ ];
		array.forEach (query ('.js-column', dom.byId ('column-list')), function (columnNode) {
			var name = domAttr.get (columnNode, 'data-name'),
				type = domAttr.get (columnNode, 'data-type');
			
			columns.push ({
				name: name,
				label: name + ' (' + type + ')',
				type: type
			});
			
			if (type == 'GEOMETRY') {
				columns.push ({
					name: name + '.area', 
					label: dataFiltersArea + ' ' + name + ' (NUMERIC)',
					type: 'NUMERIC'
				});
			}
		});
		
		return columns;
	}
	
	/**
	 * Updates the columns for all filter expressions. Invoked after the user switches datasets
	 * and a new set of columns becomes available. 
	 */
	function updateFilterColumns () {
		var columns = listColumns ();
		query ('.js-operator-compare', filterEditorNode).forEach (function (expressionNode) {
			updateExpressionColumns (expressionNode, columns);
		});
		syncTextarea ();
	}
	
	/**
	 * Sets the value of the hidden textarea based on the contents of the filter editor: keeps
	 * the textarea in sync with the rich DOM interface.
	 */
	function syncTextarea () {
		var expression = listOperators (filterEditorNode)[0],
			filterObject = { expression: expression && expression.inputs && expression.inputs.length > 0 ? expression : null },
			value = json.stringify (filterObject);
		
		filterTextarea.value = value;
		
		// Count the number of filters:
		var filterCount = 0;
		if (expression && expression.inputs) {
			array.forEach (expression.inputs, function (andExpression) {
				array.forEach (andExpression.inputs, function (operatorExpression) {
					++ filterCount;
				});
			});
		}
		
		query ('.js-filter-count').forEach (function (node) {
			domConstruct.empty (node);
			put (node, document.createTextNode (filterCount));
		});
	}

	/**
	 * Show or hide the remove button on child expressions of this container. Remove buttons are only
	 * visible if the container has at least two child expressions.
	 */
	function updateRemoveButtons (listNode) {
		var childExpressions = query ('> .js-operator', listNode);
		childExpressions.forEach (function (expressionNode) {
			var d = data (expressionNode);
			domClass[childExpressions.length > 1 ? 'remove' : 'add'] (d.removeButton, 'hidden');
		});
	}
	
	/**
	 * Validates the expression and sets the error states. Validates whether the entered value matches the
	 * type.
	 */
	function validateExpression (expressionNode) {
		var d = data (expressionNode),
			column = d.column,
			operator = d.operator,
			value = d.value,
			isValid = false;

		if (!column || column === '' || !operator || operator === '') {
			isValid = true;
		} else {
			var type = column.substring (column.indexOf (':') + 1);
			if (operator == 'IN') {
				isValid = true;
				array.forEach (value.split (','), function (part) {
					if (!typeValidators[type] (lang.trim (part))) {
						isValid = false;
					}
				});
			} else {
				isValid = typeValidators[type] (value);
			}
		}
		
		domClass[isValid ? 'remove' : 'add'] (d.valueInput.parentNode, 'has-error');
	}
	
	/**
	 * Hides or removes the value input based on the arity of the selected operator.
	 */
	function updateValueInput (expressionNode) {
		var d = data (expressionNode),
			operator = d.operator,
			arity = operator && operator !== '' ? operatorProperties[operator].arity : 1;
			
		domClass[arity == 1 ? 'add' : 'remove'] (d.valueInput, 'hidden');
		
		validateExpression (expressionNode);
	}
	
	/**
	 * Updates the set of available operators. Does the following:
	 * - Removes the current list of operators.
	 * - Adds the new list of operators and sets the selected operator if the operator still exists.
	 * - If the selected operator doesn't exist, add it to the end and mark it as removed. The
	 *   removed operator can't be selected again.
	 */
	function updateOperatorList (expressionNode) {
		var d = data (expressionNode),
			column = d.column;
		
		domConstruct.empty (d.operatorSelect);
		
		// Show or hide the select and the determine the list of operators:
		var operators;
		if (!column || column === '') {
			domAttr.set (d.operatorSelect, 'disabled', 'disabled');
			operators = [ ];
		} else {
			domAttr.remove (d.operatorSelect, 'disabled');
			var type = column.substring (column.indexOf (':') + 1);
			operators = typeOperatorMapping[type];
		}
		
		// Add the empty operator:
		domAttr.set (put (d.operatorSelect, 'option[value=$]' + (!d.operator || d.operator === '' ? '[selected]' : '') + ' $', '', dataFiltersOperation), 'value', '');

		// Add the operators that apply to this column type:
		var hasSelection = !d.operator || d.operator === '';
		array.forEach (operators, function (operatorName) {
			var selected = d.operator === operatorName;
			put (
				d.operatorSelect,
				'option[value=$]' + (selected ? '[selected]' : '') + ' $', operatorName, operatorProperties[operatorName].label
			);
			if (selected) {
				hasSelection = true;
			}
		});
		
		// Add the psuedo operator:
		if (!hasSelection && d.operator && d.operator !== '') {
			put (d.operatorSelect, 'option[value="-"][selected] span.text-danger $', operatorProperties[d.operator].label);
			domClass.add (d.operatorSelect.parentNode, 'has-error');
		} else {
			domClass.remove (d.operatorSelect.parentNode, 'has-error');
		}
		
		// Update the value input:
		updateValueInput (expressionNode);
	}
	
	/**
	 * Updates the list of columns for an expression. Does the following:
	 * - Removes the current list of columns.
	 * - Adds the new columns and sets the selected column if the column still exists.
	 * - If the selected column doesn't exist, it adds a final "pseudo" column that is marked as removed and can't be selected.
	 */
	function updateExpressionColumns (expressionNode, columns) {
		var d = data (expressionNode);
		
		domConstruct.empty (d.columnSelect);
		
		// Add the no-selection column:
		domAttr.set (put (
			d.columnSelect, 
			'option[value=$]' + (!d.column || d.column === '' ? '[selected]' : '')+ ' $',
			'',
			dataFiltersColumn
		), 'value', '');
		
		// Add the column values:
		var hasSelection = !d.column || d.column === '';
		array.forEach (columns, function (column) {
			var selected = d.column === column.name + ':' + column.type;
			put (
				d.columnSelect,
				'option[value=$]' + (selected ? '[selected]' : '') + ' $', 
				column.name + ':' + column.type, 
				column.label
			);
			if (selected) {
				hasSelection = true;
			}
		});
		
		// Add a pseudo column:
		if (!hasSelection && d.column && d.column !== '') {
			var offset = d.column.indexOf (':'),
				name = d.column.substring (0, offset),
				type = d.column.substring (offset + 1);
			
			put (d.columnSelect, 'option[value="-"][selected] span.text-danger $', dataFiltersMissingColumn + ' ' + name + ' (' + type + ')');
			domClass.add (d.columnSelect.parentNode, 'has-error');
		} else {
			domClass.remove (d.columnSelect.parentNode, 'has-error');
		}
		
		// Update the list of operators:
		updateOperatorList (expressionNode);
	}
	
	function onChangeColumn (expression) {
		var d = data (expression),
			columnValue = d.columnSelect.value;
		
		// Ignore the psuedo column:
		if (columnValue == '-') {
			return;
		}
		
		d.column = columnValue;
		
		// Remove the psuedo element:
		query ('option[value="-"]', d.columnSelect).forEach (domConstruct.destroy);
		domClass.remove (d.columnSelect.parentNode, 'has-error');
		
		// Update the operators:
		updateOperatorList (expression);
		syncTextarea ();
	}
	
	function onChangeOperator (expression) {
		var d = data (expression),
			operatorValue = d.operatorSelect.value;
		
		// Ignore the psuedo-operator:
		if (operatorValue == '-') {
			return;
		}
		
		d.operator = operatorValue;
		
		// Remove the psuedo element:
		query ('option[value="-"]', d.operatorSelect).forEach (domConstruct.destroy);
		domClass.remove (d.operatorSelect.parentNode, 'has-error');
		
		// Update the value input:
		updateValueInput (expression);
		syncTextarea ();
	}
	
	function onChangeValue (expression, sync) {
		var d = data (expression),
			value = d.valueInput.value;
		
		d.value = value;
		
		// Validate the value:
		validateExpression (expression);
		if (sync) {
			syncTextarea ();
		}
	}
	
	function onRemoveExpression (expressionContainer) {
		var parent = expressionContainer.parentNode;
		
		// Locate a separator before this container and remove it:
		var prev = query (expressionContainer).prev ()[0];
		if (prev && domClass.contains (prev, 'js-filter-separator')) {
			slowRemoveNode (prev, function () { });
		}
		
		// Remove this node:
		slowRemoveNode (expressionContainer, function () {
			// Update the textarea and remove buttons:
			syncTextarea ();
			updateRemoveButtons (parent);
		});
	}
	
	function buildOperatorExpression (expression) {
		var container = put ('div.list-group-item.js-operator.js-operator-compare'),
			row = put (container, 'div.row'),
			d = data (container);
		
		d.columnSelect = put (row, 'div.col-lg-4 select.form-control');
		d.operatorSelect = put (row, 'div.col-lg-2 select.form-control');
		d.valueInput = put (row, 'div.col-lg-4 input[type="text"].form-control');
		
		d.operator = expression.operatorType || '';
		d.value = '';
		d.column = '';

		// Set initial values for value and column:
		if (expression.inputs && expression.inputs.length >= 1 && expression.inputs[0].type == 'column-ref') {
			d.column = expression.inputs[0].column.name + ':' + expression.inputs[0].column.dataType;
		}
		if (expression.inputs && expression.inputs.length == 2 && expression.inputs[1].type == 'value') {
			d.value = expression.inputs[1].value;
			d.valueInput.value = d.value;
		}
		
		d.removeButton = put (row, 'div.col-lg-2.text-right button[type="button"].btn.btn-warning span.glyphicon.glyphicon-remove <');
		
		// Register event handlers:
		registerHandlers (container, [
			on (d.columnSelect, 'change', function (e) { onChangeColumn (container); }),
			on (d.operatorSelect, 'change', function (e) { onChangeOperator (container); }),
			on (d.valueInput, 'keyup', function (e) { onChangeValue (container, false); }),
			on (d.valueInput, 'change,blur', function (e) { onChangeValue (container, true); }),
			on (d.removeButton, 'click', function (e) { onRemoveExpression (container); e.preventDefault (); })
		]);
		
		// Set the columns:
		updateExpressionColumns (container, listColumns ());
		
		return container;
	}
	
	function buildLogicalExpression (expression) {
		var type = expression.operatorType.toLowerCase (),
			isAnd = type == 'and',
			container = put ('div.js-operator.panel.panel-default.filter-operator.filter-operator-' + type),
			header = put (container, 'div.panel-heading div.row'),
			body = put (container, 'div.panel-body'),
			list = isAnd ? put (body, 'div.list-group') : body,
			inputs = expression.inputs || [ (isAnd ? { } : { type: 'operator', operatorType: 'AND' }) ],
			children = array.map (inputs, buildExpression),
			d = data (container);
	
		domAttr.set (container, 'data-operator-type', type);
		domClass.add (list, 'js-list');

		// Create header:
		put (header, 'div.col-lg-10 $', isAnd ? dataFiltersConditionAll : dataFiltersConditionOne);
		d.removeButton = put (header, 'div.col-lg-2.text-right button[type="button"].btn.btn-warning span.glyphicon.glyphicon-remove <');
		
		for (var i = 0; i < children.length; ++ i) {
			if (!isAnd && i > 0) {
				put (list, 'div.js-filter-separator.filter-separator.filter-separator-or.text-center.h3 span.label.label-info $', dataFiltersOr);
			}
			put (list, children[i]);
		}
		
		put (container, 'div.panel-footer button[type="button"].btn.btn-success.filter-add.filter-add-or.js-add-expression span.glyphicon.glyphicon-plus');
		
		// Register event handlers:
		registerHandlers (container, [
			on (d.removeButton, 'click', function (e) { onRemoveExpression (container); e.preventDefault (); })
		]);
		
		// Set the remove buttons on this container:
		updateRemoveButtons (list);
		
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
		domConstruct.empty (filterEditorNode);
		
		put (filterEditorNode, buildExpression (filter.expression || { 
			type: 'operator',
			operatorType: 'OR'
		}));
		updateRemoveButtons (filterEditorNode);
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
					inputs: array.filter (listOperators (inputListNode), function (item) { 
						return item !== null && (!item.inputs || item.inputs.length > 0); 
					})
				};
			} else {
				// This is a binary or unary operator:
				var d = data (operatorNode);
					column = d.column,
					operator = d.operator,
					value = d.value,
					exp = {
						type: 'operator',
						operatorType: operator && operator !== '' ? operator : null,
						inputs: [ ]
					};

				if ((!column || column === '') && (!operator || operator === '') && lang.trim (value) === '') {
					return null;
				}
				
				var name, type;
				
				if (column && column !== '') {
					var offset = column.indexOf (':');
					
					name = column.substring (0, offset);
					type = column.substring (offset + 1);
				} else {
					name = null;
					type = null;
				}
					
				exp.inputs.push ({
					type: 'column-ref',
					column: {
						name: name,
						dataType: type
					}
				});
				
				if (operator && operatorProperties[operator].arity > 1) {
					exp.inputs.push ({
						type: 'value',
						value: value || '',
						valueType: operator == 'IN' ? 'TEXT' : type
					});
				}

				return exp;
			}
		});
	}

	buildFilterEditor (json.parse (filterTextarea.value));
	syncTextarea ();
	
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
			put (listNode, 'div.js-filter-separator.filter-separator.filter-separator-or.text-center.h3 span.label.label-info $', dataFiltersOr);
		}

		put (listNode, buildExpression (expression));
		
		// Update the remove buttons on this expression:
		updateRemoveButtons (listNode);
		
		syncTextarea ();
	});
});

$(function () {
	$('[data-toggle="popover"]').popover();
});