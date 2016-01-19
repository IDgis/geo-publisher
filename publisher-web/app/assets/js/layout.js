/* jshint -W099 */
require ([
    'dojo/dom',
    'dojo/dom-class',
    'dojo/dom-construct',
    'dojo/dom-attr',
    'dojo/query',
    'dojo/topic',
    'dojo/request/xhr',
    'dojo/on',
    'dojo/hash',
    
    'put-selector/put',
    
	'dojo/domReady!'
], function (
	dom,
	domClass,
	domConstruct,
	domAttr,
	query,
	topic,
	xhr,
	on,
	hash,
	
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
    	'active-tasks': 'activeTasks',
    	'notifications': 'notifications'
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
    
    // Start polling the server for events if the event bar is available in the layout:
    if (dom.byId ('event-bar')) {
    	setTimeout (function () {
    		pollEvents ();
    	});
    }
    
    // =========================================================================
    // Update task list:
    // =========================================================================
    var taskDropdown = dom.byId ('event-dropdown-active-tasks'),
    	notificationsDropdown = dom.byId ('event-dropdown-notifications'),
    	issuesDropdown = dom.byId ('event-dropdown-issues');
    
    function updateEventDropdown (dropdown, data) {
    	var badge = query ('.js-badge', dropdown)[0],
			list = query ('.js-list', dropdown)[0];
		
		// Update the badge:
		domConstruct.empty (badge);
		put (badge, document.createTextNode ((data.hasMore ? '> ' : '') + data.list.length));
		
		domClass[data.list.length === 0 ? 'add' : 'remove'] (badge, 'hidden');
		
		// Update the contents of the list:
		domConstruct.empty (list);
		list.innerHTML = data.headerContent;
    }
    
    topic.subscribe ('publisher/active-tasks', function (activeTasks) {
    	updateEventDropdown (taskDropdown, activeTasks);
    });
    topic.subscribe ('publisher/notifications', function (notifications) {
    	updateEventDropdown (notificationsDropdown, notifications);
    });
    
    // =========================================================================
    // Help document viewer:
    // =========================================================================
    function displayDocument (path) {
    	xhr.get (path, {
    		handleAs: 'html'
    	}).then (function (data) {
    		query ('#doc-modal .modal-body')[0].innerHTML = data;

    		// Display the first heading of the document as the title of the modal:
    		var heading = query ('#doc-modal .modal-body h1')[0];
    		if (heading) {
    			query ('#doc-modal h4')[0].innerHTML = heading.innerHTML;
    			put ('!', heading);
    		}
    		
        	jQuery ('#doc-modal').modal ('show');
    	});
    }
    
    function onClickDocumentLink (e) {
    	e.preventDefault ();
    	e.stopPropagation ();
    	
    	var docPath = domAttr.get (this, 'data-doc-path');
    	
    	hash ('!doc!' + docPath);
    }
   
    function hashChange (value) {
    	if (value.length >= 5 && value.substring (0, 5) == '!doc!') {
    		displayDocument (value.substring (5));
    	} else {
    		jQuery ('#doc-modal').modal ('hide');
    	}
    }
    
    on (dom.byId ('help-doc-link'), 'click', onClickDocumentLink);
    query ('#doc-modal .modal-body').on ('a[data-doc-path]:click', onClickDocumentLink);
    topic.subscribe ('/dojo/hashchange', hashChange);
    jQuery ('#doc-modal').on ('hidden.bs.modal', function (e) {
    	var hashValue = hash ();
    	if (hashValue && hashValue.length >= 5 && hashValue.substring (0, 5) == '!doc!') {
    		hash ('');
    	}
    });
    
    setTimeout (function () {
    	hashChange (hash ());
    }, 0);
});