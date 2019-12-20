require ([
	'dojo/dom',
	'dojo/on',
	'dojo/_base/window',
	'dojo/query',
	'dojo/dom-attr',
	'dojo/dom-construct',
	'dojo/_base/array',
	'put-selector/put',
	
	'dojo/NodeList-traverse',
	'dojo/domReady!'
], function (dom, on, win, query, domAttr, domConstruct, array, put) {
	var userSelect = dom.byId('input-user-select');
	var userList = dom.byId('user-list');
	
	var inputUsers = dom.byId('input-users');
	var inputUsersAtLoad = JSON.parse(inputUsers.value);
	
	array.forEach(inputUsersAtLoad,function(user) {
		putUser(user, inputUsersAtLoad.length);
	});
	
	on(userSelect, 'change', function(e) {
		var inputUsers = dom.byId('input-users');
		var userValue = userSelect.value;
		
		var array = JSON.parse(inputUsers.value);
		
		putUser(userValue, array.length);
		
		array.push(userValue);
		userSelect.value = "";
		inputUsers.value = JSON.stringify(array);
	});
	
	on(win.doc, ".close:click", function(event) {
		var itemToDel = query(this).parents(".user-item-block")[0];
		domConstruct.destroy(itemToDel);
		
		var userItems = query('.user-item');
		var newInputUsers = [];
		array.forEach(userItems,function(userItem) {
			newInputUsers.push(domAttr.get(userItem, 'value'));
		});
		
		dom.byId('input-users').value = JSON.stringify(newInputUsers);
	});
	
	function putUser(userValue, arrayLength) {
		if(userValue !== "") {
			var container = put("div.user-item-block");
			var labelInput = put(container, "div.user-item.label.label-primary[value=$]", userValue, userValue);
			var closeButton = put(labelInput, "button.close[type=button][aria-hidden=true][data-index=$]", arrayLength);
			closeButton.innerHTML = "&times;";
			
			put(userList, container);
		}
	}
});

$(function () {
	$('[data-toggle="popover"]').popover();
});
