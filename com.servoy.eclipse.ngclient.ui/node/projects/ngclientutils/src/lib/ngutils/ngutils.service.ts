import { Injectable, Renderer2, Inject, RendererFactory2 } from '@angular/core';

import { DOCUMENT, PlatformLocation } from '@angular/common';
import { WindowRefService, ServoyPublicService } from '@servoy/public';

@Injectable()
export class NGUtilsService {
    private _tags: Tag[];
    private _tagscopy: Tag[];
    private _styleclasses: { property: string };
    private _backActionCB: any;
    private confirmMessage: string;
    private renderer: Renderer2;

    constructor(private windowRef: WindowRefService,
        private servoyService: ServoyPublicService,
        private platformLocation: PlatformLocation,
        rendererFactory: RendererFactory2,
        @Inject(DOCUMENT) private document: Document) {
        this.windowRef.nativeWindow.location.hash = '';
        this.renderer = rendererFactory.createRenderer(null, null);
    }

    get contributedTags(): Tag[] {
        return this._tags;
    }

    set contributedTags(tags: Tag[]) {
        const newTags = tags.slice();
        const uitags = this.document.querySelectorAll('head > [hhsManagedTag]');
        if (this._tagscopy) {
            for (let i = 0; i < this._tagscopy.length; i++) {
                const oldTag = this._tagscopy[i];
                let newIndex = -1;
                for (let j = 0; j < newTags.length; j++) {
                    const tag = newTags[j];
                    if (oldTag.tagName == tag.tagName && oldTag.attrs.length == tag.attrs.length) {
                        let sameAttributes = true;
                        for (let k = 0; k < tag.attrs.length; k++) {
                            if (oldTag.attrs[k].name != tag.attrs[k].name || oldTag.attrs[k].value != tag.attrs[k].value) {
                                sameAttributes = false;
                                break;
                            }
                        }
                        if (sameAttributes) {
                            newIndex = j;
                            break;
                        }
                    }
                }
                if (newIndex >= 0) {
                    // element was found unchanged, just leave it alone
                    newTags.splice(newIndex, 1);
                } else {
                    // element was deleted or changed, we must remove it from dom
                    for (let j = 0; j < uitags.length; j++) {
                        const uitag = uitags[j];
                        if (uitag.tagName.toLowerCase() === oldTag.tagName.toLowerCase()) {
                            let sameAttributes = true;
                            for (let k = 0; k < oldTag.attrs.length; k++) {
                                if (uitag.getAttribute(oldTag.attrs[k].name) != oldTag.attrs[k].value) {
                                    sameAttributes = false;
                                    break;
                                }
                            }
                            if (sameAttributes) {
                                uitag.remove();
                                break;
                            }
                        }
                    }
                }
            }
        } else {
            uitags.forEach(e => e.remove());;
        }

        newTags.forEach(tag => {
            const elem = this.renderer.createElement(tag.tagName);
            if (tag.attrs) {
                tag.attrs.forEach(attr => {
                    this.renderer.setAttribute(elem, attr.name, attr.value);
                });
            }
            this.renderer.setAttribute(elem, 'hhsManagedTag', '');
            this.renderer.appendChild(this.document.head, elem);
        });
        this._tags = tags;
        this._tagscopy = tags.slice();
    }

    get styleclasses(): any {
        return this._styleclasses;
    }

    set styleclasses(styleclasses) {
        this._styleclasses = styleclasses;
        this.servoyService.setFormStyleClasses(styleclasses);
    }

    set backActionCB(backActionCB: any) {
        this._backActionCB = backActionCB;
        this.setBackActionCallback();
    }

    /**
     * This will return the user agent string of the clients browser.
     */
    public getUserAgent() {
        return this.windowRef.nativeWindow.navigator.userAgent;
    }

    /**
     *
     *
     * Set the message that will be shown when the browser tab is closed or the users navigates away,
     *
     * this can be used to let users know they have data modifications that are not yet saved.
     *
     * Note: We deprecated this api because browsers removed support for custom messages of beforeunload event. Now most browsers display a standard message.
     *
     *
     *
     * @param message the message to show when the user navigates away, null if nothing should be shown anymore.
     * @deprecated
     */
    public setOnUnloadConfirmationMessage(message: string) {
        this.confirmMessage = message;
        if (this.confirmMessage) {
            // duplicate add's of the same type and function are ignored
            // if an existing message would be updated with a new one
            this.windowRef.nativeWindow.window.addEventListener('beforeunload', this.beforeUnload);
        } else {
            this.windowRef.nativeWindow.window.removeEventListener('beforeunload', this.beforeUnload);
        }
    }

