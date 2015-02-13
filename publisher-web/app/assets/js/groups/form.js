require ([
	'dojo/dom',
	'dojo/on',
	'dojo/_base/lang',
	'listorder/listorder',
	'put-selector/put',
	
	'dojo/domReady!'
], function (dom, on, lang, OrderedList, put) {
	var main = dom.byId('groepstructuur');
	
	if(dom.byId ('groepstructuur')) {
		var list = new OrderedList ('#groepstructuur');
	}
	
	var groepen = dom.byId('addgroup');
	var lagen = dom.byId('addlayer');
	
	on(groepen,'change', function(evt) {
		var selectIndex = evt.currentTarget.options.selectedIndex;
		
		var id = evt.currentTarget[selectIndex].value;
		var naam = evt.currentTarget[selectIndex].innerHTML;
		
		var el1 = put("div.list-group-item.gp-draggable.tree-item");
		var el2 = put(el1, "input[type=hidden][name=structure][value=$]", id);
		var el3 = put(el1, "div.row");
		var el4 = put(el3, "div.col-sm-8.groupTree");
		var el5 = put(el4, "ul");
		var el6 = put(el5, "li");
		var el7 = put(el6, "a", naam);
		
		put(main, el1);
	});
	
	on(lagen,'change', function(evt) {
		var selectIndex = evt.currentTarget.options.selectedIndex;
		
		var id = evt.currentTarget[selectIndex].value;
		var naam = evt.currentTarget[selectIndex].innerHTML;
		
		var el1 = put("div.list-group-item.gp-draggable.tree-item");
		var el2 = put(el1, "input[type=hidden][name=structure][value=$]", id);
		var el3 = put(el1, "div.row");
		var el4 = put(el3, "div.col-sm-8.groupTree");
		var el5 = put(el4, "ul");
		var el6 = put(el5, "li");
		var el7 = put(el6, "a", naam);
		
		put(main, el1);
	});
});