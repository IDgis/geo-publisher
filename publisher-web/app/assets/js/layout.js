/* jshint -W099 */
require ([
    'dojo/dom',
    'dojo/dom-class',
    'dojo/query',
    'dojo/topic',
    
    'put-selector/put',
    
	'dojo/domReady!'
], function (
	dom,
	domClass,
	query,
	topic,
	
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
});