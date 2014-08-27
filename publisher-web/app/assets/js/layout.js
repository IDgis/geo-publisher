/* jshint -W099 */
require ([
    'dojo/dom',
    'dojo/dom-class',
    'dojo/dom-construct',
    'dojo/query',
    'dojo/topic',
    'dojo/request/xhr',
    
    'put-selector/put',
    
	'dojo/domReady!'
], function (
	dom,
	domClass,
	domConstruct,
	query,
	topic,
	xhr,
	
	put
) {
	
	
    $(window).bind("load resize", function() {
        width = (this.window.innerWidth > 0) ? this.window.innerWidth : this.screen.width;
        if (width < 768) {
            $('div.sidebar-collapse').addClass('collapse');
        } else {
            $('div.sidebar-collapse').removeClass('collapse');
        }
    });
   
    var pageWrapper = dom.byId ('page-wrapper'),
    	notificationContainer = dom.byId ('notification-container');
    
    // =========================================================================
    // Flash notifications:
    // =========================================================================
    var clearNotificationsTimeout = null;
    
    function clearNotifications (force) {
    	query ('.alert', notificationContainer).forEach (function (node) {
    		if (!force) {
    			domClass.remove (node, 'in');
    			setTimeout (function () { put (node, '!'); }, 500);
    		} else {
    			put (node, '!');
    		}
    	});
    }
    
    topic.subscribe ('publisher/notification', function (notificationType, message) {
    	clearNotifications (true);
    	
    	var closeButton = put ('button.close[type="button"][data-dismiss="alert"]', { innerHTML: '&times;' }),
    		notificationNode = put (notificationContainer, 'div.fade.alert.alert-' + notificationType);
    	
    	put (notificationNode, '>', closeButton);
    	put (notificationNode, '> span $', message);
    	
    	setTimeout (function () {
    		domClass.add (notificationNode, 'in');
    	}, 0);
    	
    	if (clearNotificationsTimeout) {
    		clearTimeout (clearNotificationsTimeout);
    	}
    	
    	clearNotificationsTimeout = setTimeout (function () {
    		clearNotifications ();
    		clearNotificationsTimeout = null;
    	}, 15000);
    });
    
    // =========================================================================
    // Events:
    // =========================================================================
    var eventTypes = {
    	'active-tasks': 'activeTasks'	
    };
    
    function processEvents (data) {
    	for (var i in eventTypes) {
    		if (eventTypes[i] in data) {
    			topic.publish ('publisher/' + i, data[eventTypes[i]]);
    		}
    	}
    }
    
    function pollEvents (eventTag) {
    	var url = eventTag ? jsRoutes.controllers.Events.eventsWithTag (eventTag).url
    		: jsRoutes.controllers.Events.events ().url;

    	xhr.get (url, {
    		handleAs: 'json'
    	}).then (function (data) {
    		var tag = data.tag;
    		
    		if (!tag) {
    			// Stop polling when the server didn't return a tag:
    			return;
    		}
   
    		// Process the event data:
    		processEvents (data);
    		
    		// Start a new polling operation in the future:
    		setTimeout (function () {
    			pollEvents (tag);
    		}, 0);
    	});
    }
    
    // Start polling the server for events:
    setTimeout (function () {
    	pollEvents ();
    });
    
    // =========================================================================
    // Update task list:
    // =========================================================================
    var taskDropdown = dom.byId ('event-dropdown-active-tasks');
    
    topic.subscribe ('publisher/active-tasks', function (activeTasks) {
    	var badge = query ('.js-badge', taskDropdown)[0],
    		list = query ('.js-list', taskDropdown)[0];
    	
    	console.log ('Active tasks: ', activeTasks);
    	
    	// Update the badge:
    	domConstruct.empty (badge);
    	put (badge, document.createTextNode (activeTasks.list.length));
    	
    	domClass[activeTasks.list.length === 0 ? 'add' : 'remove'] (badge, 'hidden');
    	
    	// Update the contents of the list:
    	domConstruct.empty (list);
    	list.innerHTML = activeTasks.headerContent;
    });
});