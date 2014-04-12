$(document).ready(function() {
  // WORKAROUND: content requires the total space if sidebar is hidden
  if (!$("#sidebar").length) {
    $("#content").removeClass();
  }

  // add simple click handler for elements which are not implemented yet
  $(".not_implemented")
    .attr("title", "Not implemented yet")
    .click(function(e) {
      e.preventDefault();
      alert("Function not implemented yet.");
    });

  // add close-on-click functionality to feedbackPanel (if visible)
  if ($(".feedbackPanel").length) {
    var feedbackPanel = $(".feedbackPanel");
    var fadeOutDelay = 200;
    var autoFadeOutDelay = 10000;

    // automatically hide feedback panel after some seconds
    var feedbackPanelTimeout = setTimeout(function() {
      feedbackPanel.fadeOut(fadeOutDelay);
    }, autoFadeOutDelay);

    // alternatively hide on click
    feedbackPanel.click(function(e) {
      $(this).fadeOut(fadeOutDelay);
      // clear timer, because it is not needed anymore
      clearTimeout(feedbackPanelTimeout);
    });
  }

  // apply tooltip plugin to any element with title attribute
  $("*[title]").tooltip({fadeInTime: 300, sticky: false});

  // save status (visible/invisible) and animate just once!
  var fixed_navigation_visible = false;

  // copy main navigation
  var fixed_navigation = $(".main_navigation").html();

  // include as child of body and add another class
  $("body").append($('<nav class="main_navigation fixed_navigation"></nav>').html(fixed_navigation));

  // handle behaviour when scrolling
  var fixed_navigation_distance = $(".fixed_navigation").css("top");
  $(window).scroll(function() {
    if ($(this).scrollTop() > 180) {
      if(!fixed_navigation_visible) {
        $(".fixed_navigation").stop().animate({top:0, opacity: 1}, 300);
      }
      fixed_navigation_visible = true;
    } else {
      if(fixed_navigation_visible) {
        $(".fixed_navigation").stop().animate({top:fixed_navigation_distance, opacity: 0}, 300);
        fixed_navigation_visible = false;
      }
    }
  });

  var getStateClass = function(state) {
    var cl = "state_new";
    state = state.toLowerCase();
    if (state == "failed" || state == "success") {
    cl = "state_" + state;
    }
    return(cl);
  };

  // highlight experiment status in experiment overview
  $(".state").each(function(index, value) {
    var state = $(this).html().trim();
    $(this).html(state.toLowerCase());
    var cl = getStateClass(state);
    $(this).addClass(cl);
  });

var generateFilterButtons = function(states, parent_container, list_container, filter_var_container) {
    $.each(states, function(index, value) {
      // build link
      var filter_link = $('<a class="btn btn_small">' + value + '</a>');
      // add handler
      filter_link.click(function(e) {
        if ($(this).hasClass("active")) {
          $(list_container).children().show();
          $(this).removeClass("active");
        } else {
          filterListViewByState(list_container, filter_var_container, value);
          $(parent_container + " a").removeClass("active");
          $(this).addClass("active");
        }
        e.preventDefault();
      });
      // add link to view
      $(parent_container).append(filter_link);
    });
  };


  // For each possible state of an experiment, set up a button
  // which forces the list of experiments to show only the experiments with the corresponding state
  if ($("#list_of_experiments").length > 0) {
    generateFilterButtons(
      ["success", "failed", "active", "cancelling", "cancelled", "new", "ready"],
      "#list_view_filter_experiments",
      "#list_of_experiments",
      ".list_item_state");
  }

  if ($("#list_of_workers").length > 0) {
    generateFilterButtons(
      ["unemployed", "employed", "unavailable"],
      "#list_view_filter_workers",
      "#list_of_workers",
      ".list_item_state");
  }

  if ($("#list_of_jobs").length > 0) {
	generateFilterButtons(
      ["new", "failed", "success"],
      "#list_view_filter_jobs",
      "#list_of_jobs",
      ".list_item_state");
  }


  $("#input_search_by_name").focus(function() {
  	$(this).val("");
  	$(".list_view").children().show();
  }).keypress(function() {
    var needle = $(this).val();
    $(".list_view").children().hide();
    $(".list_view h4:contains('" + needle + "')").parent().parent().parent().show();
  });

  // Helper function which hides all children of 'container' which
  // contains a subcontainer 'state_container' and the value inside this one
  // is unequal to 'filter_state'.
  var filterListViewByState = function(container, state_container, filter_state) {
    $(container).children().css("display", "block").each(function() {
      var state = $(state_container, $(this));
      if (state.html().toLowerCase() != filter_state) {
        $(this).hide();
      }
    });
  }

});
