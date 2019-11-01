import { Injectable } from '@angular/core';

import {WindowRefService} from '../../sablo/util/windowref.service'

import {ServiceChangeHandler} from '../../sablo/util/servicechangehandler'
import { ServoyService } from "../../ngclient/servoy.service";

@Injectable()
export class NGUtilsService{
    private _tags:Tag[];
    private _styleclasses:Object;
    private _backActionCB: any;
    private confirmMessage:string;

    constructor(private windowRef:WindowRefService,
            private changeHandler : ServiceChangeHandler,
            private servoyService : ServoyService) {
    }
    
    get contributedTags():Tag[] {
        return this._tags;
    }
    
    set contributedTags(tags:Tag[]) {
            this._tags = tags;
           // do something with it..
    }

    get styleclasses():Object {
        return this._styleclasses;
    }
    
    set styleclasses(styleclasses) {
         this._styleclasses = styleclasses;
         this.changeHandler.changed("ngclientutils","styleclasses", styleclasses);
    }
    
    set backActionCB(backActionCB: any) {
        this._backActionCB = backActionCB;
        this.setBackActionCallback();
    }

    private beforeUnload(e) {
        (e || window.event).returnValue = this.confirmMessage; //Gecko + IE
        return this.confirmMessage; //Gecko + Webkit, Safari, Chrome etc.
    };


    /**
     * This will return the user agent string of the clients browser.
     */
    public getUserAgent()
    {
        return this.windowRef.nativeWindow.navigator.userAgent;
    }

    /**
     * Set the message that will be shown when the browser tab is closed or the users navigates away, 
     * this can be used to let users know they have data modifications that are not yet saved. 
     * Note: We deprecated this api because browsers removed support for custom messages of beforeunload event. Now most browsers display a standard message.
     *
     * @param {string} message the message to show when the user navigates away, null if nothing should be shown anymore.
     * @deprecated
     */
    public setOnUnloadConfirmationMessage(message:string)
    {
        this.confirmMessage = message;
        if (this.confirmMessage) {
            // duplicate add's of the same type and function are ignored 
            // if an existing message would be updated with a new one
            this.windowRef.nativeWindow.window.addEventListener("beforeunload", this.beforeUnload);
        }
        else {
            this.windowRef.nativeWindow.window.removeEventListener("beforeunload", this.beforeUnload);
        }
    }

    /**
     * Set whether browser default warning message will be shown when the browser tab is closed or the users navigates away, 
     * this can be used to let users know they have data modifications that are not yet saved. 
     *
     * @param {boolean} showConfirmation boolean for whether to show confirmation message
     */
    public setOnUnloadConfirmation(showConfirmation:boolean)
    {
        // the message is ignored lately by browsers
        this.confirmMessage = 'You have unsaved data. Are you sure you want to quit?';
        if (showConfirmation) {
            // duplicate add's of the same type and function are ignored 
            // if an existing message would be updated with a new one
            this.windowRef.nativeWindow.window.addEventListener("beforeunload", this.beforeUnload);
        }
        else {
            this.windowRef.nativeWindow.window.removeEventListener("beforeunload", this.beforeUnload);
        }
    }
    
    /**
     * Call this when a solution can handle mobile device layouts (responsive design, can handle nicely
     * width < height). This call is equivalent to calling setViewportMetaForMobileAwareSites(plugins.htmlHeaders.VIEWPORT_MOBILE_DEFAULT).<br/><br/>
     * 
     * It should be what most solutions that are able layout correctly on smaller mobile screens need; it will still allow the user to zoom-in
     * and zoom-out. 
     */
    public setViewportMetaDefaultForMobileAwareSites()
    {
        // implemented in ngclientutils_server.js
    }

