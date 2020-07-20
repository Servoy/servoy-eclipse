// https://github.com/Fyrd/purejs-datalist-polyfill
// license: MIT

import { DOCUMENT } from "@angular/common";
import { Inject, Injectable } from "@angular/core";

@Injectable()
export class DatalistPolyFill {

    public static IE_SELECT_ATTRIBUTE = 'data-datalist';
    public static LIST_CLASS = 'datalist-polyfill';
    public static ACTIVE_CLASS = 'datalist-polyfill__active';

    constructor(@Inject(DOCUMENT) private document: Document){}

    triggerEvent = function(elem, eventType) {
        var event;
        if (this.document.createEvent) {
            event = this.document.createEvent("HTMLEvents");
            event.initEvent(eventType, true, true);
            elem.dispatchEvent(event);
        } else {
            event = this.document.createEventObject();
            event.eventType = eventType;
            elem.fireEvent("on" + eventType, event);
        }
    };

    apply = function(input) {

        var datalistSupported = !!(this.document.createElement('datalist') && window.HTMLDataListElement);
        var ua = navigator.userAgent;
        
        // Android does not have actual support
        var isAndroidBrowser = ua.match(/Android/) && !ua.match(/(Firefox|Chrome|Opera|OPR)/);
        if( datalistSupported && !isAndroidBrowser ) {
            return null;
        }

        var listId = input.getAttribute('list');
        var datalist = this.document.getElementById(listId);
        if( !datalist ) {
            console.error('No datalist found for input: ' + listId);
            return null;
        }
        
        // Only visible to <= IE9
        var childSelect = this.document.querySelector('select[' + DatalistPolyFill.IE_SELECT_ATTRIBUTE + '="' + listId + '"]');
        var parent = childSelect || datalist;
        var listItems = parent.getElementsByTagName('option');
        var fakeList = this.convert(input, datalist, listItems, listId);
        if( childSelect ) {
            childSelect.parentNode.removeChild( childSelect );
        }
        return fakeList;
    };

