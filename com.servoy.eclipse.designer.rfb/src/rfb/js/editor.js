function handleJsIds()
{
	handleModalIds();
	handleAccordionIds();
	handleCarouselIds();
	handleTabsIds()
}

function handleAccordionIds()
{
	var e=$("#canvas #myAccordion");
	var t=randomNumber();
	var n="panel-"+t;
	var r;e.attr("id",n);e.find(".panel").each(function(e,t)
	{
		r="panel-element-"+randomNumber();
		$(t).find(".panel-title").each(function(e,t)
		{
			$(t).attr("data-parent","#"+n);
			$(t).attr("href","#"+r)
		});
		$(t).find(".panel-collapse").each(function(e,t)
		{
			$(t).attr("id",r)
		});
	});
}

function handleCarouselIds()
{
	var e=$("#canvas #myCarousel");
	var t=randomNumber();
	var n="carousel-"+t;e.attr("id",n);
	e.find(".carousel-indicators li").each(function(e,t)
	{
		$(t).attr("data-target","#"+n)
	});
	e.find(".left").attr("href","#"+n);
	e.find(".right").attr("href","#"+n)
}

function handleModalIds()
{
	var e=$("#canvas #myModalLink");
	var t=randomNumber();
	var n="modal-container-"+t;
	var r="modal-"+t;
	e.attr("id",r);
	e.attr("href","#"+n);
	e.next().attr("id",n)
}

function handleTabsIds()
{
	var e=$("#canvas #myTabs");
	var t=randomNumber();
	var n="tabs-"+t;e.attr("id",n);
	e.find(".tab-pane").each(function(e,t)
	{
		var n=$(t).attr("id");var r="panel-"+randomNumber();$(t).attr("id",r);$(t).parent().parent().find("a[href=#"+n+"]").attr("href","#"+r)
	});
}

function randomNumber()
{
	return randomFromInterval(1,1e6)
}

function randomFromInterval(e,t)
{
	return Math.floor(Math.random()*(t-e+1)+e)
}

function updateGridColumns(elemNode)
{
	var e=0;
	var t="";
	var n=false;
	var r=elemNode.val().split(",",12);
	$.each(r,function(r,i)
	{
		if (!n)
		{
			if(parseInt(i)<=0)n=true;
			e=e+parseInt(i);
			t+='<div class="col-md-'+i+' column"></div>'
		}
	});
	
	if (e==12&&!n)
	{
		elemNode.parent().next().children().html(t);
		elemNode.parent().prev().show()
	}
	else
	{
		elemNode.parent().prev().hide()
	}
}

function fillGridSystemGeneratorPalette()
{
	//add grid rows from template
	var gridArray = ["12","6,6","8,4","4,4,4","2,6,4"];
	for (var i in gridArray) 
	{
		var ghtml = $("#gridGenerator").html();
		ghtml = ghtml.replace("xxx",gridArray[i]);
		$("#estRows").append(ghtml);
	}
	
	//expand them
	$(".lyrow .preview input").each(function()
	{
		updateGridColumns($(this));
	});
	
	//bind them for changes
	$(".lyrow .preview input").bind("keyup",function()
	{
		updateGridColumns($(this));
	});

	//make them draggable
	$(".sidebar-nav .lyrow").draggable(
	{
		connectToSortable:"#canvas, #canvas .container",helper:"clone",handle:".drag",
		drag:function(e,t)
		{
			t.helper.width(400)
		},
		stop:function(e,t)
		{
			$("#canvas .column").sortable(
			{
				opacity:.35,connectWith:".column"
			})
			$("#canvas .row").sortable(
			{
				opacity:.35,connectWith:"#canvas"
			})
		}
	});
}

function installConfigurationDelegateAction(e,t)
{
	$("#canvas").delegate(".configuration > a","click",function(e)
	{
		e.preventDefault();var t=$(this).parent().next().next().children();
		$(this).toggleClass("active");t.toggleClass($(this).attr("rel"))
	});
	
	$("#canvas").delegate(".configuration .dropdown-menu a","click",function(e)
	{
		e.preventDefault();
		var t=$(this).parent().parent();
		var n=t.parent().parent().next().next().children();
		t.find("li").removeClass("active");
		$(this).parent().addClass("active");
		var r="";t.find("a").each(function()
		{
			r+=$(this).attr("rel")+" "
		});
		t.parent().removeClass("open");
		n.removeClass(r);
		n.addClass($(this).attr("rel"))
	});
}

