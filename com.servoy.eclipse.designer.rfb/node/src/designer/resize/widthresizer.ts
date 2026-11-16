import { Renderer2 } from '@angular/core';
import { EditorSessionService } from '../services/editorsession.service';
import { EditorContentService } from '../services/editorcontent.service';

export class WidthResizer {

    private currentPosition = 0;
    private widthLimit = 5;
    private widthOffset = 0;
    private active = false;
    private ghostContainers!: HTMLElement[];
    private editorContent!: HTMLElement;
    private contentArea!: HTMLElement;
    private glasspane!: HTMLElement;
    private lastPageX = 0;
    private ghostsRight = 0;

    constructor(
        private readonly renderer: Renderer2,
        private readonly editorSession: EditorSessionService,
        private readonly editorContentService: EditorContentService
    ) { }

    start(event: MouseEvent) {
        this.active = true;
        this.ghostContainers = this.editorContentService.querySelectorAll('.ghostcontainer');
        this.editorContent = this.editorContentService.querySelector('.content');
        this.contentArea = this.editorContentService.getContentArea();
        this.glasspane = this.editorContentService.getGlassPane();

        this.currentPosition = this.ghostContainers[0].offsetWidth;
        let partWidth = 0;
        this.ghostsRight = 0;

        const ghostsList = this.contentArea.getElementsByClassName('ghost label');
        for (const _ghost of ghostsList) {
            const ghost = ghostsList.item(0)!;
            const ghostType = ghost.getAttribute('svy-ghosttype');
            if (ghostType == 'comp') {
                this.ghostsRight = Math.max(this.ghostsRight, (ghost as HTMLElement).offsetLeft + (ghost as HTMLElement).offsetWidth);
            } else if (ghostType == 'part') {
                partWidth = (ghost as HTMLElement).offsetWidth;
            }
        }

        this.widthOffset = this.ghostContainers[0].offsetLeft + partWidth;
        this.lastPageX = event.pageX;
    }

    move(event: MouseEvent) {
        if (!this.active) return;
        const step = event.pageX - this.lastPageX;
        if (step != 0) {
            this.currentPosition += step;
            if (this.currentPosition >= this.widthLimit) {
                for (const ghostContainer of this.ghostContainers) {
                    this.renderer.setStyle(ghostContainer, 'width', this.currentPosition + 'px');
                }
                this.renderer.setStyle(this.editorContent, 'width', this.currentPosition + 'px');

                if (step > 0 && this.currentPosition + this.widthOffset > this.glasspane.offsetWidth) {
                    this.glasspane.style.width = this.currentPosition + this.widthOffset + 'px';
                }
            }
            this.lastPageX = event.pageX;
        }
    }

    end() {
        if (!this.active) return;
        this.active = false;
        if (this.currentPosition < this.widthLimit) {
            this.currentPosition = this.widthLimit;
        }
        const changes: Record<string, any> = {};
        const id = this.editorContentService.querySelector('.ghost[svy-ghosttype="form"]').getAttribute('svy-id')!;
        changes[id] = {
            'width': this.currentPosition
        };
        this.glasspane.style.width = Math.max(this.currentPosition + this.widthOffset, this.ghostsRight) + 'px';
        this.editorSession.sendChanges(changes);
    }

    updateLocation(changeX: number) {
        if (!this.active) return;
        if (this.currentPosition >= this.widthLimit) {
            for (const ghostContainer of this.ghostContainers) {
                this.renderer.setStyle(ghostContainer, 'width', this.currentPosition + 'px');
            }
            this.renderer.setStyle(this.editorContent, 'width', this.currentPosition + 'px');

            if (this.currentPosition + this.widthOffset > this.glasspane.offsetWidth) {
                this.glasspane.style.width = this.currentPosition + this.widthOffset + 'px';
            }
            this.currentPosition += changeX;
            this.contentArea.scrollLeft += changeX;
        }
    }
}