    convert = function(input, datalist, listItems, listId) {
        var fakeList = this.document.createElement('ul');
        var visibleItems = null;
        fakeList.id = listId;
        fakeList.className = DatalistPolyFill.LIST_CLASS;
        this.document.body.appendChild( fakeList );

        var scrollValue = 0;

          // Used to prevent reflow
        var tempItems = this.document.createDocumentFragment();
        
        for( var i = 0; i < listItems.length; i++ ) {
            var item = listItems[i];
            var li = this.document.createElement('li');
            li.innerText = item.value;
            tempItems.appendChild( li );
        }
        fakeList.appendChild( tempItems );
        var fakeItems = fakeList.childNodes;
        var eachItem = function(callback) {
            for( var i = 0; i < fakeItems.length; i++ ) {
                callback(fakeItems[i]);
            }
        };
        var listen = function(elem, event, func) {
            if( elem.addEventListener ) {
                elem.addEventListener(event, func, false);
            } else {
                elem.attachEvent('on' + event, func);
            }
        };
        
        datalist.parentNode.removeChild( datalist );
        
        listen(input, 'focus', function() {
            // Reset scroll
            fakeList.scrollTop = 0;
            scrollValue = 0;
        });
        
        listen(input, 'blur', function(evt) {
            // If this fires immediately, it prevents click-to-select from working
            setTimeout(function() {
                fakeList.style.display = 'none';
                eachItem( function(item) {
                    // Note: removes all, not just ACTIVE_CLASS, but should be safe
                    item.className = '';
                });
            }, 100);
        });
        
        var positionList = function() {
            var positionAndSize = input.getBoundingClientRect();
            fakeList.style.top = positionAndSize.top + positionAndSize.height + 'px';
            fakeList.style.left = positionAndSize.left + 'px';
            fakeList.style.width = positionAndSize.width + 'px';
        };
        
        var itemSelected = function(item) {
            input.value = item.innerText == '' ? ' ' : item.innerText;
            this.triggerEvent(input, 'change');
            setTimeout(function() {
                fakeList.style.display = 'none';
                input.focus();
            }, 100);
        };
        
        var buildList = function(e) {
            // Build datalist
            fakeList.style.display = 'block';
            positionList();
            visibleItems = [];
            eachItem( function(item) {
                // Note: removes all, not just ACTIVE_CLASS, but should be safe
                var query = input.value.toLowerCase();
                var itemTxt = item.innerText.toLowerCase();
                if(itemTxt == '') itemTxt = ' ';
                var isFound = query.length == 0 || itemTxt.indexOf( query ) > -1;
                if( isFound ) {
                    visibleItems.push( item );
                }
                item.style.display = isFound ? 'block' : 'none';
            } );
        };
        
        listen(input, 'keyup', function(e) {
            if(e.keyCode != 13) {
                this.buildList();
            }
        });
        listen(input, 'mousedown', function() {
            if(this.document.activeElement == input) {
                this.buildList();
            }
        });
        
        // Don't want to use :hover in CSS so doing this instead
        // really helps with arrow key navigation
        eachItem( function(item) {
            // Note: removes all, not just ACTIVE_CLASS, but should be safe
            listen(item, 'mouseover', function(evt) {
                eachItem( function(_item) {
                    _item.className = item == _item ? DatalistPolyFill.ACTIVE_CLASS : '';
                });
            });
            listen(item, 'mouseout', function(evt) {
                item.className = '';
            });
            // Mousedown fires before native 'change' event is triggered
            // So we use this instead of click so only the new value is passed to 'change'
            listen(item, 'mousedown', function(evt) {
                itemSelected(item);
            });
        });
        
        listen(window, 'resize', positionList);
        
        listen(input, 'keydown', function(e) {
            var activeItem = fakeList.querySelector("." + DatalistPolyFill.ACTIVE_CLASS);
            if( !visibleItems.length ) {
                return;
            }
            
            var lastVisible = visibleItems[ visibleItems.length-1 ];
            var datalistItemsHeight = lastVisible.offsetTop + lastVisible.offsetHeight;
            
            // up/down arrows
            var isUp = e.keyCode == 38;
            var isDown = e.keyCode == 40;
            if ( (isUp || isDown) ) {
                if( isDown && !activeItem ) {
                    visibleItems[0].className = DatalistPolyFill.ACTIVE_CLASS;
                } else if (activeItem) {
                    var prevVisible = null;
                    var nextVisible = null;
                    for( var i = 0; i < visibleItems.length; i++ ) {
                        var visItem = visibleItems[i];
                        if( visItem == activeItem ) {
                            prevVisible = visibleItems[i-1];
                            nextVisible = visibleItems[i+1];
                            break;
                        }
                    }

                    activeItem.className = '';
                    if ( isUp ) {
                        if( prevVisible ) {
                            prevVisible.className = DatalistPolyFill.ACTIVE_CLASS;
                            if ( prevVisible.offsetTop < fakeList.scrollTop ) {
                                fakeList.scrollTop -= prevVisible.offsetHeight;
                            }
                        } else {
                            visibleItems[visibleItems.length - 1].className = DatalistPolyFill.ACTIVE_CLASS;
                        }
                    }
                    if ( isDown ) {
                        if( nextVisible ) { 
                            nextVisible.className = DatalistPolyFill.ACTIVE_CLASS;
                            if( nextVisible.offsetTop + nextVisible.offsetHeight > fakeList.scrollTop + fakeList.offsetHeight ) {
                                fakeList.scrollTop += nextVisible.offsetHeight;
                            }
                        } else {
                            visibleItems[0].className = DatalistPolyFill.ACTIVE_CLASS;
                        }
                    }
                }
            }
            
            // return or tab key
            if ( activeItem && (e.keyCode == 13 || e.keyCode == 9) ){
                itemSelected(activeItem);
            }
        });
        return fakeList;
    }
}