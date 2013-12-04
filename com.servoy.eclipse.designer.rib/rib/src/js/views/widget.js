/*
 * Rapid Interface Builder (RIB) - A simple WYSIWYG HTML5 app creator
 * Copyright (c) 2011-2012, Intel Corporation.
 *
 * This program is licensed under the terms and conditions of the
 * Apache License, version 2.0.  The full text of the Apache License is at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 */
"use strict";

// Widget view widget


(function($, undefined) {

    $.widget('rib.widgetView',  $.rib.treeView, {

        _create: function() {
            var widget = this;
            /* $.getJSON("src/assets/groups.json", */ /*{*/var readGroups =/*}*/ function (groups) {
                var resolveRefs = function (root, data) {
                    $.each(data, function(name, value) {
                        var refObj;
                        if (value &&  typeof value == "string" &&
                            value.indexOf('#') == 0) {
                            refObj = root;
                            $.each(value.substring(1).split('.'),
                                function (i, attr) {
                                    refObj = refObj[attr];
                                });
                            data.splice(data.indexOf(value), 1, refObj);
                        }
                        else if (value && typeof value === "object")
                            resolveRefs(root, value);
                    });
                };
                resolveRefs(groups, groups);
                widget._setOption("model", groups);
                widget.findDomNode(groups[0]['Functional Groups'])
                    .find('> a').trigger('click');
            }/* )*/;
	    /*{ */
	    readGroups(
[{
    "_hidden_node": {
        "jqm_toolbars":["Header" /*, "CustomHeader"*/, "Footer"/* , "Navbar"*/],
        "jqm_input_boolean":[/* "ToggleSwitch", */"RadioGroup", "SingleCheckbox", "CheckboxGroup"],
        "jqm_other_inputs":[ {
                    /* "Form": ["Form"],*/
                    "Text": ["TextInput", "PasswordField", "TextArea", "Calendar", "Bean"],
                    "Select": ["SelectMenu"]/*,
                    "Number": ["Slider"]*/
            }
         ]
    },
    "Functional Groups": [
        {
            "Toolbars": ["#0._hidden_node.jqm_toolbars"],
            "Buttons": ["Button"/* , "ButtonGroup"*/],
            "Content Formatting": [/* "Grid", "Collapsible", "Accordion", "Div"*/],
            "Form Elements": [
                "#0._hidden_node.jqm_other_inputs.0", /* "Text",*/
                {
                    "Boolean": ["#0._hidden_node.jqm_input_boolean"]
                }
            ],
            "List Views": [
                 "InsetList"/* List , "OrderedList", "ListItem", "ListDivider", "ListButton"
          */ ],
          "Image": [/*"Image"*/],
          "Label": ["Label"]
    }
    ],

    "Widget Sets": [
        {
            "Simple HTML": [ "Text", "Image", "Div" ],
            "jQuery Mobile": [ 
                "#0._hidden_node.jqm_toolbars",
                "#0.Functional Groups.0.Buttons",
                "#0.Functional Groups.0.Content Formatting",
                "#0._hidden_node.jqm_other_inputs",
                "#0._hidden_node.jqm_input_boolean",
                "#0.Functional Groups.0.List Views"
            ]
        }
    ]
}]
			    )
	    /*} */
            this.enableKeyNavigation();
            return this;
        },

        _nodeSelected: function (treeModelNode, data, domNode) {
            this._setSelected(domNode);
            $(':rib-paletteView').paletteView('option', "model", treeModelNode);
        },

        resize: function(event, widget) {
            var headerHeight = 30, resizeBarHeight = 20, used, e;
            e = this.element;

            // allocate 30% of the remaining space for the filter tree
            used = 2 * headerHeight + resizeBarHeight;
            e.height(Math.round((e.parent().height() - used) * 0.3));
        }
    });
})(jQuery);
