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
	
	on(dom.byId('addgroup'),'change', function(evt) {
		var selectIndex = evt.currentTarget.options.selectedIndex;
		
		var id = evt.currentTarget[selectIndex].value;
		var name = evt.currentTarget[selectIndex].innerHTML;
		var structureName="structure[]";
		
		if(name !== "") {
			var el1 = put("div.list-group-item.js-gp-draggable.tree-item[value=$]", id);
			var el2 = put(el1, "input[type=hidden][name=$][value=$]", structureName, id);
			var el3 = put(el1, "div.row");
			var el4 = put(el3, "div.col-sm-11.groupTree");
			var el5 = put(el4, "ul.treelist");
			var el6 = put(el5, "li");
			var el7 = put(el6, "a", name);
			var el8 = put(el3, "div.col-sm-1");
			var el9 = put(el8, "div.pull-right.tree-item-delete");
			var el10 = put(el9, "a.btn.btn-warning.btn-sm.delete-el[value=$]", id);
			var el11 = put(el10, "span.glyphicon.glyphicon-remove");
			
			put(main, el1);
		}
	});
	
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
			xhr ('/groups/structure-item/' + item.id)
				.then (function (data) {
					domConstruct.place(data, main);				
				});
		}
	});
	new Pager ('#layers-select .js-dropdown');
	
});

$(function () {
	$('[data-toggle="popover"]').popover();
});