/**
 * Method to generate page
 * @author Michael DUBUIS
 */
/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * * * * * * * * * * * * * * * * * * * * * PAGE GENERATORS * * * * * * * * * * * * * * * * * * * * * * *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
function getElement(json) {
	if(json.element=="text"){
		var text = document.createTextNode(json.value);
		return text;
	}
	var element = document.createElement(json.element);
	if(json.element == "textarea"){
		$(element).attr("onkeyup", "textAreaAdjust(this);");
		$(element).attr("onfocus", "textAreaAdjust(this);");
		$(element).attr("onscroll", "textAreaAdjust(this);");
	}
	$.each(json.attributes, function(key, value){
		$(element).attr(key, value);
	});
	for( var i = 0 ; i < json.inside.length ; i++ ){
		$(element).append(getElement(json.inside[i]));
	}
	return element;
}

function textAreaAdjust(o) {
    o.style.height = "1px";
    o.style.height = (20+o.scrollHeight)+"px";
}

function getHeader(){
	var div = document.createElement("div");
	for ( var i = 0 ; i < header.length ; i++ ) {
		$(div).append(getElement(header[i]));
	}
	return div;
}

function getMenu(){
	var nav = document.createElement("nav");
	$(nav).append(getElement(menu));
	return nav;
}

function clearFeedback() {
	$("p.feedbackBox").empty();
}

function printFeedback(feedback, isOk) {
	$("p.feedbackBox").removeClass("feedbackOk");
	clearFeedback();
	if(feedback.length > 0) {
		$("p.feedbackBox").append(feedback);
		$("p.feedbackBox").removeClass("hidden");
	} else {
		$("p.feedbackBox").addClass("hidden");
	}
	if(isOk) {
		$("p.feedbackBox").addClass("feedbackOk");
	}
}