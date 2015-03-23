require ([
	'dojo/request/xhr',
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
], function (xhr, dom, on, win, query, domattr, domConstruct, OrderedList, put, Pager, Select) {
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
	
	on(win.doc, ".delete-el:click", function(event) {
		var idItem = domattr.get(this, 'value');
		var itemToDel = query(".delete-el[value$=" + idItem + "]").closest(".list-group-item[value$=" + idItem + "]")[0];
		
		domConstruct.destroy(itemToDel);
	});
	
	new Select ('#layers-select', {
		onSelect: function (item) {
			xhr (jsRoutes.controllers.Layers.structureItem (item.id).url)
				.then (function (data) {
					domConstruct.place(data, main);				
				});
		}
	});	
	new Pager ('#layers-select .js-dropdown');
	
	new Select ('#groups-select', {
		onSelect: function (item) {
			xhr (jsRoutes.controllers.Groups.structureItem (item.id).url)
				.then (function (data) {
					domConstruct.place(data, main);				
				});
		}
	});
	new Pager ('#groups-select .js-dropdown');	

});

$(function () {
	$('[data-toggle="popover"]').popover();
});