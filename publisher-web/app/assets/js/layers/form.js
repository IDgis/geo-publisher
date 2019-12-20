require ([
	'dojo/dom',
	'dojo/on',
	'dojo/dom-style',
	'dojo/_base/window',
	'dojo/query',
	'dojo/dom-attr',
	'dojo/dom-construct',
	'tree-select/tree-select',
	'publisher/Pager',
	'put-selector/put',
	
	'dojo/NodeList-traverse',
	'dojo/domReady!'
], function (dom, on, domStyle, win, query, domAttr, domConstruct, TreeSelect, Pager, put) {
	var treeSelect = new TreeSelect ('.gp-tree-select', '.gp-tree-values', { firstIsDefault: true }),
		pager = new Pager ('.gp-tree-values .js-pager');
	var keywordbutton = dom.byId('add-keyword');
	var keywordlist = dom.byId('keyword-list');
	
	var userGroupSelect = dom.byId('input-usergroup-select');
	var userGroupList = dom.byId('usergroup-list');
	
	var inputEnable = dom.byId('input-enable');
	var jsTiledForm = dom.byId('js-tiled-form');
	if (inputEnable.checked) {
		domStyle.set(jsTiledForm, "display", "block");
	} else {
		domStyle.set(jsTiledForm, "display", "none");
	}
	
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
	
	on(inputEnable, 'click', function(evt) {
		if (inputEnable.checked) {
			domStyle.set(jsTiledForm, "display", "block");
		} else {
			domStyle.set(jsTiledForm, "display", "none");
		}
	});
});

$(function () {
	$('[data-toggle="popover"]').popover();
});
