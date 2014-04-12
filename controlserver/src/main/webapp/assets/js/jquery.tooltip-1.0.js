;(function($) {
	jQuery.fn.tooltip = function(arg) {
		var options = $.extend(
			{},
			$.fn.tooltip.defaults,
			arg
		);


		return this.each(function() {
			// save title and remove from element
			var theTitle = $(this).attr("title");
			$(this).attr("title", "");

			$(this).hover(function(e) {
				// remove tooltip if already active
				if($('#tooltip').length > 0) {
					$('#tooltip').stop().remove();
				}

				// add tooltip window
				$('<div id="tooltip"></div>')
					.html(theTitle)
					.append(options.appendHTML)
					.css({display: "none", position: "absolute", 'z-index': "10000", top: e.pageY + options.offsetToPointer, left: e.pageX + options.offsetToPointer}).appendTo('body')
					.fadeIn(options.fadeInTime);
			}, function() {
				$('#tooltip')
					.fadeOut(options.fadeOutTime,
						function(e) {
							$(this).remove();
						}
					);
			});

			// sticky tooltips do not move
			if(!options.sticky) {
				$(this).mousemove(function(e) {
					$('#tooltip')
						.css({top: e.pageY + options.offsetToPointer, left: e.pageX + options.offsetToPointer});
				});
			}

		});
	};

	$.fn.tooltip.defaults ={
		sticky: false, // if sticky tooltip appears and does not move with the pointer
		offsetToPointer: 15, // offset of the tooltip window to the mouse pointer
		fadeInTime: 1000, // time in milliseconds for fade in
		fadeOutTime: 250, // time in milliseconds for fade out
		appendHTML: '<span id="tooltipInner"></span>' // some html stuff to append to the tooltip window
	};
})(jQuery);




