require ([
	'dojo/request/xhr',
	'dojo/dom',
	'dojo/on',
	'dojo/dom-style',
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
], function (
	xhr,
	dom, 
	on, 
	domStyle, 
	win, 
	query, 
	domAttr, 
	domConstruct, 
	OrderedList,
	
	put,

	Pager,
	Select
) {
	var main = dom.byId('groupLayerStructure');
	
	var userGroupSelect = dom.byId('input-usergroup-select');
	var userGroupList = dom.byId('usergroup-list');
	
	if(dom.byId ('groupLayerStructure')) {
		var list = new OrderedList ('#groupLayerStructure');
	}
	
	var inputEnable = dom.byId('input-enable');
	var jsTiledForm = dom.byId('js-tiled-form');
	if (inputEnable.checked) {
		domStyle.set(jsTiledForm, "display", "block");
	} else {
		domStyle.set(jsTiledForm, "display", "none");
	}
	
	on(win.doc, ".delete-el:click", function(event) {
		var idItem = domAttr.get(this, 'value');
		var itemToDel = query(".delete-el[value$=" + idItem + "]").closest(".list-group-item[value$=" + idItem + "]")[0];
		
		domConstruct.destroy(itemToDel);
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
		var userGroupToDel = query(this).parents(".usergroup-item-block")[0];
		if(userGroupToDel) domConstruct.destroy(userGroupToDel);
	});
	
	on(inputEnable, 'click', function(evt) {
		if (inputEnable.checked) {
			domStyle.set(jsTiledForm, "display", "block");
		} else {
			domStyle.set(jsTiledForm, "display", "none");
		}
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