    /**
     *
     *
     * Set whether browser default warning message will be shown when the browser tab is closed or the users navigates away,
     *
     * this can be used to let users know they have data modifications that are not yet saved.
     *
     *
     *
     * @param showConfirmation boolean for whether to show confirmation message
     */
    public setOnUnloadConfirmation(showConfirmation: boolean) {
        // the message is ignored lately by browsers
        this.confirmMessage = 'You have unsaved data. Are you sure you want to quit?';
        if (showConfirmation) {
            // duplicate add's of the same type and function are ignored
            // if an existing message would be updated with a new one
            this.windowRef.nativeWindow.window.addEventListener('beforeunload', this.beforeUnload);
        } else {
            this.windowRef.nativeWindow.window.removeEventListener('beforeunload', this.beforeUnload);
        }
    }

    /**
     * Call this when a solution can handle mobile device layouts (responsive design, can handle nicely
     * width < height). This call is equivalent to calling setViewportMetaForMobileAwareSites(plugins.htmlHeaders.VIEWPORT_MOBILE_DEFAULT).<br/><br/>
     *
     * It should be what most solutions that are able layout correctly on smaller mobile screens need; it will still allow the user to zoom-in
     * and zoom-out.
     */
    public setViewportMetaDefaultForMobileAwareSites() {
        // implemented in ngclientutils_server.js
    }

    /**
     *
     *
     * Call this when a solution can handle mobile device layouts (responsive design, can handle nicely
     *
     * width < height). It will tell the device via the "viewport" meta header that it doesn't need to
     *
     * automatically zoom-out and use a big viewport to allow the page to display correctly as it would
     *
     * on a desktop.<br/><br/>
     *
     *
     *
     * 'viewportDefType' can be one of:<br/>
     *
     * <ul>
     *
     * <li>plugins.ngclientutils.VIEWPORT_MOBILE_DEFAULT - will show content correctly, allow zoom-in and
     *
     *                      zoom-out; the generated meta tag will be
     *
     *                      <meta name="viewport" content="width=device-width, initial-scale=1.0" /></li>
     *
     * <li>plugins.ngclientutils.VIEWPORT_MOBILE_DENY_ZOOM - will show content correctly, denies zoom-in
     *
     *                      and zoom-out; the generated meta tag will be
     *
     *                      <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0" /></li>
     *
     * <li>plugins.ngclientutils.VIEWPORT_MOBILE_DENY_ZOOM_OUT - will show content correctly, allows zoom-in
     *
     *                      but denies zoom-out; the generated meta tag will be
     *
     *                      <meta name="viewport" content="width=device-width, initial-scale=1.0, minimum-scale=1.0" /></li>
     *
     * <li>plugins.ngclientutils.VIEWPORT_MOBILE_DENY_ZOOM_IN - will show content correctly, denies zoom-in
     *
     *                      but allows zoom-out; the generated meta tag will be
     *
     *                      <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0" /></li>
     *
     * </ul><br/>
     *
     * This method actually uses replaceHeaderTag. For example plugins.ngclientutils.VIEWPORT_MOBILE_DEFAULT would call
     *
     * <pre>  replaceHeaderTag("meta", "name", "viewport", {
     *
     *        tagName: "meta",
     *
     *        attrs: [ { name: "name", value: "viewport" },
     *
     *                 { name: "content", value: "width=device-width, initial-scale=1.0" } ]
     *
     *  });</pre>
     *
     *
     *
     * @param viewportDefType one of the constants listed above.
     */
    public setViewportMetaForMobileAwareSites(_viewportDefType: number) {
        // implemented in ngclientutils_server.js
    }

    /**
     *
     *
     * Utility method for manipulating 'contributedTags' array. It searches for an existing 'tag'
     *
     * that has the given 'tagName' and attribute ('attrNameToFind' & 'attrValueToFind'). If found
     *
     * it will replace it with 'newTag'. If not found it will just append 'newTag' to 'contributedTags'.<br/><br/>
     *
     *
     *
     * NOTE: this call will only replace/remove tags that were added via this plugin/service, not others that were previously present in the DOM.
     *
     *
     *
     * @param tagName the tag name to find for replacement.
     * @param attrNameToFind the attribute to find on that tag name for replacement. If null it will just find the first by 'tagName' and use that one.
     * @param attrValueToFind the value the given attribute must have to match for replacement.
     * @param newTag the new tag that replaces the old one. If null/undefined it will just remove what it finds.
     * @return the tag that was removed if any.
     */
    public replaceHeaderTag(_tagName: string, _attrNameToFind: string, _attrValueToFind: string, _newTag: string) {
        // implemented in ngclientutils_server.js
    }