function installRemoveDelegateAction()
{
	$("#canvas").delegate(".remove","click",function(e)
	{
		e.preventDefault();
		$(this).parent().remove();
	});
}

function clearDemo()
{
	$("#canvas").empty()
}

function removeMenuClasses()
{
	$("#menu-layoutit li button").removeClass("active")
}

function changeStructure(e,t)
{
	$("#src ."+e).removeClass(e).addClass(t)
}

function cleanHtml(e)
{
	$(e).parent().append($(e).children().html())
}

function setLayoutSrc(src, callback)
{
	$("#canvas").html(src);
	
	$("#canvas").selectable({
		filter: ".element",
		selected: function(event, ui) {
			
			// send selected element ids
			callback.setSelection($("#canvas .ui-selected").map(function(index,dom){return dom.id}).toArray())
		},
		unselected: function(event, ui) {
			
			// send selected element ids
			callback.setSelection($("#canvas .ui-selected").map(function(index,dom){return dom.id}).toArray())
		}
	});
}

function downloadLayoutSrc()
{
	var e="";
	$("#src").html($("#canvas").html());
	var t=$("#src").children();
	t.find(".preview, .configuration, .drag, .remove").remove();
	t.find(".lyrow").addClass("removeClean");t.find(".box-element").addClass("removeClean");
	t.find(".lyrow .lyrow .lyrow .lyrow .lyrow .removeClean").each(function()
	{
		cleanHtml(this)
	});
	t.find(".lyrow .lyrow .lyrow .lyrow .removeClean").each(function()
	{
		cleanHtml(this)
	});
	t.find(".lyrow .lyrow .lyrow .removeClean").each(function()
	{
		cleanHtml(this)
	});
	t.find(".lyrow .lyrow .removeClean").each(function()
	{
		cleanHtml(this)
	});
	t.find(".lyrow .removeClean").each(function()
	{
		cleanHtml(this)
	});
	t.find(".removeClean").each(function()
	{
		cleanHtml(this)
	});
	t.find(".removeClean").remove();
	$("#src .column").removeClass("ui-sortable");
	$("#src .row-fluid").removeClass("clearfix").children().removeClass("column");
	if($("#src .container").length>0)
	{
		changeStructure("row-fluid","row")
	}
	var formatSrc=$.htmlClean($("#src").html(),
	{
		format:true,allowedAttributes:[["id"],["class"],["data-toggle"],["data-target"],["data-parent"],["role"],["data-dismiss"],["aria-labelledby"],["aria-hidden"],["data-slide-to"],["data-slide"]]
	});
	$("#src").html('');
	return formatSrc;
}

function connectPaletteDnD(elems)
{
	if (window.absolute_layout)
	{
		elems.draggable(
		{
			helper:"clone",handle:".drag",
			drag:function(e,ui)
			{
				ui.helper.width(400);
			},
			stop:function(e,ui)
			{
				//move a clone of helper to canvas
				var clone = $(ui.helper).clone(true).removeClass('ui-draggable ui-draggable-dragging');
//				clone.position( { of: $(this), my: 'left top', at: 'left top' } );
				$("#canvas").append(clone);
				
				//make clone draggable/selectable
				connectElementDnD(clone);
			}
		});
	}
	else
	{
		elems.draggable(
		{
			connectToSortable:".column",helper:"clone",handle:".drag",
			drag:function(e,ui)
			{
				ui.helper.width(400);
			},
			stop:function()
			{
				handleJsIds();
			}
		});
	}
}

function fillPalette()
{
	if (!window.absolute_layout)
	{
		fillGridSystemGeneratorPalette();
	}
	
	//dynamically add one category in palette 
	$.get("palette/base.template", function(data){
		$("#elmBase").append(data);
		connectPaletteDnD($(".sidebar-nav .box"));
	});

	//dynamically add one category in palette 
	$.get("palette/components.template", function(data){
		$("#elmComponents").append(data);
		connectPaletteDnD($(".sidebar-nav .box"));
	});

	//dynamically add one category in palette 
	$.get("palette/advanced.template", function(data){
		$("#elmJS").append(data);
		connectPaletteDnD($(".sidebar-nav .box"));
	});
	
	//dynamically add one item to palette category
	$.get("palette/progressbar.template", function(data){
		$("#elmComponents").append(data);
		connectPaletteDnD($(".sidebar-nav .box"));
	});
	
	//add palette slide
	$(".nav-header").click(function()
	{
		$(".sidebar-nav .boxes, .sidebar-nav .rows").hide();
		$(this).next().slideDown()
	});
}

