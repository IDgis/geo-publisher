define ([
	'dojo/_base/lang',
	'dojo/_base/array',

	'dojo/dom-attr',
	'dojo/dom-class',
	
	'dojo/query',
	
	'dojo/on',
	
	'dojo/json',
	
	'put-selector/put'
], function (
	lang,
	array,
	
	domAttr,
	domClass,
	
	query,
	
	on,
	
	json,
	
	put
) {
	
	function TreeSelect (node, treeNode, options) {
		this.containerNode = query (node)[0];
		this.treeNode = query (treeNode)[0];
		this.inputNode = query ('input[type="hidden"], input[type="text"]', this.containerNode)[0];
		this.addButton = null;
		
		this.values = [ ];

		if (options && options.onChange) {
			this.onChange = options.onChange;
		}
		
		// Set the current control value:
		this.syncTree ();
		this.syncValue ();
		
		var self = this;
		this.handles = [
		    on (this.containerNode, '.close:click', lang.hitch (this, this._onClickRemove)),
		    on (this.containerNode, '.add-term:click', lang.hitch (this, this._onClickAdd))
		];
	}
	
	lang.mixin (TreeSelect.prototype, {

		/**
		 * Reads the value of the input and updates the list of taxonomy terms accordingly. 
		 */
		syncValue: function () {
			var stringValue = lang.trim (this.inputNode.value);
			if (stringValue == '') {
				stringValue = '[]';
			}
			
			var values = json.parse (stringValue);
			
			this.values = values;
			
			query ('.term, .add-term', this.containerNode).forEach (function (n) { put (n, '!'); })
			
			this.addButton = put (this.containerNode, 'a.label.label-default.add-term[href="#"] span.glyphicon.glyphicon-plus <');
			
			array.forEach (values, function (value) {
				var label = value[0],
					id = value[1];
				
				this.addTerm (id, label, true);
			}, this);
			
		},
		
		/**
		 * Syncs the tree containging the selectable values.
		 */
		syncTree: function () {
		},
		
		_onClickExpander: function (node) {
			var itemNode = node.parentNode,
				iconNode = query ('.glyphicon', node)[0];
			
			if (domClass.contains (itemNode, 'expanded')) {
				domClass.remove (itemNode, 'expanded');
				domClass.add (iconNode, 'glyphicon-plus');
				domClass.remove (iconNode, 'glyphicon-minus');
			} else {
				domClass.add (itemNode, 'expanded');
				domClass.remove (iconNode, 'glyphicon-plus');
				domClass.add (iconNode, 'glyphicon-minus');
			}
		},
		
		_onClickTreeNode: function (node) {
			var id = domAttr.get (node, 'data-id'),
				label = domAttr.get (node, 'data-label');
			
			this.addTerm (id, label);
		},
		
		_onClickRemove: function (e) {
			var termNode = e.target.parentNode,
				id = domAttr.get (termNode, 'data-id');
			
			e.preventDefault ();
			e.stopPropagation ();
			
			this.removeTerm (id);
		},
		
		_onClickAdd: function (e) {
			e.preventDefault ();
			e.stopPropagation ();
			
			this._openTree ();
		},
		
		_openTree: function () {
			put (this.containerNode, '+', this.treeNode);
			if (domClass.contains (this.treeNode, 'open')) {
				return;
			}
			
			domClass.add (this.treeNode, 'open');

			var self = this;
			this._treeHandles = [
			    on (query ('html')[0], 'click', function (e) {
					var node = e.target;
					
					while (node) {
						if (node == self.treeNode) {
							return;
						}
						
						node = node.parentNode;
					}
					
					self._closeTree ();
				}),
			    on (this.treeNode, '.expander:click', function (e) {
			    	e.preventDefault ();
			    	e.stopPropagation ();
			    	
			    	self._onClickExpander (this);
			    }),
			    on (this.treeNode, 'a[data-id]:click', function (e) {
			    	e.preventDefault ();
			    	e.stopPropagation ();
			    	
			    	self._onClickTreeNode (this);
			    })
			];
		},
		
		_closeTree: function () {
			if (!domClass.contains (this.treeNode, 'open')) {
				return;
			}
			
			domClass.remove (this.treeNode, 'open');
			if (this._treeHandles) {
				array.forEach (this._treeHandles, function (h) { h.remove (); });
				this._closeTreeHandle = null;
			}
		},
		
		removeTerm: function (id) {
			for (var i = 0; i < this.values.length; ++ i) {
				if (this.values[i][1] == id) {
					this.values.splice (i, 1);
				}
			}
			
			query ('.term[data-id="' + id + '"]', this.containerNode).forEach (function (n) { put (n, '!'); });
			
			this._updateInput ();
		},
		
		addTerm: function (id, label, skipUpdate) {
			if (!skipUpdate) {
				for (var i = 0; i < this.values.length; ++ i) {
					if (this.values[i][1] == id) {
						return;
					}
				}
			}
	
			var labelType = 'label-primary';
			
			put (this.addButton, '- span.term.label.' + labelType + '[data-id=$] $ button[type="button"][aria-hidden="true"].close', id, label, { innerHTML: '&times;' });
			
			if (!skipUpdate) {
				this.values.push ([label, id]);
				this._updateInput ();
			}
		},
		
		_updateInput: function () {
			var stringValue= json.stringify (this.values);
			this.inputNode.value = stringValue;
			this.onChange (this, stringValue);
		},
		
		onChange: function (treeSelect, value) { }
	});
	
	return TreeSelect;
});