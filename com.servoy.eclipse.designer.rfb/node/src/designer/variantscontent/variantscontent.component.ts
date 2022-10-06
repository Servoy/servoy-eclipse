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
    margin = 16; //ng-popover margin
	rowInterspace = 10;
	columnInterspace = 20;
	maxFormSize = {width: 200, height: 300}; //these dimentions doesn't consider margins and space between columns
	variantItemBeingDragged: Node;
	variantsIFrame: HTMLIFrameElement;
	size: {width: number, height: number};
	activeVariant = false;
	keepOneColumn = false;

    
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
				if (event.data.id === 'resizePopover') {
                    this.resizePopover(event.data.popoverWidth, event.data.popoverHeight);
                }
            }
        });
    }

	resizePopover(width, height: number) {
		let element = this.doc.getElementsByClassName('popover-body').item(0) as HTMLElement;
		let footer = element.getElementsByClassName('modal-footer').item(0) as HTMLElement;
		let body = element.getElementsByClassName('modal-body').item(0) as HTMLElement;

		element.style.width = width + 2 * this.margin + 'px';
		element.style.height = height + 2 * this.margin + Math.floor(this.margin / 2) + footer.clientHeight + 'px';
		
		footer.style.width = width + 'px';
		body.style.width = width + 'px';
		body.style.height = height + this.margin + 'px';
	}
    
    previewStylesForComponent() {
        if (this.popover.isOpen()) {
          	this.popover.close(true);
        } else {	  
			//this.size = this.getVariantsFormSize() as {width: number, height: number};
			//TODO: set width & height for popover from computed sizes
			// this.popover.open({ comp: this.component, popOv: this.popover, width: this.size.width + "px", height: this.size.height + "px", clientURL: this.clientURL});
			this.popover.open({ comp: this.component, popOv: this.popover, clientURL: this.clientURL});
        }    
	}
	
	
	sendStylesToVariantsForm() {
		this.variantsIFrame = this.doc.getElementById('VariantsForm') as HTMLIFrameElement;
		const message = { 
			id: 'createVariants', 
			variants: this.component.styleVariants, 
			model: this.component.model, 
			name: this.convertToJSName(this.component.name), 
			type: 'component',
			rowInterspace: this.rowInterspace,
			columnInterspace: this.columnInterspace,
			maxFormSize: this.maxFormSize
		};
		this.variantsIFrame.contentWindow.postMessage(message, '*');

		this.variantsIFrame.contentWindow.document.body.addEventListener('mouseup', this.onMouseUp);
        this.variantsIFrame.contentWindow.document.body.addEventListener('mousemove', this.onMouseMove);

	}
	
	//in the future we need to resize the form AFTER the variants was rendered
	// we need to be able to display variants based on diferent scenarios.
	// The default scenario (below) is using two columns. If there is no room to display
	// all variants in the maximum form size then everything goes back to one column
	// getVariantsFormSize() {
	// 	let columnsWidth = [0,0]; //can we have more than three columns in variants form?
	// 	let columnHeight = [0,0];
	// 	let columnIndex = 0;
	// 	this.component.styleVariants.forEach(variant => {
	// 		columnsWidth[columnIndex] = (variant.size.width > columnsWidth[columnIndex]) ? variant.size.width : columnsWidth[columnIndex];
	// 		if (columnsWidth.reduce((a, b) => a + b, 0) > this.maxFormSize.height) {//at any moment if max form width can't handle the variants width as divided into columns keep one colums
	// 			columnsWidth[0] = Math.max(columnsWidth[0], Math.max(columnsWidth[1], columnsWidth[2]));
	// 			this.keepOneColumn = true;
	// 		}
	// 		columnHeight[columnIndex] += variant.size.height;
	// 		if (!this.keepOneColumn && columnHeight[columnIndex] > this.maxFormSize.height) {
	// 			if (columnIndex < 2) {
	// 				columnIndex++
	// 			} else {//form height is not enough so get back to one columns to avoid design inconsistencies
	// 				columnsWidth[0] = Math.max(columnsWidth[0], columnsWidth[1]);
	// 				columnsWidth[1] = 0;
	// 				this.keepOneColumn = true;
	// 				columnIndex = 1;
	// 			}
	// 		}
	// 		columnHeight[columnIndex] += this.rowInterspace;
	// 	});
	// 	let maxVariantWidth = columnsWidth.reduce((a,b) => a+b, 0) + 2 * this.margin;
	// 	maxVariantWidth += (columnIndex - 1) * this.columnInterspace;
	// 	let maxFormWidth = (this.keepOneColumn) ? this.maxFormSize.height : Math.max(columnHeight[0], columnHeight[1]);
	// 	return {
	// 		width: maxVariantWidth,
	// 		height: maxFormWidth
	// 	};
	// }
				
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