function getURLParameter(sParam)
{
    var sPageURL = window.location.search.substring(1);
    var sURLVariables = sPageURL.split('&');
    for (var i = 0; i < sURLVariables.length; i++) 
    {
        var sParameterName = sURLVariables[i].split('=');
        if (sParameterName[0] == sParam) 
        {
            return sParameterName[1];
        }
    }
}

//this creates the selected variable
//we are going to store the selected objects in here
var selected = $([]), offset = {top:0, left:0}; 
function connectElementDnD(elem)
{
	elem.draggable({
		handle:".drag",
		start: function(ev, ui) {
		    if ($(this).hasClass("ui-selected")){
		        selected = $(".ui-selected").each(function() {
		           var el = $(this);
		           el.data("offset", el.offset());
		        });
		    }
		    else {
		        selected = $([]);
		        elem.removeClass("ui-selected");
		    }
		    offset = $(this).offset();
		},
		drag: function(ev, ui) {
		    var dt = ui.position.top - offset.top, dl = ui.position.left - offset.left;
		    // take all the elements that are selected except $("this"), which is the element being dragged and loop through each.
		    selected.not(this).each(function() {
		         // create the variable for we don't need to keep calling $("this")
		         // el = current element we are on
		         // off = what position was this element at when it was selected, before drag
		         var el = $(this), off = el.data("offset");
		         el.css({top: off.top + dt, left: off.left + dl});
		    });
		}
	});
	
	//manually trigger the "select" of clicked elements
	elem.click( function(e){
		if (e.ctrlKey == false) {
		    // if command key is pressed don't deselect existing elements
		    elem.removeClass("ui-selected");
		    $(this).addClass("ui-selecting");
		}
		else {
		    if ($(this).hasClass("ui-selected")) {
		        // remove selected class from element if already selected
		        $(this).removeClass("ui-selected");
		    }
		    else {
		        // add selecting class if not
		        $(this).addClass("ui-selecting");
		    }
		}
		$( "#canvas" ).data("selectable")._mouseStop(null);
	});
}

function windowResizer()
{
	$("body").css("min-height",$(window).height()-90);
	$("#canvas").css("min-height",$(window).height()-160);
}

$(document).ready(function()
{
	//apply corrections
	$(window).resize(windowResizer);
	windowResizer();

	//enable absolute layout if parameter is present
	if (getURLParameter("absolute_layout") == "true")
	{
		//set global var
		window.absolute_layout = true;
	}
	
	//initial drop positions
	if (window.absolute_layout)
	{
		//set helper class
		$("body").addClass("abs");

// Commented this out because it clashes with selectable set in setLayoutSrc()
//		$("#canvas").selectable({
//			filter:".box",cancel:"a,span",delay:150
//		});

		$("#canvas").droppable();
	}
	else
	{
		$("#canvas, .container, #canvas .column").sortable(
		{
			connectWith:".column",opacity:.35,handle:".drag"
		});
	}

	fillPalette();
	
	//view click handlers
	$("#edit").click(function()
	{
		$("body").removeClass("editor devpreview sourcepreview");
		$("body").addClass("editor");
		removeMenuClasses();
		$(this).addClass("active");
		return false
	});
	$("#devpreview").click(function()
	{
		$("body").removeClass("editor devpreview sourcepreview");
		$("body").addClass("devpreview");
		removeMenuClasses();
		$(this).addClass("active");
		return false
	});
	$("#sourcepreview").click(function()
	{
		$("body").removeClass("editor devpreview sourcepreview");
		$("body").addClass("sourcepreview");
		removeMenuClasses();
		$(this).addClass("active");
		return false
	});
	
	//action click handlers
	$("#save").click(function(e)
	{
		e.preventDefault();
		window.prompt("Copy to clipboard: Ctrl+C, Enter", downloadLayoutSrc());
	});
	$("#refresh").click(function()
	{
		return false
	});
	$("#clear").click(function(e)
	{
		e.preventDefault();
		clearDemo()
	});
			
	//attach canvas size handlers
	var defaultsize = function()
	{
		$("#canvas").removeClass("size-desktop size-tablet size-phone").addClass("size-desktop");
	}
	$("#size-desktop").click(defaultsize);
	$("#size-tablet").click(function()
	{
		$("#canvas").removeClass("size-desktop size-tablet size-phone").addClass("size-tablet");
	});
	$("#size-phone").click(function()
	{
		$("#canvas").removeClass("size-desktop size-tablet size-phone").addClass("size-phone");
	});
	defaultsize();
	
	installRemoveDelegateAction();
	installConfigurationDelegateAction();
});