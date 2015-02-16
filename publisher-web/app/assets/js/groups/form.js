require ([
	'dojo/dom',
	'dojo/on',
	'dojo/_base/window',
	'dojo/query',
	'dojo/dom-attr',
	'dojo/dom-construct',
	'listorder/listorder',
	'put-selector/put',
	
	'dojo/NodeList-traverse',
	
	'dojo/domReady!'
], function (dom, on, win, query, domattr, domConstruct, OrderedList, put) {
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
		
		if(naam !== "") {
			var el1 = put("div.list-group-item.gp-draggable.tree-item[value=$]", id);
			var el2 = put(el1, "input[type=hidden][name=structure][value=$]", id);
			var el3 = put(el1, "div.row");
			var el4 = put(el3, "div.col-sm-11.groupTree");
			var el5 = put(el4, "ul");
			var el6 = put(el5, "li");
			var el7 = put(el6, "a", naam);
			var el8 = put(el3, "div.col-sm-1");
			var el9 = put(el8, "div.pull-right.tree-item-delete");
			var el10 = put(el9, "a.btn.btn-default.btn-sm.delete-el[value=$]", id);
			var el11 = put(el10, "span.glyphicon.glyphicon-remove");
			
			put(main, el1);
		}
	});
	
	on(lagen,'change', function(evt) {
		var selectIndex = evt.currentTarget.options.selectedIndex;
		
		var id = evt.currentTarget[selectIndex].value;
		var naam = evt.currentTarget[selectIndex].innerHTML;
		
		if(naam !== "") {
			var el1 = put("div.list-group-item.gp-draggable.tree-item[value=$]", id);
			var el2 = put(el1, "input[type=hidden][name=structure][value=$]", id);
			var el3 = put(el1, "div.row");
			var el4 = put(el3, "div.col-sm-11.groupTree");
			var el5 = put(el4, "ul");
			var el6 = put(el5, "li");
			var el7 = put(el6, "a", naam);
			var el8 = put(el3, "div.col-sm-1");
			var el9 = put(el8, "div.pull-right.tree-item-delete");
			var el10 = put(el9, "a.btn.btn-default.btn-sm.delete-el[value=$]", id);
			var el11 = put(el10, "span.glyphicon.glyphicon-remove");
			
			put(main, el1);
		}
	});
	
	on(win.doc, ".delete-el:click", function(event) {
		var idItem = domattr.get(this, 'value');
		var itemToDel = query(".delete-el[value$=" + idItem + "]").closest(".list-group-item[value$=" + idItem + "]")[0];
		
		domConstruct.destroy(itemToDel);
	});
});