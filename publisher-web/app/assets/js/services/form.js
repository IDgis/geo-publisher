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
], function (xhr, dom, on, win, query, domAttr, domConstruct, OrderedList, put, Pager, Select) {
	/*
	 * Add and remove keywords
	 */
	var keywordbutton = dom.byId('add-keyword');
	var keywordlist = dom.byId('keyword-list');
	
	var userGroupSelect = dom.byId('input-usergroup-select');
	var userGroupList = dom.byId('usergroup-list');
	
	on(keywordbutton,'click', function(evt) {
		var keywordinput = dom.byId('input-keyword').value;
		
		if(keywordinput !== "") {
			var container = put("div.keyword-item-block[value=$]", keywordinput);
			put(container, "input[type=hidden][name=$][value=$]", "keywords[]", keywordinput);
			var label = put(container, "div.keyword-item.label.label-primary", keywordinput);
			var closeButton = put(label, "button.close[type=button][aria-hidden=true][value=$]", keywordinput);
			closeButton.innerHTML = "&times;";
			
			put(keywordlist, container);
		}
		
		dom.byId('input-keyword').value = "";
	});
	
	on(userGroupSelect,'change', function(e) {
		var userGroupValue = userGroupSelect.value;
		
		if(userGroupValue !== "") {
			var container = put("div.usergroup-item-block[value=$]", userGroupValue);
			put(container, "input[type=hidden][name=$][value=$]", "userGroups[]", userGroupValue);
			var label = put(container, "div.usergroup-item.label.label-primary", userGroupValue);
			var closeButton = put(label, "button.close[type=button][aria-hidden=true][value=$]", userGroupValue);
			closeButton.innerHTML = "&times;";
			
			put(userGroupList, container);
		}
		
		userGroupSelect.value = "";
	});
	
	on(win.doc, ".close:click", function(event) {
		var keywordToDel = query(this).parents(".keyword-item-block")[0];
		var userGroupToDel = query(this).parents(".usergroup-item-block")[0];
		
		if(keywordToDel) domConstruct.destroy(keywordToDel);
		if(userGroupToDel) domConstruct.destroy(userGroupToDel);
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
		var idItem = domAttr.get(this, 'value');
		var itemToDel = query(".delete-el[value$=" + idItem + "]").closest(".list-group-item[value$=" + idItem + "]")[0];
		
		domConstruct.destroy(itemToDel);
	});
	
	new Select ('#layers-select', {
		onSelect: function (item) {
			xhr (jsRoutes.controllers.Layers.structureItem (item.id, false).url)
				.then (function (data) {
					domConstruct.place(data, main);
				});
		}
	});	
	new Pager ('#layers-select .js-dropdown');
	
	new Select ('#groups-select', {
		onSelect: function (item) {
			xhr (jsRoutes.controllers.Groups.structureItem (item.id, false).url)
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