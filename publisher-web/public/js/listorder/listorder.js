define ([
    'dojo/_base/array',
    
    'dojo/topic',
    
	'dojo/query',
	'dojo/dom',
	'dojo/dom-geometry',
	'dojo/dom-style',
	'dojo/dom-class',
	'dojo/dom-attr',
	
	'dojo/on',

	'dojo/json',
	
	'dojo/request/xhr',
	
	'put-selector/put',
	
	(window.navigator ? 'dojo/domReady!' : 'dojo/domReady')
], function (
	array,
	
	topic,
	
	query,
	dom,
	domGeom,
	domStyle,
	domClass,
	domAttr,
	
	on,
	
	json,
	
	xhr,
	
	put
) {
	
	var body = query ('body')[0];
	
	function OrderedList (element) {
		this.init (element);
	}

	OrderedList.prototype.init = function (element) {
		element = query (element)[0];
		
		// =========================================================================
		this.orderedList = element;
		
		var orderedListItems = query ('> .list-group-item', this.orderedList),
			self = this;
	
		// Change list order:
		// =========================================================================
		
				
		on (this.orderedList, '.gp-draggable:mousedown', function (e) {
			var dragNode = this,
				placeholder = null,
				handles = [ ],
				startPosition,
				startMousePosition,
				height,
				totalHeight,
				items,
				currentOverNode = null,
				currentRel = null;
			
			e.preventDefault();
			e.stopPropagation();
			
			function updateItemPositions () {
				// Store the position of all other items:
				items = [ ];
				totalHeight = 0;
				array.forEach (orderedListItems, function (i) {
					var pos = domGeom.getMarginBox (i);
					
					totalHeight += pos.h;
					
					if (i == dragNode) {
						return;
					}
					
					items.push ({
						node: i,
						top: i.offsetTop,
						height: pos.h
					});
				});
			}
			
			function startDrag (position) {
				// Update HTML DOM-tree
				orderedListItems = query ('> .list-group-item', self.orderedList);
				var order = array.map (orderedListItems , function (listItem) { return domAttr.get (listItem, 'data-layer-id'); });
				
				
				// Store the start mouse position:
				startMousePosition = position;
				
				// Get the relative position and dimensions of the dragging item:
				var position = domGeom.getMarginBox (dragNode);
				startPosition = dragNode.offsetTop;
				height = position.h;
				
				// Create a placeholder and give it the correct height:
				placeholder = put (dragNode, '- div.list-group-item.gp-placeholder');
				domStyle.set (placeholder, 'height', position.h + 'px');
				domStyle.set (placeholder, 'backgroundColor', 'gray');
				
				// Give the drag node absolute positioning:
				domStyle.set (dragNode, {
					position: 'absolute',
					width: position.w + 'px',
					height: position.h + 'px',
					left: position.l + 'px',
					top: position.t + 'px',
					zIndex: '1',
					transform: 'rotate(0.5deg)'
				});
				
				updateItemPositions ();
			}
	
			function stopDrag () {
				// Remove event handlers:
				array.forEach (handles, function (h) { h.remove (); });
				
				if (!placeholder) {
					return;
				}
				
				// Place the drag node:
				put (placeholder, '+', dragNode);
				put (placeholder, '!');
				
				// Reset the style on the drag node:
				domStyle.set (dragNode, {
					position: '',
					width: '',
					height: '',
					left: '',
					top: '',
					zIndex: '',
					transform: ''
				});
				
				// Get the new list order:
				orderedListItems = query ('> .list-group-item', self.orderedList);
				var order = array.map (orderedListItems , function (listItem) { return domAttr.get (listItem, 'data-layer-id'); });
				
				//topic.publish ('planoview/theme-layer-order', order);
			}
			
			function updatePlaceholder (node, rel) {
				if (currentOverNode == node && currentRel == rel) {
					return;
				}
				
				if (rel == 'before') {
					put (node, '-', placeholder);
				} else {
					put (node, '+', placeholder);
				}
				
				currentOverNode = node;
				currentRel = rel;
				
				updateItemPositions ();
			}
			
			function onMove (e) {
				var position = e.clientY;
				
				if (!placeholder) {
					startDrag (position);
				}
				
				// Update the position of the drag node:
				var delta = position - startMousePosition,
					top = startPosition + delta;
				
				if (top < -height) {
					top = -height;
				}
				if (top > totalHeight) {
					top = totalHeight;
				}
				
				domStyle.set (dragNode, {
					top: top + 'px'
				});
				
				// Locate the item the mouse is currently over:
				var pos = top + (height / 2),
					overNode,
					rel;
				for (var i = 0; i < items.length; ++ i) {
					if (pos >= items[i].top && pos < items[i].top + items[i].height) {
						rel = ((pos - items[i].top) < (items[i].height / 2) ? 'before' : 'after');
						overNode = items[i].node;
						break;
					}
				}
				
				if (overNode) {
					updatePlaceholder (overNode, rel);
				}
				
				e.preventDefault ();
			}
	
			handles.push (on (body, 'mousemove', onMove));
			handles.push (on (body, 'mouseup', stopDrag));
		});
	};

	return OrderedList;
});