    /**
     * Call this when a solution can handle mobile device layouts (responsive design, can handle nicely
     * width < height). It will tell the device via the "viewport" meta header that it doesn't need to
     * automatically zoom-out and use a big viewport to allow the page to display correctly as it would
     * on a desktop.<br/><br/>
     *
     * 'viewportDefType' can be one of:<br/>
     * <ul>
     * <li>plugins.ngclientutils.VIEWPORT_MOBILE_DEFAULT - will show content correctly, allow zoom-in and
     *                      zoom-out; the generated meta tag will be
     *                      <meta name="viewport" content="width=device-width, initial-scale=1.0" /></li>
     * <li>plugins.ngclientutils.VIEWPORT_MOBILE_DENY_ZOOM - will show content correctly, denies zoom-in
     *                      and zoom-out; the generated meta tag will be
     *                      <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0" /></li>
     * <li>plugins.ngclientutils.VIEWPORT_MOBILE_DENY_ZOOM_OUT - will show content correctly, allows zoom-in
     *                      but denies zoom-out; the generated meta tag will be
     *                      <meta name="viewport" content="width=device-width, initial-scale=1.0, minimum-scale=1.0" /></li>
     * <li>plugins.ngclientutils.VIEWPORT_MOBILE_DENY_ZOOM_IN - will show content correctly, denies zoom-in
     *                      but allows zoom-out; the generated meta tag will be
     *                      <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0" /></li>
     * </ul><br/>
     * This method actually uses replaceHeaderTag. For example plugins.ngclientutils.VIEWPORT_MOBILE_DEFAULT would call
     * <pre>  replaceHeaderTag("meta", "name", "viewport", {
     *        tagName: "meta",
     *        attrs: [ { name: "name", value: "viewport" },
     *                 { name: "content", value: "width=device-width, initial-scale=1.0" } ]
     *  });</pre>
     *
     * @param {int} viewportDefType one of the constants listed above.
     */
    public setViewportMetaForMobileAwareSites(viewportDefType)
    {
        // implemented in ngclientutils_server.js
    }

    /**
     * Utility method for manipulating 'contributedTags' array. It searches for an existing 'tag'
     * that has the given 'tagName' and attribute ('attrNameToFind' & 'attrValueToFind'). If found
     * it will replace it with 'newTag'. If not found it will just append 'newTag' to 'contributedTags'.<br/><br/>
     * 
     * NOTE: this call will only replace/remove tags that were added via this plugin/service, not others that were previously present in the DOM.
     * 
     * @param {string} tagName the tag name to find for replacement.
     * @param {string} attrNameToFind the attribute to find on that tag name for replacement. If null it will just find the first by 'tagName' and use that one.
     * @param {string} attrValueToFind the value the given attribute must have to match for replacement.
     * @param {tag} newTag the new tag that replaces the old one. If null/undefined it will just remove what it finds.
     * @return {tag} the tag that was removed if any.
     */
    public replaceHeaderTag(tagName, attrNameToFind, attrValueToFind, newTag)
    {
        // implemented in ngclientutils_server.js
    }
    
    /**
     * Utility method for manipulating form style classes. 
     * It will add a style class to a certain form, similar as a design style class would work.
     * 
     * @param {string} formname the form name to add to.
     * @param {string} styleclass the styleclass to be added to form tag.
     */
    public addFormStyleClass(formname,styleclass)
    {
        // implemented in ngutils_server.js
    }
    
    /**
     * Utility method for manipulating form style classes. 
     * It will get styleclasses assigned to a certain form, multiple styleclasses are separated by space.
     * 
     * NOTE: this call will only get style classes that were added via this plugin/service, not others that were previously set at design time or via solution model.
     * 
     * @param {string} formname the form name to get style classes.
     * @return {string} the styleclass of that form.
     */
    public getFormStyleClass(formname)
    {
        // implemented in ngutils_server.js
    }
    
    /**
     * Utility method for manipulating form style classes. 
     * It will remove a styleclasse assigned to a certain form.
     * 
     * NOTE: this call will only remove style classes that were added via this plugin/service, not others that were previously set at design time or via solution model.
     * 
     * @param {string} formname the form name to remove from.
     * @param {string} styleclass the styleclass to be removed from form tag.
     */
    public removeFormStyleClass(formname,styleclass)
    {
        // implemented in ngutils_server.js
    }
    
    public setServiceChangeHandler(changeHandler : ServiceChangeHandler)
    {
        this.changeHandler = changeHandler;
    }
    
    /**
     * Cancel the default back button action from browser and do a callback when clicking on it. 
     */
    private setBackActionCallback() {
        history.pushState('captureBack', null, null);
        this.windowRef.nativeWindow.window.addEventListener("popstate", (event) => {
            if (event.state && event.state == 'captureBack') {
                event.stopPropagation();
                event.preventDefault();
                this.servoyService.executeInlineScript(this._backActionCB.formname, this._backActionCB.script,[]);
                history.pushState('captureBack', null, null);
            }
        })
    }
}

class Tag {
    public tagname:string;
    public attrs:Attribute[];
}

class Attribute {
    public name:string;
    public value:string;
}