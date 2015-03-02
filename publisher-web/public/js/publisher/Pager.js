define ([
	'dojo/_base/lang',
	'dojo/_base/array',
	
	'dojo/query',
	'dojo/on',
	'dojo/dom-attr',
	'dojo/dom-construct',
	'dojo/request/xhr',
	
	'put-selector/put'
], function (
	lang,
	array,
	
	query,
	on,
	domAttr,
	domConstruct,
	xhr,
	
	put
) {
	
	function Pager (nodeOrSelector) {
		this.node = query (nodeOrSelector)[0];
		this._handlers = [ ];
		this._currentValues = { };
		
		var self = this;
		
		on (this.node, '.pagination a[href]:click', function (e) {
			e.preventDefault ();
			e.stopPropagation ();
			
			var url = domAttr.get (this, 'href');
			if (!url.contains ('#')) {
				self._doPage (url);
			}
		});
		
		this._bind ();
	}
	
	lang.mixin (Pager.prototype, {
		_doPage: function (url) {
			this._update (url);
		},
		
		_scheduleUpdate: function (url) {
			if (this._scheduleTimeout) {
				clearTimeout (this._scheduleTimeout);
			}
			
			this._scheduleTimeout = setTimeout (lang.hitch (this, function () {
				this._update (url);
			}), 100);
		},
		
		_update: function (url) {
			var parameters = '';
			
			// Query pager parameters:
			query ('select[data-pager-parameter],input[data-pager-parameter]', this.node).forEach (function (node) {
				var name = domAttr.get (node, 'data-pager-parameter'),
					value = node.value;

				if (lang.trim (value) != '') {
					if (parameters.length == 0 && !url.contains ('?')) {
						parameters += '?';
					} else {
						parameters += '&';
					}
					
					parameters += name + '=';
					parameters += encodeURIComponent (value);
				}
			});
			
			xhr.get (url + parameters, {
				handleAs: 'json'
			}).then (lang.hitch (this, function (data) {
				domConstruct.empty (this.node);
				this.node.innerHTML = data.htmlContent;
				this._bind ();
			}));
		},
		
		_bind: function () {
			array.forEach (this._handlers, function (handler) {
				handler.remove ();
			});
			
			this._handlers = [ ];
			this._currentValues = { };
			
			query ('select[data-pager-parameter],input[data-pager-parameter]', this.node).forEach (lang.hitch (this, function (node) {
				var name = domAttr.get (node, 'data-pager-parameter');
				
				this._handlers.push (on (node, 'change,keydown,keyup', lang.hitch (this, function (e) {
					var currentValue = lang.trim (node.value);
					
					if (!(name in this._currentValues) || this._currentValues[name] !== currentValue) {
						this._changeValue (name, lang.trim (node.value));
						this._currentValues[name] = currentValue;
					}
				})));
				
				this._currentValues[name] = lang.trim (node.value || '');
			}));
		},
		
		_changeValue: function (name, value) {
			query ('*[data-pager-url]', this.node).forEach (lang.hitch (this, function (node) {
				this._scheduleUpdate (domAttr.get (node, 'data-pager-url'));
			}));
		}
	});
	
	return Pager;
});