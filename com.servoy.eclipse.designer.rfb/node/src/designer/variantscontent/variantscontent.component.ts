import { DOCUMENT } from '@angular/common';
import { Component, OnInit, Renderer2, ViewChild, ElementRef, Inject, AfterViewInit, HostListener, Input, Output, EventEmitter } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { URLParserService } from '../services/urlparser.service';
import { WindowRefService } from '@servoy/public';
import { EditorSessionService, PaletteComp } from '../services/editorsession.service';
import { EditorContentService } from '../services/editorcontent.service';
import { NgbPopover } from '@ng-bootstrap/ng-bootstrap';

@Component({
    selector: 'designer-variantscontent',
    templateUrl: './variantscontent.component.html',
    styleUrls: ['./variantscontent.component.css']
})
export class VariantsContentComponent implements OnInit {

    clientURL: SafeResourceUrl;
    popover: NgbPopover;
    component: PaletteComp;
    margin = 15;
    
    constructor(private sanitizer: DomSanitizer, private urlParser: URLParserService, protected readonly renderer: Renderer2,
        @Inject(DOCUMENT) private doc: Document, private windowRef: WindowRefService,
        private editorSession: EditorSessionService, private editorContentService: EditorContentService) {
	
		this.editorSession.openPopoverTriggered.subscribe((value) => {
			console.log('variantsContentComponent event received');
			console.log(value);
			this.previewVariantsForComponent(value.component);
		});
    }

    ngOnInit() {
        this.clientURL = this.sanitizer.bypassSecurityTrustResourceUrl('http://' + this.windowRef.nativeWindow.location.host + '/designer/solution/' + this.urlParser.getSolutionName() + '/form/PreviewForm/clientnr/' + this.urlParser.getContentClientNr() + '/index.html');
		this.windowRef.nativeWindow.addEventListener('message', (event) => {
            // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
        	if (event.data.id === 'previewReady') {
                console.log('Preview ready event received');
                this.sendVariantsToPreviewForm(this.component);
            }
            if (event.data.id === 'createVariantItem') {
				console.log('createVariantItem received');
	            const element = event.data.element;
	            console.log(element);
	 			this.editorSession.getState().dragging = true;
	 			/*if (this.popover.isOpen()) {
          			this.popover.close();
        		}*/
        		this.editorSession.getState().dragging = true;
            	this.editorContentService.sendMessageToIframe({ id: 'createElement', name: element.type, model: element.model, type: 'component', attributes: element.attributes, children: element.children });
			}
        });
    }
    
    onVariantClick(popover: NgbPopover, component: PaletteComp) {
		this.popover = popover
	}
	
	 /*doAddVariant(event: MouseEvent, component: PaletteComp) {
        event.stopPropagation();
        this.editorSession.addStyleVariantFor(component.styleVariantCategory);
    }

    doEditVariants(event: MouseEvent, component: PaletteComp) {
        event.stopPropagation();
        this.editorSession.editStyleVariantsFor(component.styleVariantCategory);
    }*/
	
    
    previewVariantsForComponent(component: PaletteComp) {
        if (this.popover.isOpen()) {
          	this.popover.close();
        } else {	  
	        this.editorSession.getStyleVariantFor(component.styleVariantCategory)
	            .then((result) => {
					component.styleVariants = result;
					this.component = component;
					const size = this.getPreviewFormSize(component) as {width: number, height: number};
					//TODO: set width & height for popover from computed sizes
					this.popover.open({ comp: component, popOv: this.popover, width: size.width + "px", height: size.height + "px", clientURL: this.clientURL});
				}).catch((err) => {
					console.log(err);
				});
        }    
	}
	
	
	sendVariantsToPreviewForm(component: PaletteComp) {
		console.log('sendVariantsToPreviewForm called');
		const columns = component.styleVariants.length > 8 ? 3 : component.styleVariants.length > 3 ? 2 : 1;
		const previewElement: HTMLIFrameElement = this.doc.getElementById('PreviewForm') as HTMLIFrameElement;
		const message = { 
			id: 'createVariants', 
			variants: component.styleVariants, 
			model: component.model, 
			name: this.convertToJSName(component.name), 
			type: 'component',
			margin: this.margin,
			columns: columns
		};
		previewElement.contentWindow.postMessage(message, '*');
		//this.editorContentService.sendMessageToPreview();
	}
	
	getPreviewFormSize(component: PaletteComp) {
		//TODO: render the size based on the real component and not the model
		const columns = component.styleVariants.length > 8 ? 3 : component.styleVariants.length > 3 ? 2 : 1;
		const size = (JSON.parse(JSON.stringify(component.model))['size']) as {width: number, height: number}; 
		const rows = Math.round(component.styleVariants.length / columns ) + 1;
		return {width: (columns) * (size.width + this.margin),
				height: rows * (size.height + this.margin)};
	}
				
	convertToJSName(name: string): string {
        // this should do the same as websocket.ts #scriptifyServiceNameIfNeeded() and ClientService.java #convertToJSName()
        if (name) {
            const packageAndName = name.split('-');
            if (packageAndName.length > 1) {
                name = packageAndName[0];
                for (let i = 1; i < packageAndName.length; i++) {
                    if (packageAndName[1].length > 0) name += packageAndName[i].charAt(0).toUpperCase() + packageAndName[i].slice(1);
                }
            }
        }
        return name;
        
    }
}
