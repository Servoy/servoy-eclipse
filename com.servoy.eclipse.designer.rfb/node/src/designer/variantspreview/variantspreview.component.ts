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
	variantItemBeingDisplayed: Node;
	variantsIFrame: HTMLIFrameElement;
	top = -1000;
	left = -1000;
	placement = 'right right-top right-bottom top-left bottom-left';
	isPopoverInitialized = false;
	popoverFooterHeight = 0;
	document: Document;
	minPopupWidth = 100;
	maxPopupWidth = 240;
	maxPopupHeight = 300;
    popupParkingPosition = '-10000px';
    scrollbarwidth = 32;
	showVariantPopup = false;
	
    constructor(private sanitizer: DomSanitizer, private urlParser: URLParserService, protected readonly renderer: Renderer2,
        private windowRef: WindowRefService, private popoverCfgRef: NgbPopoverConfig, private editorSession: EditorSessionService,
		private editorContentService: EditorContentService) {
	
		this.editorSession.variantsTrigger.subscribe((value) => {
			if (value.show == true) {
				this.top = value.top;
				this.left = value.left;
				this.showPopover();
			} else if (value.show == false) {
				this.hidePopover();
			}
		});

		this.editorSession.variantsScroll.subscribe((value) => {
			if (this.isPopoverInitialized) {
				const popoverCtrl = this.document.getElementById('VariantsCtrl');
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
                // eslint-disable-next-line @typescript-eslint/no-unsafe-argument, @typescript-eslint/no-unsafe-member-access
                this.setPopoverSizeAndPosition(event.data.formWidth, event.data.formHeight);
                
				this.showPopover();
            // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
            } else if (event.data.id === 'onVariantMouseDown') {
				// eslint-disable-next-line @typescript-eslint/no-unsafe-argument, @typescript-eslint/no-unsafe-member-access
				this.onVariantMouseDown(event.data.pageX, event.data.pageY);
			// eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
			} else if (event.data.id === 'variantsReady') {
				this.variantsIFrame = this.document.getElementById('VariantsForm') as HTMLIFrameElement;
				this.variantsIFrame.contentWindow.document.body.addEventListener('mouseup', this.onMouseUp);
                this.variantsIFrame.contentWindow.document.body.addEventListener('mousemove', this.onMouseMove);
			} else if (event.data.id === 'variantsEscapePressed') {
				this.editorSession.variantsTrigger.emit({ show: false });
			}
        });
        if (!this.isPopoverInitialized) {
			this.isPopoverInitialized = true;
			this.initPopover();
		}
    }

    initPopover() {
        this.popover.open({ popOv: this.popover, clientURL: this.clientURL});
        this.top = -1000;
        this.left = -1000;
        //need to create the form prior to correctly rendering variants in designer
        this.setPopoverSizeAndPosition(100, 100);
		this.hidePopover();

    }

	hidePopover() {
		this.editorSession.variantsPopup.emit({status: 'hidden'});
        this.variantsIFrame.style.display = 'none';
        const popoverCtrl = this.document.getElementById('VariantsCtrl');
        popoverCtrl.style.top = this.popupParkingPosition;
        popoverCtrl.style.left = this.popupParkingPosition;	
	}

	showPopover() {
		this.editorSession.variantsPopup.emit({status: 'visible'});
		this.variantsIFrame.style.display = 'block';
	}

	setPopoverSizeAndPosition(formWidth: number, formHeight: number) {

		//set popover size
		const popover = this.document.getElementsByClassName('popover-body').item(0) as HTMLElement;
		// let footer = popover.getElementsByClassName('modal-footer').item(0) as HTMLElement;
		const body = popover.getElementsByClassName('modal-body').item(0) as HTMLElement;

		const width = Math.max(this.minPopupWidth, formWidth);
		const height = Math.min(formHeight, this.maxPopupHeight);

		const palette = this.editorContentService.getPallete();
        let scrollbarWidth = 0;
		if (formHeight > height) {
			scrollbarWidth = 30;
		}
 
		popover.style.width = (width + scrollbarWidth + 2 * this.margin) + 'px';
		popover.style.height = height + 2 * this.margin + Math.floor(this.margin / 2) + this.popoverFooterHeight + 'px';
		
		body.style.width = width + 'px';
		body.style.height = height + this.margin + 'px';

		//set popover position
		const popoverCtrl = this.document.getElementById('VariantsCtrl');
		popoverCtrl.style.top = this.top - palette.scrollTop + 'px';
		popoverCtrl.style.left = this.left + 'px';
        popoverCtrl.style.display = 'inline-block';

		const contentArea = this.document.getElementsByClassName('variant-content-area').item(0) as HTMLElement;
		const contentOverlay = this.document.getElementsByClassName('variant-content-overlay').item(0) as HTMLElement;
		contentArea.style.width = (width + scrollbarWidth) + 'px';
		contentArea.style.height = height + 'px';
		contentOverlay.style.width= this.maxPopupWidth + 'px';
		contentOverlay.style.height = formHeight + 'px';
		contentArea.scrollTop = 0;
	}

	onVariantsClick = () => {
		this.showPopover();
	}

	onVariantMouseDown = (pageX: number, pageY: number) => {
		this.variantItemBeingDragged = this.variantsIFrame.contentWindow.document.elementFromPoint(pageX, pageY)?.cloneNode(true) as Element;
		this.variantItemBeingDisplayed = this.variantsIFrame.contentWindow.document.elementFromPoint(pageX, pageY)?.parentNode.cloneNode(true) as Element;
		this.renderer.setAttribute(this.variantItemBeingDragged, 'id', 'svy_variantelement');

		const applyStyles = (element: Element) => {
			this.renderer.setStyle(element, 'left', `${pageX}px`);
			this.renderer.setStyle(element, 'top', `${pageY}px`);
			this.renderer.setStyle(element, 'position', 'absolute');
		};
		
        applyStyles(this.variantItemBeingDragged as Element);
		applyStyles(this.variantItemBeingDisplayed as Element);
		this.variantsIFrame.contentWindow.document.body.appendChild(this.variantItemBeingDisplayed);
	}

	onMouseUp = (event: MouseEvent) => {
		event.stopPropagation();
		if (this.variantItemBeingDisplayed) {
			this.variantsIFrame.contentWindow.document.body.removeChild(this.variantItemBeingDisplayed);
			this.windowRef.nativeWindow.postMessage({ id: 'onVariantMouseUp'});
		}
		this.variantItemBeingDragged = null;
		this.variantItemBeingDisplayed = null;
	}

	onMouseMove = () => {
		if (this.variantItemBeingDisplayed) {
			this.variantsIFrame.contentWindow.document.body.removeChild(this.variantItemBeingDisplayed);
			this.variantItemBeingDragged = null;
			this.variantItemBeingDisplayed = null;
			this.hidePopover();
		}
	}

	onAreaMouseUp = (event: MouseEvent) => {
		//avoid palette receiving this event (and trigger a popup close) when click inside variants area
		event.stopPropagation();
	}
}