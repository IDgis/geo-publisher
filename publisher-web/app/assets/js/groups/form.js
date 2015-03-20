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
	domattr, 
	domConstruct, 
	OrderedList,
	
	put,

	Pager,
	Select
) {
	var main = dom.byId('groupLayerStructure');
	
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
		var idItem = domattr.get(this, 'value');
		var itemToDel = query(".delete-el[value$=" + idItem + "]").closest(".list-group-item[value$=" + idItem + "]")[0];
		
		domConstruct.destroy(itemToDel);
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
			xhr ('/layers/' + item.id + '/structure-item')
				.then (function (data) {
					domConstruct.place(data, main);				
				});
		}
	});	
	new Pager ('#layers-select .js-dropdown');
	
	new Select ('#groups-select', {
		onSelect: function (item) {
			xhr ('/groups/' + item.id + '/structure-item')
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