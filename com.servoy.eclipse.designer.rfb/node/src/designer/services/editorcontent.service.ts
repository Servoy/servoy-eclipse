import { Injectable, Inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { WindowRefService } from '@servoy/public';
import { URLParserService } from '../services/urlparser.service';

@Injectable()
export class EditorContentService {
    private frameElement: HTMLIFrameElement;
    private contentAreaElement: HTMLElement;
    private contentElement: HTMLElement;
    private glassPaneElement: HTMLElement;
    private palette: HTMLElement;
    private afterInitCallbacks: Array<() => void> = new Array<() => void>();
    private contentMessageListeners: Array<IContentMessageListener> = new Array<IContentMessageListener>();
    private contentWasInit = false;
    
    private topAdjust: number;
    private leftAdjust: number;
    
    constructor(@Inject(DOCUMENT) private document: Document, private windowRefService: WindowRefService, private urlParser: URLParserService) {
        windowRefService.nativeWindow.addEventListener('message', (event: MessageEvent<{ id: string, formname: string }>) => {
            // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
            if (event.data.id === 'afterContentInit') {
                if (event.data.formname == this.urlParser.getFormName()) {
                    this.contentWasInit = true;
                    this.afterInitCallbacks.forEach(listener => {
                        listener();
                    });
                    this.afterInitCallbacks.splice(0, this.afterInitCallbacks.length);
                }
            }
            else if (!event.data.formname || event.data.formname == this.urlParser.getFormName()){
                this.contentMessageListeners.forEach(listener => listener.contentMessageReceived(event.data.id, event.data));
            }

        });
    }

    getContentElement(nodeid: string): HTMLElement {
        this.initIFrame();
        return this.frameElement.contentWindow.document.querySelector("[svy-id='" + nodeid + "']");
    }

    getAllContentElements(): Array<HTMLElement> {
        this.initIFrame();
        return Array.from(this.frameElement.contentWindow.document.querySelectorAll("[svy-id]"));
    }

    querySelectorAllInContent(selector: string): Array<HTMLElement> {
        this.initIFrame();
        return Array.from(this.frameElement.contentWindow.document.querySelectorAll(selector));
    }

    getContentForm(): HTMLElement {
        this.initIFrame();
        return this.frameElement.contentWindow.document.querySelector('.svy-form');
    }

    getContentElementsFromPoint(point: { x: number; y: number; }): Element[] {
        this.initIFrame();
        return Array.from(this.frameElement.contentWindow.document.elementsFromPoint(point.x, point.y));
    }

    getTopPositionIframe(variants?: boolean): number {
        this.initIFrame(variants);
        const frameRect = this.frameElement.getBoundingClientRect();
        return frameRect.top;
    }

    getLeftPositionIframe(variants?: boolean): number {
        this.initIFrame(variants);
        const frameRect = this.frameElement.getBoundingClientRect();
        return frameRect.left;
    }

    getContentArea(): HTMLElement {
        if (!this.contentAreaElement) {
            this.contentAreaElement = this.document.querySelector('.content-area');
        }
        return this.contentAreaElement;
    }

    getPallete(): HTMLElement {
        if (!this.palette) {
            this.palette = this.document.querySelector('.palette');
        }
        return this.palette;
    }

    getContent(): HTMLElement {
        if (!this.contentElement) {
            this.contentElement = this.document.querySelector('.content');
        }
        return this.contentElement;
    }

    getGlassPane(): HTMLElement {
        if (!this.glassPaneElement) {
            this.glassPaneElement = this.document.querySelector('.contentframe-overlay');
        }
        return this.glassPaneElement;
    }

    getBodyElement(): HTMLElement {
        return this.document.body;
    }

    getDocument(): Document {
        return this.document;
    }

    getContentBodyElement(): HTMLElement {
        this.initIFrame();
        return this.frameElement.contentWindow.document.body;
    }

    querySelector(selector: string): HTMLElement {
        return this.document.querySelector(selector);
    }

    querySelectorAll(selector: string): Array<HTMLElement> {
        return Array.from(this.document.querySelectorAll(selector));
    }

    sendMessageToIframe(message) {
        this.initIFrame();
        this.frameElement.contentWindow.postMessage(message, '*');
    }

    getContentElementById(id: string, variants?: boolean): HTMLElement {
        this.initIFrame(variants);
        return this.frameElement.contentWindow.document.getElementById(id);
    }

    getDesignerElementById(id: string): HTMLElement {
        return this.document.getElementById(id);
    }
    
    executeOnlyAfterInit(callback: () => void) {
        if (this.contentWasInit) {
            callback();
        }
        else {
            this.afterInitCallbacks.push(callback);
        }
    }

    addContentMessageListener(listener: IContentMessageListener) {
        if (this.contentMessageListeners.indexOf(listener) < 0) {
            this.contentMessageListeners.push(listener);
        }
    }

    removeContentMessageListener(listener: IContentMessageListener) {
        this.contentMessageListeners.splice(this.contentMessageListeners.indexOf(listener), 1);
    }

    getGlasspaneTopDistance(){
        this.initAdjustments();
        return this.topAdjust;
    }
    
    getGlasspaneLeftDistance(){
        this.initAdjustments();
        return this.leftAdjust;
    }
    
   private initIFrame(variants?: boolean) {
        if (variants) {
            if (!this.frameElement || this.frameElement.id != 'VariantsForm') {
                const frames = this.document.getElementsByTagName('iframe');
                if (frames[0] && frames[0].id === 'VariantsForm') {
                    this.frameElement = frames[0];
                } else if (frames.length > 0 ){//true when variants form is rendered
                    this.frameElement = frames[1];
                }   
            }
        } else {//!variants
            const frames = this.document.getElementsByTagName('iframe');
                if (frames[0] && frames[0].id != 'VariantsForm') {
                    this.frameElement = frames[0];
                } else if (frames.length > 0 ){//true when preview
                    this.frameElement = frames[1];
                }
        }
    }
    
    private initAdjustments(){
         if (!this.topAdjust) {
            const content = this.getContentArea();
            const computedStyle = this.windowRefService.nativeWindow.getComputedStyle(content, null)
            this.topAdjust = parseInt(computedStyle.getPropertyValue('padding-left').replace('px', ''));
            this.leftAdjust = parseInt(computedStyle.getPropertyValue('padding-top').replace('px', ''))
        }
    }
}

export interface IContentMessageListener {

    contentMessageReceived(id: string, data: { [key: string]: any }): void;

}