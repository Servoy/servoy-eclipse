import { Renderer2 } from '@angular/core';
import { EditorSessionService } from '../services/editorsession.service';
import { EditorContentService } from '../services/editorcontent.service';

export class HeightResizer {

    private lowestPart: Element | null = null;
    private active = false;
    private ghostContainers!: HTMLElement[];
    private editorContent!: HTMLElement;
    private contentArea!: HTMLElement;
    private glasspane!: HTMLElement;
    private lastPageY = 0;

    private heightOffset = 0;
    private currentPosition = 0;
    private heightLimit = 5;
    private ghostsBottom = 0;

    constructor(
        private readonly renderer: Renderer2,
        private readonly editorSession: EditorSessionService,
        private readonly editorContentService: EditorContentService
    ) { }

    start(event: MouseEvent) {
        this.active = true;
        this.lowestPart = null;
        this.heightLimit = 5;
        this.ghostsBottom = 0;

        this.contentArea = this.editorContentService.getContentArea();
        this.ghostContainers = this.editorContentService.querySelectorAll('.ghostcontainer');
        this.editorContent = this.editorContentService.querySelector('.content');
        this.glasspane = this.editorContentService.getGlassPane();

        this.currentPosition = this.ghostContainers[0].offsetHeight;
        this.lastPageY = event.pageY;

        let partHeight = 0;
        const ghostsList = this.editorContentService.getContentArea().getElementsByClassName('ghost label');
        for (let index = 0; index < ghostsList.length; index++) {
            const ghost = ghostsList.item(index)!;
            const ghostType = ghost.getAttribute('svy-ghosttype');
            if (ghostType == 'part') {
                if (!this.lowestPart) {
                    this.lowestPart = ghost;
                }
                partHeight = (ghost as HTMLAnchorElement).offsetHeight;
                const tmpTop = (ghost as HTMLElement).offsetTop;
                const ghostTop = (this.lowestPart as HTMLElement).offsetTop;
                if (tmpTop > ghostTop) {
                    this.heightLimit = (this.lowestPart as HTMLElement).offsetTop + 5;
                    this.lowestPart = ghost;
                }
            } else {
                this.ghostsBottom = Math.max(this.ghostsBottom, (ghost as HTMLElement).offsetTop + (ghost as HTMLElement).offsetHeight);
            }
        }
        this.heightOffset = this.ghostContainers[0].offsetTop + partHeight;
    }

    move(event: MouseEvent) {
        if (!this.active) return;
        const step = event.pageY - this.lastPageY;
        if (step != 0) {
            this.currentPosition += step;
            if (this.currentPosition >= this.heightLimit) {
                for (const ghostContainer of this.ghostContainers) {
                    this.renderer.setStyle(ghostContainer, 'height', this.currentPosition + 'px');
                }
                this.renderer.setStyle(this.editorContent, 'height', this.currentPosition + 'px');
                if (this.lowestPart) {
                    this.renderer.setStyle(this.lowestPart, 'top', this.currentPosition + 'px');
                }
                if (step > 0 && this.currentPosition + this.heightOffset > this.glasspane.offsetHeight) {
                    this.glasspane.style.height = this.currentPosition + this.heightOffset + 'px';
                }
            }
            this.lastPageY = event.pageY;
        }
    }

    end() {
        if (!this.active) return;
        this.active = false;
        if (this.currentPosition < this.heightLimit) {
            this.currentPosition = this.heightLimit;
        }
        if (this.lowestPart) {
            const changes: Record<string, any> = {};
            const id = this.lowestPart.getAttribute('svy-id');
            changes[id!] = { 'y': this.currentPosition };
            this.editorSession.sendChanges(changes);
        }

        const changes: Record<string, any> = {};
        const id = this.editorContentService.querySelector('.ghost[svy-ghosttype="form"]').getAttribute('svy-id')!;
        changes[id] = { 'height': this.currentPosition };
        this.editorSession.sendChanges(changes);

        const glasspaneHeight = Math.max((this.lowestPart ? this.currentPosition : this.ghostContainers[0].offsetHeight) + this.heightOffset, this.ghostsBottom);
        this.glasspane.style.height = glasspaneHeight + 'px';
    }

    updateLocation(changeY: number) {
        if (!this.active) return;
        if (this.currentPosition >= this.heightLimit) {
            for (const ghostContainer of this.ghostContainers) {
                this.renderer.setStyle(ghostContainer, 'height', this.currentPosition + 'px');
            }
            this.renderer.setStyle(this.editorContent, 'height', this.currentPosition + 'px');
            if (this.lowestPart) {
                this.renderer.setStyle(this.lowestPart, 'top', this.currentPosition + 'px');
            }

            if (this.currentPosition + this.heightOffset > this.glasspane.offsetHeight) {
                this.glasspane.style.height = this.currentPosition + this.heightOffset + 'px';
            }
            this.currentPosition += changeY;
            this.contentArea.scrollTop += changeY;
        }
    }
}
