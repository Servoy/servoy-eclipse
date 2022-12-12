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
	minPopupWidth = 100;//pixels
	maxPopupWidth = 30; //percent
	maxPopupHeight = 300;//pixels
	hidePopupSize = '100px';
	
    constructor(private sanitizer: DomSanitizer, private urlParser: URLParserService, protected readonly renderer: Renderer2,
        private windowRef: WindowRefService, private popoverCfgRef: NgbPopoverConfig, private editorSession: EditorSessionService,
		private editorContentService: EditorContentService) {
	
		this.editorSession.variantsTrigger.subscribe((value) => {
			if (value.show) {
				this.top = value.top;
				this.left = value.left;
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
                this.setPopoverSizeAndPosition(event.data.formWidth, event.data.formHeight);
				this.showPopover();
            } else if (event.data.id === 'onVariantMouseDown') {
				this.onVariantMouseDown(event.data.pageX, event.data.pageY);
			} else if (event.data.id === 'variantsReady') {
				this.variantsIFrame = this.document.getElementById('VariantsForm') as HTMLIFrameElement;
				this.variantsIFrame.contentWindow.document.body.addEventListener('mouseup', this.onMouseUp);
        		this.variantsIFrame.contentWindow.document.body.addEventListener('mousemove', this.onMouseMove);
			}
        });
		if (this.isPopoverClosed) {
			this.isPopoverClosed = false;
			this.popover.open({ popOv: this.popover, clientURL: this.clientURL});
			this.hidePopover();
		}
    }

	hidePopover() {
		this.editorSession.variantsPopup.emit({status: 'hidden'});
		if (this.isPopoverVisible) {
			this.isPopoverVisible = false;

			let popoverCtrl = this.document.getElementById('VariantsCtrl') as HTMLElement;
			let popover = this.document.getElementsByClassName('popover-body').item(0) as HTMLElement;

			//TODO: set variable popover height (maximum height is limited by the palette)
			//TODO: set position for popoverArrow relative to popover with dinamic size
			//let popoverArrow = this.document.getElementsByClassName('popover-arrow').item(0) as HTMLElement;

			//set popover position
			//let footer = popover.getElementsByClassName('modal-footer').item(0) as HTMLElement;
			let body = popover.getElementsByClassName('modal-body').item(0) as HTMLElement;

			popover.style.width = this.hidePopupSize;
			popover.style.height = this.hidePopupSize;
		
			//footer.style.width = this.hidePopupSize;
			body.style.width = this.hidePopupSize;
			body.style.height = this.hidePopupSize;

			popoverCtrl.style.top = -1000 + 'px';
			popoverCtrl.style.left = -1000 + 'px';
		}	
	}

	showPopover() {
		this.editorSession.variantsPopup.emit({status: 'visible'});
		if (!this.isPopoverVisible) {
			this.isPopoverVisible = true;

			let popoverCtrl = this.document.getElementById('VariantsCtrl') as HTMLElement;
			popoverCtrl.style.display = 'block'

			let popover = this.document.getElementsByClassName('popover-body').item(0) as HTMLElement;
			popover.style.display = 'block';

			let popoverArrow = this.document.getElementsByClassName('popover-arrow').item(0) as HTMLElement;
			popoverArrow.style.display = 'block';
		}
	}

	setPopoverSizeAndPosition(formWidth: number, formHeight: number) {

		//get max popup width
		const popoverWidth = Math.round(this.maxPopupWidth * (this.editorContentService.getPallete().offsetWidth + this.editorContentService.getContentArea().offsetWidth) / 100.0);

		//set popover size
		let popover = this.document.getElementsByClassName('popover-body').item(0) as HTMLElement;
		// let footer = popover.getElementsByClassName('modal-footer').item(0) as HTMLElement;
		let body = popover.getElementsByClassName('modal-body').item(0) as HTMLElement;
		let scrollbarWidth = 0;

		const width = Math.max(this.minPopupWidth, formWidth);
		const height = Math.min(formHeight, this.maxPopupHeight);

		let palette = this.editorContentService.getPallete();
		if (formHeight > height) {
			scrollbarWidth = palette.offsetWidth - palette.scrollWidth + 1; //all developer scroll width are the same
		}

		//very first display of the footer contain the footer's height
		//hiding / showing popup -> footer's height is zero (ng-popup bug?) and overall height is no longer correct
		//save hight on the first display and use it further
		// if (this.popoverFooterHeight == 0 && footer.clientHeight != 0) {
		// 	this.popoverFooterHeight = footer.clientHeight
		// }
 
		popover.style.width = (width + scrollbarWidth + 2 * this.margin) + 'px';
		popover.style.height = height + 2 * this.margin + Math.floor(this.margin / 2) + this.popoverFooterHeight + 'px';
		
		//footer.style.width = width + 'px';
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
		contentOverlay.style.width= popoverWidth + 'px';
		contentOverlay.style.height = formHeight + 'px';
		contentArea.scrollTop = 0;
	}

	onVariantsClick() {
		this.hidePopover();
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

	onAreaMouseUp(event: MouseEvent) {
		//avoid palette receiving this event (and trigger a popup close) when click inside variants area
		event.stopPropagation();
	}
}