    /**
     *
     *
     * Utility method for manipulating form style classes.
     *
     * It will add a style class to a certain form, similar as a design style class would work.
     *
     *
     *
     * @param formname the form name to add to.
     * @param styleclass the styleclass to be added to form tag.
     */
    public addFormStyleClass(_formname: string, _styleclass: string) {
        // implemented in ngutils_server.js
    }

    /**
     *
     *
     * Utility method for manipulating form style classes.
     *
     * It will get styleclasses assigned to a certain form, multiple styleclasses are separated by space.
     *
     *
     *
     * NOTE: this call will only get style classes that were added via this plugin/service, not others that were previously set at design time or via solution model.
     *
     *
     *
     * @param formname the form name to get style classes.
     * @return the styleclass of that form.
     */
    public getFormStyleClass(_formname: string) {
        // implemented in ngutils_server.js
    }

    /**
     *
     *
     * Utility method for manipulating form style classes.
     *
     * It will remove a styleclasse assigned to a certain form.
     *
     *
     *
     * NOTE: this call will only remove style classes that were added via this plugin/service, not others that were previously set at design time or via solution model.
     *
     *
     *
     * @param formname the form name to remove from.
     * @param styleclass the styleclass to be removed from form tag.
     */
    public removeFormStyleClass(_formname: string, _styleclass: string) {
        // implemented in ngutils_server.js
    }
    
    /**
	* Print a document from specific URL. This will open browser specific print dialog. 
	* 
	* NOTE: url should be from same domain, otherwise a temp file on server should be created and served
	* 
	* @sample
	* 	//if url is not from same domain we must create a temporary file
	* 	var file = plugins.http.getMediaData(url);  
	*	var remoteFileName = application.getUUID().toString() + '.pdf'; 
	*	var remoteFile = plugins.file.convertToRemoteJSFile('/'+remoteFileName) 
	*	remoteFile.setBytes(file,true);  //Convert the remote file to a url, and print it
	*	var remoteUrl = plugins.file.getUrlForRemoteFile('/'+remoteFileName);  
	*	plugins.ngclientutils.printDocument(remoteUrl)
	* @param {string} url The URL of document to be printed.
	*/
    public printDocument(url: string) {
		const objFra = document.createElement('iframe');   
	    objFra.style.visibility = 'hidden';    
	    objFra.src = url;                      
	    document.body.appendChild(objFra);  
	    objFra.contentWindow.focus();      
	    objFra.contentWindow.print();
	}

    /**
    * Retrieves the screen location of a specific element. Returns the location as point (object with x and y properties).
    *
    * @param component the component to retrieve location for.
    * @return the location of the component.
    */
    public getAbsoluteLocation(component: string): { x: number; y: number } {
        const el = this.document.getElementById(component);
        if (el) {
            const rect = el.getBoundingClientRect();
            return { x: rect.left + this.windowRef.nativeWindow.scrollX, y: rect.top + this.windowRef.nativeWindow.scrollY };
        }
        return null;
    }
    
    /**
	* Set lang attribute on html tag.
	* 
	* @param {string} lang of the html page
	*/
	public setLangAttribute(lang: string)
	{
		document.querySelector('html').setAttribute('lang', lang);
	}

    /**
     *
     *
     * This will register a callback that will be triggered on all history/window popstate events (back,forward but also next main form).
     *
     * If this is registered then we will try to block the application from going out of the application.
     *
     * The callback gets 1 argument and that is the hash of the url, that represents at this time the form where the back button would go to.
     *
     * If this hash argument is an empty string then that means the backbutton was hit to the first loaded page and we force a forward again.
     *
     *
     *
     * @param callback
     */
    public setBackActionCallback() {
        this.platformLocation.onPopState(() => {
            if (this._backActionCB) {
                if (this.platformLocation.hash) {
                    this._backActionCB(this.platformLocation.hash);
                } else if (this.platformLocation.href.endsWith('/index.html')) {
                    this.platformLocation.forward(); // if the back button is registered then don't allow to move back, go to the first page again.
                }
            }
        });
    }

