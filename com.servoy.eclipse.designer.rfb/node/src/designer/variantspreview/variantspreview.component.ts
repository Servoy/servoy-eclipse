import { Component, Renderer2, ViewChild, AfterViewInit, Input, ViewEncapsulation, ElementRef } from '@angular/core';
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
    styleUrls: ['./variantspreview.component.css'],
	encapsulation: ViewEncapsulation.Emulated
})
export class VariantsPreviewComponent implements AfterViewInit {

    @Input() component: PaletteComp;
    @ViewChild('popover') popover: NgbPopover;
	@ViewChild('variantGlasspane') glasspane: ElementRef; 
	@ViewChild('variantContent') content: ElementRef;

    clientURL: SafeResourceUrl;
    margin = 16; //ng-popover margin
	variantItemBeingDragged: Node;
	variantsIFrame: HTMLIFrameElement;
	top = 0;
	left = 0;
	placement = 'right';
	isPopoverClosed = true;
	isPopoverVisible = false
	popoverFooterHeight = 0;
	document: Document;
	
    constructor(private sanitizer: DomSanitizer, private urlParser: URLParserService, protected readonly renderer: Renderer2,
        private windowRef: WindowRefService, private popoverCfgRef: NgbPopoverConfig, private editorSession: EditorSessionService,
		private editorContentService: EditorContentService) {
	
		this.editorSession.variantsTrigger.subscribe((value) => {
			if (value.show) {
				this.top = value.top;
				this.left = value.left;
				if (this.isPopoverClosed) {
					this.isPopoverClosed = false;
					this.popover.open({ popOv: this.popover, clientURL: this.clientURL});
					//we need to switch focus on the inner form else first click on scroll bar will close the popup
				}
			} else {
				this.hidePopover();
			}
		});

		this.editorSession.variantsScroll.subscribe((value) => {
			if (!this.isPopoverClosed) {
				let popoverCtrl = this.document.getElementById('VariantsCtrl') as HTMLElement;
				popoverCtrl.style.top = this.top - value.scrollPos + 'px';
			}
		});

		this.document = this.editorContentService.getDocument();

		popoverCfgRef.autoClose = false;
		popoverCfgRef.triggers = 'manual';
    }

    ngAfterViewInit() {
        this.clientURL = this.sanitizer.bypassSecurityTrustResourceUrl('http://' + this.windowRef.nativeWindow.location.host + '/designer/solution/' + this.urlParser.getSolutionName() + '/form/VariantsForm/clientnr/' + this.urlParser.getContentClientNr() + '/index.html');
		this.windowRef.nativeWindow.addEventListener('message', (event) => {
            // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
			if (event.data.id === 'resizePopover') {
                this.setPopoverSizeAndPosition(event.data.popoverWidth, event.data.popoverHeight, event.data.formWidth, event.data.formHeight);
				this.showPopover();
            } else if (event.data.id === 'onVariantMouseDown') {
				this.onVariantMouseDown(event.data.pageX, event.data.pageY);
			} else if (event.data.id === 'variantsReady') {
				this.variantsIFrame = this.document.getElementById('VariantsForm') as HTMLIFrameElement;
				this.variantsIFrame.contentWindow.document.body.addEventListener('mouseup', this.onMouseUp);
        		this.variantsIFrame.contentWindow.document.body.addEventListener('mousemove', this.onMouseMove);
			}
        });
    }

	hidePopover() {
		this.isPopoverVisible = false;

		let popoverCtrl = this.document.getElementById('VariantsCtrl') as HTMLElement;
		popoverCtrl.style.display = 'none'

		let popover = this.document.getElementsByClassName('popover-body').item(0) as HTMLElement;
		popover.style.display = 'none';

		let popoverArrow = this.document.getElementsByClassName('popover-arrow').item(0) as HTMLElement;
		popoverArrow.style.display = 'none';	
	}

	showPopover() {
		this.isPopoverVisible = true;

		let popoverCtrl = this.document.getElementById('VariantsCtrl') as HTMLElement;
		popoverCtrl.style.display = 'block'

		let popover = this.document.getElementsByClassName('popover-body').item(0) as HTMLElement;
		popover.style.display = 'block';

		let popoverArrow = this.document.getElementsByClassName('popover-arrow').item(0) as HTMLElement;
		popoverArrow.style.display = 'block';		
	}

	setPopoverSizeAndPosition(width, height: number, formWidth: number, formHeight: number) {
		//set popover size
		let popover = this.document.getElementsByClassName('popover-body').item(0) as HTMLElement;
		let footer = popover.getElementsByClassName('modal-footer').item(0) as HTMLElement;
		let body = popover.getElementsByClassName('modal-body').item(0) as HTMLElement;
		let scrollbarWidth = 0;

		let palette = this.editorContentService.getPallete();
		if (formHeight > height) {
			scrollbarWidth = palette.offsetWidth - palette.scrollWidth + 1; //all developer scroll width are the same
		}

		//very first display of the footer contain the footer's height
		//hiding / showing popup -> footer's height is zero (ng-popup bug?) and overall height is no longer correct
		//save hight on the first display and use it further
		if (this.popoverFooterHeight == 0 && footer.clientHeight != 0) {
			this.popoverFooterHeight = footer.clientHeight
		}
 
		popover.style.width = (width + scrollbarWidth + 2 * this.margin) + 'px';
		popover.style.height = height + 2 * this.margin + Math.floor(this.margin / 2) + this.popoverFooterHeight + 'px';
		
		footer.style.width = width + 'px';
		body.style.width = width + 'px';
		body.style.height = height + this.margin + 'px';
		

		//set popover position
		let popoverCtrl = this.document.getElementById('VariantsCtrl') as HTMLElement;
		popoverCtrl.style.top = this.top - palette.scrollTop + 'px';
		popoverCtrl.style.left = this.left + 'px';

		let contentArea = this.document.getElementsByClassName('variant-content-area').item(0) as HTMLElement;
		let contentOverlay = this.document.getElementsByClassName('variant-content-overlay').item(0) as HTMLElement;
		contentArea.style.width = (width + scrollbarWidth) + 'px';
		contentArea.style.height = height + 'px';
		contentOverlay.style.width= formWidth + 'px';
		contentOverlay.style.height = formHeight + 'px';

		contentArea.scrollTop = 0;
	}

	onVariantsClick() {
		this.hidePopover();
	}

	onAddVariant(event: MouseEvent) {
		//TODO: add iplementation
		event.stopPropagation();
		console.log('Add variant - not implemented');
	}

	onEditVariant(event: MouseEvent) {
		//TODO: add implementation
		event.stopPropagation();
		console.log('Edit variant - not implemented');
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