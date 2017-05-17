jQuery(document).ready(function($) {
    // The template to load
    var _template = getParameterByName('template');
    // The step where it should start, defaults to 0
    var _step = getParameterByName('step');
    if (_template) {
        $.getJSON("./templates/" + _template + ".json", function(data) {
            SVY_TEMPLATE.setTemplate(data)
            SVY_TEMPLATE.setStep(((_step) ? parseInt(_step) : 0));
        });
    }
});

var SVY_TEMPLATE = {
    template: [],
    curIndex: 0,
    setTemplate: function(json) {
        this.template = json
    },
    getTemplate: function() {
        return this.template;
    },
    hasNext: function() {
        return ((this.curIndex < this.template["steps"].length - 1) ? true : false);
    },
    hasPrev: function() {
        return ((this.curIndex != 0) ? true : false);
    },
    nextStep: function() {
        this.setStep(this.curIndex + 1);
    },
    prevStep: function() {
        this.setStep(this.curIndex - 1);
    },
    setStep: function(step) {
        $("[data-template]").hide();
        var _template = this.template["steps"][step].template;
        this.curIndex = step;
        $("[data-template=" + _template + "]").show();

        if (this.template["steps"][step].showFooterNavigation) {
            $('.footer').show();
        } else {
            $('.footer').hide();
        }
        $(".button-area").empty();

        for (var i in this.template["steps"][step]['elements']) {
            element = this.template["steps"][step]['elements'][i];
            console.log(element);


            switch (element.type) {
                case "popup-title":
                case "sub-title":
                    $('[id=' + element.type + ']').html(element.value);
                    break;

                case "menu":
                    $('[id=' + element.id + '] h2').html(element.title);
                    $('[id=' + element.id + '] ul').empty();
                    for (var k in element.items) {
                        $('[id=' + element.id + '] ul').append('<li><a href="' + element.items[k].url + '"><img src="assets/img/' + element.items[k].icon + '" /><span>' + element.items[k].title + '</span></a></li>');
                    }
                    break;

                case "three-icon-icons":
                    $('[id=three-icon-icons] div.center').empty();
                    for (var k in element.items) {
                        $('[id=three-icon-icons] div.center').append('<div class="icon-item"><a href="' + element.items[k].url + '"><img src="assets/img/' + element.items[k].icon + '" /><span class="title">' + element.items[k].title + '</span><span class="sub-title">' + element.items[k].subtitle + '</span></a></div>');
                    }
                    break;

                case "two-icon-icons":
                    $('[id=two-icon-icons] div.center').empty();
                    for (var k in element.items) {
                        $('[id=two-icon-icons] div.center').append('<div class="icon-item"><a href="' + element.items[k].url + '"><img src="assets/img/' + element.items[k].icon + '" /><span class="title">' + element.items[k].title + '</span><span class="sub-title">' + element.items[k].subtitle + '</span></a></div>');
                    }
                    break;

                case "image":
                    $('.textual-image').attr('style', "background-image:url('assets/img/" + element.value + "')");
                    break;

                case "image-tag":
                    $('.image').html("<img src=\"assets/img/" + element.value + "\" />");
                    break;

                case "title":
                    $('#title').html(element.value);
                    break;

                case "intro-text":
                    $('#intro-text').html(element.value);
                    break;

                case "button":
                    $('#' + element.id).html(element.value);
                    break;

                case "welcome-button":
                    $(".button-area").append('<div class="button-container">\
                    <div class="title">' + element.title + '</div>\
                    <div class="text">' + element.text + '</div>\
                    <div clss="button">\
                    <a href="' + element.buttonUrl + '">' + element.buttonText + '</a>\
                    </div>\
                </div>');
                    break;

                case "list":
                    $('#list-title').html(element.title);
                    $('[id=list-items] ul').empty();
                    for (var k in element.items) {
                        $('[id=list-items] ul').append('<li>' + element.items[k] + '</li>');
                    }
                    break;
            }
        }

        if (!this.hasPrev()) {
            $('.button-bar .prev').hide();
        } else {
            $('.button-bar .prev').show();
        }

        if (!this.hasNext()) {
            $('.button-bar .next').hide();
        } else {
            $('.button-bar .next').show();
        }

        $('.steps').empty();
        for (var i in this.template["steps"]) {
            $('.steps').append('<div class="step-indicator ' + ((i == step) ? "active" : "") + '"></div>');
        }
    }
}


/* Functions to get the Query Var */
function getParameterByName(name, url) {
    if (!url) {
        url = window.location.href;
    }
    name = name.replace(/[\[\]]/g, "\\$&");
    var regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)"),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';
    return decodeURIComponent(results[2].replace(/\+/g, " "));
}