    /**
     * Move the scrollbar to the position of the given anchorSelector.
     * The target anchorSelector can be a Servoy Form, Layout Container or element in a responsive form or any element in a form.
     * You can use styleClass as selector.
     * For example: you can add 'scroll-element' to an element of the form.
     * Examples of usage:
     * - plugins.ngclientutils.scrollIntoView(".toScroll-To");
     * - plugins.ngclientutils.scrollIntoView(".toScroll-To", { behavior: "smooth", block: "start", inline: "nearest" });

     * @param anchorSelector {string} the selector to which the scrollbar should be moved to.
     * @param scrollIntoViewOptions option argument used for scrolling animation (example:  { behavior: "smooth", block: "start", inline: "nearest" }).
     */
    public scrollIntoView(anchorSelector: string, scrollIntoViewOptions?: any) {
        const anchor = this.document.querySelector(anchorSelector);
        if (anchor) {
            if (!scrollIntoViewOptions) scrollIntoViewOptions = { behavior: 'smooth', block: 'start', inline: 'nearest' };
            // move scrolling to position
            anchor.scrollIntoView(scrollIntoViewOptions);
        } else {
            console.log('cannot find anchor element ' + anchorSelector);
        }
    }
    
    /**
     * Move the scrollbar to top position of the given selector.
     * The target selector can be a Servoy Form, Layout Container or element in a responsive form or any element in a form.
     * You can use styleClass as selector.
     * For example: you can add 'scroll-element' to an element of the form.
     * Examples of usage:
     * - plugins.ngclientutils.scrollToTop(".toScroll-To");

     * @param selector {string} the selector to which the scrollbar should be moved to top.
     */
    public scrollToTop(selector: string) {
        // find container
        const container = this.document.querySelector(selector);
        
        // validate elements found
        if (!container) {
			console.warn(`cannot find container ${selector}`);
			return;
		}
        
        // move scrolling to top position
        window.scrollTo({top: container.getBoundingClientRect().top + window.scrollY, behavior: 'smooth'});
    }

    /**
     * Utility method for manipulating any DOM element's style classes.
     * It will add the given class to the DOM element identified via the jQuery selector param.
     * 
     * NOTE: This operation is not persistent; it executes client-side only; so for example when the browser is reloaded (F5/Ctrl+F5) by the user classes added by this method are lost.
     * If you need this to be persistent - you can do that directly via server side scripting elements.myelement.addStyleClass(...) if the DOM element is a Servoy component. If the DOM element is
     * not a component then you probably lack something in terms of UI and you could build what you need as a new custom component or use another approach/set of components when building the UI.
     * 
     * @param cssSelector {string} the css selector string that is used to find the DOM element.
     * @param className {string} the class to be added to the element.
     */
    public addClassToDOMElement(cssSelector: string, className: string) {
        const nodeList = this.document.querySelectorAll(cssSelector);
        for (let i = 0; i < nodeList.length; i++) {
            nodeList[i].classList.add(className);
        }
    }

    /**
     * Utility method for manipulating any DOM element's style classes.
     * It will remove the given class from the DOM element identified via the jQuery selector param.
     * 
     * NOTE: This operation is not persistent; it executes client-side only; so for example when the browser is reloaded (F5/Ctrl+F5) by the user classes removed by this method are lost;
     * If you need this to be persistent - you can do that directly via server side scripting elements.myelement.removeStyleClass(...) if the DOM element is a Servoy component. If the DOM element it is
     * not a component then you probably lack something in terms of UI and you could build what you need as a new custom component or use another approach/set of components when building the UI.
     * 
     * @param cssSelector {string} the css selector string that is used to find the DOM element.
     * @param className {string} the class to be added to the element.
     */
    public removeClassFromDOMElement(cssSelector: string, className: string) {
        const nodeList = this.document.querySelectorAll(cssSelector);
        for (let i = 0; i < nodeList.length; i++) {
            nodeList[i].classList.remove(className);
        }
    }

    /**
    * This method removes the arguments from the client url. This is used for bookmark url to be correct or for back button behavior.
    */
    public removeArguments() {
        this.windowRef.nativeWindow.history.replaceState({}, '', this.windowRef.nativeWindow.location.pathname + this.windowRef.nativeWindow.location.hash);
    }

    private beforeUnload(e: any) {
        (e || window.event).returnValue = this.confirmMessage; //Gecko + IE
        return this.confirmMessage; //Gecko + Webkit, Safari, Chrome etc.
    };

}

class Tag {
    public tagName: string;
    public attrs: Attribute[];
}

class Attribute {
    public name: string;
    public value: string;
}
