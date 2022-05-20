import { DOCUMENT } from '@angular/common';
import { AfterViewInit, Component, ElementRef, HostListener, Inject, ChangeDetectorRef, Renderer2, ViewChild } from '@angular/core';
import { DesignerUtilsService } from '../services/designerutils.service';
import { EditorSessionService } from '../services/editorsession.service';

@Component({
    selector: 'designer-inline-edit',
    templateUrl: './inlineedit.component.html',
     styleUrls: ['./inlineedit.component.css']
})
export class InlineEditComponent implements AfterViewInit {

    @ViewChild('inlineedit', { static: true }) elementRef: ElementRef<HTMLElement>;
    showDirectEdit = false;
    node: string;
    directEditProperty: string;
    propertyValue: string;
    
    keyupListener: () => void;
    keydownListener: () => void;
    blurListener: () => void;
    lastTimestamp: number;
    
    constructor(protected readonly editorSession: EditorSessionService, private readonly designerUtilsService: DesignerUtilsService,
        @Inject(DOCUMENT) private doc: Document, protected readonly renderer: Renderer2, private readonly cdRef: ChangeDetectorRef) {
    }

    ngAfterViewInit(): void {
        // if time between mouseup and mousedown is too much, browser won't trigger click/dblclick event; so we have to fake a double click
        this.editorSession.registerCallback.next({ event: 'mouseup', function: (event: MouseEvent) => { 
                if (event.timeStamp - this.lastTimestamp < 350 && !this.editorSession.isInlineEditMode()){
                    this.enterInlineEdit(event);
                }
                this.lastTimestamp = event.timeStamp;
            } 
        });
    }

    enterInlineEdit(event: MouseEvent){
        const selection = this.editorSession.getSelection();
        if (selection && selection.length > 0) {
            let eventNode = this.designerUtilsService.getNode(this.doc, event);
            if (eventNode) {
                for (let i = 0; i < selection.length; i++) {
                    const node = selection[i];
                    if (eventNode.getAttribute("svy-id") === node) {
                        const directEditProperty = eventNode.getAttribute("directEditPropertyName");
                        if (directEditProperty) {
                            this.editorSession.getComponentPropertyWithTags(node, directEditProperty).then((propertyValue: string) => {
                                if (eventNode.clientHeight === 0 && eventNode.clientWidth === 0 && eventNode.firstElementChild) {
                                    eventNode = eventNode.firstElementChild;
                                }
                                const position = eventNode.getBoundingClientRect();
                                const absolutePoint = this.designerUtilsService.convertToAbsolutePoint(this.doc, {
                                    x: position.x,
                                    y: position.y
                                });
                                const newAbsolutePoint = {
                                    x: absolutePoint.x,
                                    y: absolutePoint.y,
                                    width: position.width,
                                    height: position.height
                                }
                                this.handleDirectEdit(node, newAbsolutePoint, directEditProperty, propertyValue);
                            }).catch(err => console.error(err));
                            break;
                        }
                    }
                }
            }
        }
    }
    
    applyValue(node: string, directEditProperty: string, propertyValue: string) {
        const changes = {};
        const newValue = this.elementRef.nativeElement.textContent;
        const oldValue = propertyValue;
        if (oldValue != newValue && !(oldValue === null && newValue === "")) {
            const value = {};
            value[directEditProperty] = newValue;
            changes[node] = value;
            this.editorSession.sendChanges(changes);
        } 
        this.showDirectEdit = false;
        this.cdRef.detectChanges();
        
        this.editorSession.setInlineEditMode(false);
    }
    
    addListeners(){
        if (!this.keyupListener) {
            this.keyupListener = this.renderer.listen(this.elementRef.nativeElement, 'keyup', (event: KeyboardEvent) => {
                if (event.key === 'Escape') {
                    this.showDirectEdit = false;
                    this.editorSession.setInlineEditMode(false);
                }
                if (event.key === 'Enter') {
                    this.applyValue(this.node, this.directEditProperty, this.propertyValue);
                }
                if (event.key == 'Delete') {
                    return false;
                }
            });
            this.keydownListener = this.renderer.listen(this.elementRef.nativeElement, 'keydown', (event: KeyboardEvent) => {
                if (event.key === 'Backspace') {
                    event.stopPropagation();
                }
                if (event.key === 'a' && event.ctrlKey) {
                    // TODO: find an alternative for execCommand
                    this.doc.execCommand('selectAll', false, null);
                }
                if (event.metaKey && (event.target as Element).className == 'inlineEdit' && (event.key === 'x' || event.key === 'X')) {//cut action for mac: see the case SVY-17017
                    //this code is executing only on mac (event.metaKey)
                    let selectedObj = null;
                    if (window.getSelection) {
                        selectedObj = window.getSelection();
                    } else if (document.getSelection) {
                        selectedObj = document.getSelection();
                    } 
                    if (selectedObj != null) {
                        let nodeText = selectedObj.anchorNode.nodeValue;
                        //in this context anchorNode and focusNode are the same
                        let startRange = Math.min(selectedObj.anchorOffset, selectedObj.focusOffset);
                        let endRange = Math.max(selectedObj.anchorOffset, selectedObj.focusOffset);
                        if (startRange != endRange) { //something is selected; 
                            document.execCommand('copy'); //deprecated but navigator.clipboard.writeText is not working
                            var selectedText = nodeText.substring(startRange, endRange);
                            document.activeElement.textContent = nodeText.replace(selectedText, '');
                        }
                    }
                    return false; //do not dispatch the event further
                }
            });
            this.blurListener = this.renderer.listen(this.elementRef.nativeElement, 'blur', (event: Event) => {
                this.applyValue(this.node, this.directEditProperty, this.propertyValue);
            });
        }
    }
    
    handleDirectEdit(node: string, absolutePoint: { x: number; y: number; width: number, height: number }, directEditProperty: string, propertyValue: string) {
        this.showDirectEdit = true;
        
        this.addListeners();
        this.node = node;
        this.directEditProperty = directEditProperty;
        this.propertyValue = propertyValue;
        
        this.editorSession.setInlineEditMode(true);
        this.renderer.setProperty(this.elementRef.nativeElement, 'innerHTML', propertyValue);
        this.renderer.setStyle(this.elementRef.nativeElement, 'left', absolutePoint.x+'px');
        this.renderer.setStyle(this.elementRef.nativeElement, 'top', absolutePoint.y + 'px');
        this.renderer.setStyle(this.elementRef.nativeElement, 'width', absolutePoint.width + 'px');
        this.renderer.setStyle(this.elementRef.nativeElement, 'height', absolutePoint.height + 'px');

        setTimeout(() => this.elementRef.nativeElement.focus());
    }

}

