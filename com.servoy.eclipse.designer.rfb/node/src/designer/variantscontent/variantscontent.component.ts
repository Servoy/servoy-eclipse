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

    @Input() component: PaletteComp;
    @ViewChild('popover') popover: NgbPopover;

    clientURL: SafeResourceUrl;
    margin = 15;
	variantItemBeingDragged: Node;
	variantsIFrame: HTMLIFrameElement;
	size: {width: number, height: number};
	activeVariant = false;
    
    constructor(private sanitizer: DomSanitizer, private urlParser: URLParserService, protected readonly renderer: Renderer2,
        @Inject(DOCUMENT) private doc: Document, private windowRef: WindowRefService,
        private editorSession: EditorSessionService, private editorContentService: EditorContentService) {
	
		this.editorSession.openPopoverTrigger.subscribe((value) => {
			if (this.component == value.component) {
			     this.activeVariant = true;
			     this.previewStylesForComponent();
			 }
			 else{
			     this.activeVariant = false;
			 }
		});
    }

    ngOnInit() {
        this.clientURL = this.sanitizer.bypassSecurityTrustResourceUrl('http://' + this.windowRef.nativeWindow.location.host + '/designer/solution/' + this.urlParser.getSolutionName() + '/form/VariantsForm/clientnr/' + this.urlParser.getContentClientNr() + '/index.html');
		this.windowRef.nativeWindow.addEventListener('message', (event) => {
            // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
            if ( this.activeVariant)
            {
                if (event.data.id === 'variantsReady') {
                    this.sendStylesToVariantsForm();
                }
                if (event.data.id === 'onVariantMouseDown') {
                    this.onVariantMouseDown(event.data.pageX, event.data.pageY);
                }
            }
        });
    }
    
    previewStylesForComponent() {
        if (this.popover.isOpen()) {
          	this.popover.close(true);
        } else {	  
			this.size = this.getVariantsFormSize() as {width: number, height: number};
			//TODO: set width & height for popover from computed sizes
			this.popover.open({ comp: this.component, popOv: this.popover, width: this.size.width + "px", height: this.size.height + "px", clientURL: this.clientURL});
        }    
	}
	
	
	sendStylesToVariantsForm() {
		//variants form is already created, now waiting styles
		const columns = this.component.styleVariants.length > 8 ? 3 : this.component.styleVariants.length > 3 ? 2 : 1;
		this.variantsIFrame = this.doc.getElementById('VariantsForm') as HTMLIFrameElement;
		const message = { 
			id: 'createVariants', 
			variants: this.component.styleVariants, 
			model: this.component.model, 
			name: this.convertToJSName(this.component.name), 
			type: 'component',
			margin: this.margin,
			columns: columns
		};
		this.variantsIFrame.contentWindow.postMessage(message, '*');

		this.variantsIFrame.contentWindow.document.body.addEventListener('mouseup', this.onMouseUp);
        this.variantsIFrame.contentWindow.document.body.addEventListener('mousemove', this.onMouseMove);

	}
	
	getVariantsFormSize() {
		//TODO: render the size based on the real component and not the model
		const columns = this.component.styleVariants.length > 8 ? 3 : this.component.styleVariants.length > 3 ? 2 : 1;
		const size = (JSON.parse(JSON.stringify(this.component.model))['size']) as {width: number, height: number}; 
		const rows = Math.round(this.component.styleVariants.length / columns ) + 1;
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

	onAddVariant(event: MouseEvent) {
		//TODO: add iplementation
		event.stopPropagation();
		console.log('Add variant clicked');
	}

	onEditVariant(event: MouseEvent) {
		//TODO: add implementation
		event.stopPropagation();
		console.log('Edit variant clicked');
	}

	onVariantMouseDown = (pageX, pageY: number) => {
		this.variantItemBeingDragged = this.variantsIFrame.contentWindow.document.elementFromPoint(pageX, pageY).cloneNode(true) as Element;
		this.renderer.setStyle(this.variantItemBeingDragged, 'left',pageX + 'px');
        this.renderer.setStyle(this.variantItemBeingDragged, 'top', pageY + 'px');
        this.renderer.setStyle(this.variantItemBeingDragged, 'position', 'absolute');
		this.renderer.setAttribute(this.variantItemBeingDragged, 'id', 'svy_variantelement');
        this.variantsIFrame.contentWindow.document.body.appendChild(this.variantItemBeingDragged);
	}

	onMouseUp = (event: MouseEvent) => {
		event.stopPropagation();
		if (this.variantItemBeingDragged) {
			this.variantsIFrame.contentWindow.document.body.removeChild(this.variantItemBeingDragged);
			this.variantItemBeingDragged = null;
			this.windowRef.nativeWindow.postMessage({ id: 'onVariantMouseUp'});
		}
	}

	onMouseMove = (event: MouseEvent) => {
		if (this.variantItemBeingDragged && this.popover.isOpen()) {
			this.variantItemBeingDragged = null;
			this.popover.close(true);
		}
	}
}