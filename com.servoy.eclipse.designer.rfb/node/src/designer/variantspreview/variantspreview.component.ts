import { DOCUMENT } from '@angular/common';
import { Component, OnInit, Renderer2, ViewChild, ElementRef, Inject, AfterViewInit, HostListener, Input, Output, EventEmitter } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { URLParserService } from '../services/urlparser.service';
import { WindowRefService } from '@servoy/public';
import { EditorSessionService, PaletteComp } from '../services/editorsession.service';
import { EditorContentService } from '../services/editorcontent.service';
import { NgbPopover } from '@ng-bootstrap/ng-bootstrap';
import { NgbPopoverConfig } from '@ng-bootstrap/ng-bootstrap';

@Component({
    selector: 'designer-variantspreview',
    templateUrl: './variantspreview.component.html',
    styleUrls: ['./variantspreview.component.css']
})
export class VariantsPreviewComponent implements AfterViewInit {

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
	top = 0;
	left = 0;
	placement = 'right';
	isPopoverClosed = true;
	isPopoverVisible = false
	popoverFooterHeight = 0;
    
    constructor(private sanitizer: DomSanitizer, private urlParser: URLParserService, protected readonly renderer: Renderer2,
        @Inject(DOCUMENT) private doc: Document, private windowRef: WindowRefService, private popoverCfgRef: NgbPopoverConfig,
        private editorSession: EditorSessionService) {
	
		this.editorSession.variantsTrigger.subscribe((value) => {
			if (value.show) {
				if (this.isPopoverClosed) {
					this.isPopoverClosed = false;
					this.popover.open({ popOv: this.popover, clientURL: this.clientURL});
				}
				this.top = value.top;
				this.left = value.left;
			} else {
				this.hidePopover();
			}
			
		});

		popoverCfgRef.autoClose = false;
		popoverCfgRef.triggers = 'manual';
    }

    ngAfterViewInit() {
        this.clientURL = this.sanitizer.bypassSecurityTrustResourceUrl('http://' + this.windowRef.nativeWindow.location.host + '/designer/solution/' + this.urlParser.getSolutionName() + '/form/VariantsForm/clientnr/' + this.urlParser.getContentClientNr() + '/index.html');
		this.windowRef.nativeWindow.addEventListener('message', (event) => {
            // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
			if (event.data.id === 'resizePopover') {
                this.setPopoverSizeAndPosition(this.top, this.left, event.data.popoverWidth, event.data.popoverHeight);
				this.showPopover();
            } else if (event.data.id === 'onVariantMouseDown') {
				this.onVariantMouseDown(event.data.pageX, event.data.pageY);
			} else if (event.data.id === 'variantsReady') {
				this.variantsIFrame = this.doc.getElementById('VariantsForm') as HTMLIFrameElement;
				this.variantsIFrame.contentWindow.document.body.addEventListener('mouseup', this.onMouseUp);
        		this.variantsIFrame.contentWindow.document.body.addEventListener('mousemove', this.onMouseMove);
			}
        });
    }

	hidePopover() {
		this.isPopoverVisible = false;

		let popoverCtrl = this.doc.getElementById('VariantsCtrl') as HTMLElement;
		popoverCtrl.style.display = 'none'

		let popover = this.doc.getElementsByClassName('popover-body').item(0) as HTMLElement;
		popover.style.display = 'none';

		let popoverArrow = this.doc.getElementsByClassName('popover-arrow').item(0) as HTMLElement;
		popoverArrow.style.display = 'none';

		//move popover to "parking" position
	}

	showPopover() {
		this.isPopoverVisible = true;

		let popoverCtrl = this.doc.getElementById('VariantsCtrl') as HTMLElement;
		popoverCtrl.style.display = 'block'

		let popover = this.doc.getElementsByClassName('popover-body').item(0) as HTMLElement;
		popover.style.display = 'block';

		let popoverArrow = this.doc.getElementsByClassName('popover-arrow').item(0) as HTMLElement;
		popoverArrow.style.display = 'block';		
	}

	setPopoverSizeAndPosition(top, left, width, height: number) {

		//set popover size
		let popover = this.doc.getElementsByClassName('popover-body').item(0) as HTMLElement;
		let footer = popover.getElementsByClassName('modal-footer').item(0) as HTMLElement;
		let body = popover.getElementsByClassName('modal-body').item(0) as HTMLElement;

		//very first display of the footer contain the footer's height
		//hiding / showing popup -> footer's height is zero (ng-popup bug?) and overall height is no longer correct
		//save hight on the first display and use it further
		if (this.popoverFooterHeight == 0 && footer.clientHeight != 0) {
			this.popoverFooterHeight = footer.clientHeight
		}
 
		popover.style.width = width + 2 * this.margin + 'px';
		popover.style.height = height + 2 * this.margin + Math.floor(this.margin / 2) + this.popoverFooterHeight + 'px';
		
		footer.style.width = width + 'px';
		body.style.width = width + 'px';
		body.style.height = height + this.margin + 'px';

		//set popover position
		let popoverCtrl = this.doc.getElementById('VariantsCtrl') as HTMLElement;
		popoverCtrl.style.top = top + 'px';
		popoverCtrl.style.left = left + 'px';
	}

	onVariantsClick() {
		this.hidePopover();
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
		if (this.variantItemBeingDragged && this.isPopoverVisible) {
			this.variantsIFrame.contentWindow.document.body.removeChild(this.variantItemBeingDragged);
			this.variantItemBeingDragged = null;
		 	this.hidePopover();
		}
	}
}