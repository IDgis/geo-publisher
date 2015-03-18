require ([
	'dojo/dom',
	'dojo/on',
	'dojo/_base/window',
	'dojo/query',
	'dojo/dom-attr',
	'dojo/dom-construct',
	'listorder/listorder',

	'put-selector/put',
	
	'publisher/Pager',
	'publisher/Select',
	
	'dojo/NodeList-traverse',
	
	'dojo/domReady!'
], function (dom, on, win, query, domattr, domConstruct, OrderedList, put, Pager, Select) {
	/*
	 * Add and remove keywords
	 */
	var keywordbutton = dom.byId('add-keyword');
	var keywordlist = dom.byId('keyword-list');
	
	on(keywordbutton,'click', function(evt) {
		var keywordinput = dom.byId('input-keyword').value;
		
		if(keywordinput !== "") {
			var el1 = put("div.keyword-item-block[value=$]", keywordinput);
			var el2a = put(el1, "input[type=hidden][name=$][value=$]", "keywords[]", keywordinput);
			var el2b = put(el1, "div.keyword-item.label.label-primary", keywordinput);
			var el3 = put(el2b, "button.close[type=button][aria-hidden=true][value=$]", keywordinput);
			el3.innerHTML = "&times;";
			
			put(keywordlist, el1);
		}
		
		dom.byId('input-keyword').value = "";
	});
	
	on(win.doc, ".close:click", function(event) {
		
		
		var valueItem = domattr.get(this, 'value');
		
		var itemToDel = query(this).parents(".keyword-item-block")[0];
		domConstruct.destroy(itemToDel);
	});
	
	/*
	 * Group layerstructure
	 * Functionality to add groups and layers to (the rootgroup of) the service
	 */
	var main = dom.byId('groupLayerStructure');
	
	if(dom.byId ('groupLayerStructure')) {
		var list = new OrderedList ('#groupLayerStructure');
	}
	
	var groepen = dom.byId('addgroup');
	var lagen = dom.byId('addlayer');
	
	on(groepen,'change', function(evt) {
		var selectIndex = evt.currentTarget.options.selectedIndex;
		
		var id = evt.currentTarget[selectIndex].value;
		var naam = evt.currentTarget[selectIndex].innerHTML;
		var structureName="structure[]";
		console.log("groepnaam: " + naam);
		if(naam !== "") {
			var el1 = put("div.list-group-item.gp-draggable.tree-item[value=$]", id);
			var el2 = put(el1, "input[type=hidden][name=$][value=$]", structureName, id);
			var el3 = put(el1, "div.row");
			var el4 = put(el3, "div.col-sm-11.groupTree");
			var el5 = put(el4, "ul.treelist");
			var el6 = put(el5, "li");
			var el7 = put(el6, "a", naam);
			var el8 = put(el3, "div.col-sm-1");
			var el9 = put(el8, "div.pull-right.tree-item-delete");
			var el10 = put(el9, "a.btn.btn-warning.btn-sm.delete-el[value=$]", id);
			var el11 = put(el10, "span.glyphicon.glyphicon-remove");
			
			put(main, el1);
		}
	});
	
	function addLayer (id, label) {
		var naam = label;
		var structureName="structure[]";
		
		if(naam !== "") {
			var el1 = put("div.list-group-item.gp-draggable.tree-item[value=$]", id);
			var el2 = put(el1, "input[type=hidden][name=$][value=$]", structureName, id);
			var el3 = put(el1, "div.row");
			var el4 = put(el3, "div.col-sm-11.groupTree");
			var el5 = put(el4, "ul.treelist");
			var el6 = put(el5, "li");
			var el7 = put(el6, "a", naam);
			var el8 = put(el3, "div.col-sm-1");
			var el9 = put(el8, "div.pull-right.tree-item-delete");
			var el10 = put(el9, "a.btn.btn-warning.btn-sm.delete-el[value=$]", id);
			var el11 = put(el10, "span.glyphicon.glyphicon-remove");
			
			put(main, el1);
		}
	}
	
	on(win.doc, ".delete-el:click", function(event) {
		var idItem = domattr.get(this, 'value');
		var itemToDel = query(".delete-el[value$=" + idItem + "]").closest(".list-group-item[value$=" + idItem + "]")[0];
		
		domConstruct.destroy(itemToDel);
	});
	
	new Select ('#layers-select', {
		onSelect: function (item) {
			addLayer (item.id, item.label);
		}
	});
	new Pager ('#layers-select .js-dropdown');

});

$(function () {
	$('[data-toggle="popover"]').popover();
});