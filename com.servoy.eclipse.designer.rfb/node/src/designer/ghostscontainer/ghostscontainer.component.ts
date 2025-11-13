import { Component, OnInit, Renderer2, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { EditorSessionService, ISelectionChangedListener, ISupportAutoscroll } from '../services/editorsession.service';
import { URLParserService } from '../services/urlparser.service';
import { Point } from '../mouseselection/mouseselection.component';
import { EditorContentService, IContentMessageListener } from '../services/editorcontent.service';

@Component({
    selector: 'designer-ghostscontainer',
    templateUrl: './ghostscontainer.component.html',
    styleUrls: ['./ghostscontainer.component.css'],
    standalone: false
})
export class GhostsContainerComponent implements OnInit, ISelectionChangedListener, OnDestroy, IContentMessageListener, ISupportAutoscroll {

    @ViewChild('element', { static: false }) elementRef: ElementRef<Element>;

    ghostOffset = 20;
    containerLeftOffset: number;
    containerTopOffset: number;
    leftOffsetRelativeToSelectedGhost: number;
    topOffsetRelativeToSelectedGhost: number;

    ghostContainers: Array<GhostContainer>;
    removeSelectionChangedListener: () => void;
    mousedownpoint: Point;
    draggingGhost: Ghost;
    draggingInGhostContainer: GhostContainer;
    draggingClone: Element;
    draggingGhostComponents: Array<DraggingGhostInfo>;

    draggingGhostComponent: HTMLElement;
    formWidth: number;
    formHeight: number;
    partTopPosition: number;

    private topLimit = 0;
    private bottomLimit = 0;
    private isLowestPart = false;
    private editorContent: HTMLElement;
    private contentArea: HTMLElement;
    private glasspane: HTMLElement;
    private ghostsBottom = 0;
    private lastMouseY = 0;
    private frameRect: DOMRect;

    constructor(protected readonly editorSession: EditorSessionService, protected readonly renderer: Renderer2,
        protected urlParser: URLParserService, private editorContentService: EditorContentService) {
        this.editorContentService.addContentMessageListener(this);
        this.removeSelectionChangedListener = this.editorSession.addSelectionChangedListener(this);
    }

    ngOnInit(): void {
        if (this.urlParser.isAbsoluteFormLayout()) {
            // for responsive this is way too early we need the real content size to show the ghosts
            this.renderGhosts()
        }
        this.editorContentService.getDocument().addEventListener('mouseup', (event: MouseEvent) => this.onMouseUp(event));
        this.editorContentService.getDocument().addEventListener('mousemove', (event: MouseEvent) => this.onMouseMove(event));
        this.editorContent = this.editorContentService.querySelector('.content');
        this.contentArea = this.editorContentService.getContentArea();
        this.glasspane = this.editorContentService.getGlassPane();
    }

    ngOnDestroy(): void {
        this.removeSelectionChangedListener();
        this.editorContentService.removeContentMessageListener(this);
    }

    contentMessageReceived(id: string, data: { property: string, width?: number, height?: number }) {
        if (id === 'renderGhosts') {
            this.renderGhosts();
        }
        // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
        if (id === 'updateFormSize' && this.urlParser.isAbsoluteFormLayout()) {
            this.formWidth = data.width;
            this.formHeight = data.height;
            this.renderGhosts();
        }

        if (id === 'redrawDecorators') {
            if (this.ghostContainers) {
                for (const ghostContainer of this.ghostContainers) {
                    for (const ghost of ghostContainer.ghosts) {
                        if (this.editorSession.getSelection().indexOf(ghost.uuid) >= 0) {
                            this.renderGhosts();
                            return;
                        }
                    }
                }
            }
        }

        if (id !== 'hideGhostContainer' && id !== 'positionClick') {
            this.hideShowGhosts('visible');
        }

        if (id === 'hideGhostContainer') {
            this.hideShowGhosts('hidden');
        }

    }

    hideShowGhosts(visibility: string) {
        if (this.elementRef) {
            const ghostsContainer = document.querySelectorAll(`.${this.elementRef.nativeElement.classList.value}`);
            Array.from(ghostsContainer).slice(1).forEach((item: HTMLElement) => {
                item.style.visibility = visibility;
                item.querySelectorAll('.ghost').forEach((ghost: HTMLElement) => ghost.style.visibility = visibility);
            });
        }
    }

    renderGhosts() {
        void this.editorSession.getGhostComponents<{ ghostContainers: Array<GhostContainer> }>().then((result: { ghostContainers: Array<GhostContainer> }) => {
            this.renderGhostsInternal(result.ghostContainers);
        });
    }

    private renderGhostsInternal(ghostContainers: Array<GhostContainer>) {
        if (!this.formWidth) {
            this.formWidth = this.urlParser.getFormWidth();
        }
        if (!this.formHeight) {
            this.formHeight = this.urlParser.getFormHeight();
        }
        this.ghostsBottom = 0;
        if (ghostContainers) {
            // set the ghosts
            for (const ghostContainer of ghostContainers) {
                if (!ghostContainer.style) ghostContainer.style = {} as CSSStyleDeclaration;
                if (ghostContainer.containerPositionInComp != undefined) {
                    const odd = (ghostContainer.containerPositionInComp % 2);
                    ghostContainer.style['background-color'] = odd ? 'rgba(150, 150, 150, 0.05)' : 'rgba(0, 100, 80, 0.05)';
                    ghostContainer.style['color'] = odd ? 'rgb(150, 150, 150)' : 'rgb(0, 100, 80)';
                    if (odd) {
                        ghostContainer.style['border-top'] = ghostContainer.style['border-bottom'] = 'dashed 1px';
                    }
                }
                if (!this.urlParser.isAbsoluteFormLayout()) {
                    const node = this.editorContentService.getContentElement(ghostContainer.uuid);
                    if (node !== undefined) {
                        ghostContainer.parentCompBounds = node.getBoundingClientRect();
                    }
                }
                if (ghostContainer.parentCompBounds) {
                    const spaceForEachContainer = 62 /*(basicWebComponent.getSize().height / totalGhostContainersOfComp)*/;
                    let emptySpaceTopBeforGhosts = 0; // see if we have room to add some extra pixels to top location - it shows nicer on large components when space is available
                    if (ghostContainer.parentCompBounds.height > ghostContainer.totalGhostContainersOfComp * spaceForEachContainer + 30) emptySpaceTopBeforGhosts = 30;

                    ghostContainer.style.left = (ghostContainer.parentCompBounds.left + 20) + 'px';
                    ghostContainer.style.top = (ghostContainer.parentCompBounds.top + ghostContainer.containerPositionInComp * spaceForEachContainer + emptySpaceTopBeforGhosts + 20) + 'px';
                    ghostContainer.style.width = ghostContainer.parentCompBounds.width + 'px';
                    ghostContainer.style.height = spaceForEachContainer + 'px';
                }
                else if (this.urlParser.isAbsoluteFormLayout()) {
                    ghostContainer.style.left = '20px';
                    ghostContainer.style.top = '20px';
                    ghostContainer.style.width = this.formWidth + 'px';
                    ghostContainer.style.height = this.formHeight + 'px';
                }

                if (this.editorContentService.getContentElement(ghostContainer.uuid)?.parentElement?.parentElement?.classList.contains('maxLevelDesign')) {
                    ghostContainer.style.display = 'none';
                }

                const filterGhostParts = ghostContainer.ghosts.filter(ghost => ghost.type == GHOST_TYPES.GHOST_TYPE_PART);
                const onlyBodyPart = filterGhostParts.length === 1 && filterGhostParts[0].text.toLowerCase() === "body";

                for (const ghost of ghostContainer.ghosts) {
                    if (ghost.type == GHOST_TYPES.GHOST_TYPE_GROUP) {
                        ghostContainer.style.display = 'none';
                        // groups are deprecated in new designer
                        continue;
                    }
                    let style = {};
                    ghost.hrstyle = { display: 'none' } as CSSStyleDeclaration;
                    if (ghost.type == GHOST_TYPES.GHOST_TYPE_PART) { // parts
                        style = {
                            background: '#d0d0d0',
                            top: ghost.location.y + 'px',
                            right: '-90px',
                            width: '90px',
                            height: '20px',
                            overflow: 'visible'
                        };
                        if (onlyBodyPart) {
                            style['visibility'] = 'hidden';
                        }
                        ghost.hrstyle = {
                            marginTop: '-1px',
                            borderTop: '1px dashed #000',
                            height: '0px',
                            width: (this.formWidth + 90) + 'px',
                            cursor: 'ns-resize',
                            float: 'right'
                        } as CSSStyleDeclaration;
                    } else if (ghost.type == GHOST_TYPES.GHOST_TYPE_FORM) { // the form
                        // why should we need this?, it interferes a lot with the events
                        style = {
                            display: 'none'/*
                            left: 0,
                            top: 0,
                            width: ghost.size.width + "px",
                            height: ghost.size.height + "px",
                            padding: "3px"*/
                        };
                    } else {
                        let xOffset = this.ghostOffset;
                        let yOffset = this.ghostOffset;
                        if (ghost.type == GHOST_TYPES.GHOST_TYPE_COMPONENT) {
                            // show outside component at exact location
                            xOffset = 0;
                            yOffset = 0;
                        }
                        // SOME of these are set directly in editor.css for .ghost-dnd-placeholder; if you change things that should affect
                        // the placeholder used when moving droppable config custom objects via drag&drop, please change them in editor.css as well
                        style = {
                            opacity: 0.7,
                            padding: '3px',
                            left: ghost.location.x + xOffset + 'px',
                            top: ghost.location.y + yOffset + 'px',
                            width: ghost.size.width + 'px',
                            height: ghost.size.height + 'px'
                        };
                        if (ghost.type == GHOST_TYPES.GHOST_TYPE_INVISIBLE) {
                            style['background'] = '#d0d0d0';
                        }
                        else if (ghost.type == GHOST_TYPES.GHOST_TYPE_CONFIGURATION) {
                            style['background'] = '#ffbb37';
                        }
                        else {
                            style['background'] = '#e4844a';
                        }
                        this.ghostsBottom = Math.max(this.ghostsBottom, ghost.location.y + yOffset + ghost.size.height);
                    }
                    if (this.editorSession.getSelection().indexOf(ghost.uuid) >= 0) {
                        style['background'] = '#07f';
                        style['color'] = '#fff';
                    }
                    ghost.style = style;
                }
            }
        }
        this.ghostContainers = ghostContainers;
    }

    openContainedForm(ghost: Ghost) {
        if (ghost.type != GHOST_TYPES.GHOST_TYPE_PART) {
            this.editorSession.openContainedForm(ghost.uuid);
        }
    }

    onMouseDown(event: MouseEvent, ghost: Ghost, ghostContainer: GhostContainer) {
        let selection = this.editorSession.getSelection();
        if (event.ctrlKey || event.metaKey) {
            const index = selection.indexOf(ghost.uuid);
            if (index >= 0) {
                selection.splice(index, 1);
            }
            else {
                selection.push(ghost.uuid);
            }
            this.editorSession.setSelection(selection);
        }
        else {
            if (event.button == 2 && selection.indexOf(ghost.uuid) >= 0) {
                //if we right click on the selected element while multiple selection, just show context menu and do not modify selection
                this.editorSession.getState().ghosthandle = true;
                return;
            }
            else if (this.editorSession.getSelection().indexOf(ghost.uuid) == -1 || ghost.type != GHOST_TYPES.GHOST_TYPE_COMPONENT) {
                this.editorSession.setSelection([ghost.uuid]);
            }

        }
        if (event.button == 0) {
            this.editorSession.getState().dragging = true;
            this.mousedownpoint = { x: event.pageX, y: event.pageY };
            this.draggingGhost = ghost;
            this.draggingInGhostContainer = ghostContainer;
            if (this.draggingGhost.type == GHOST_TYPES.GHOST_TYPE_CONFIGURATION || this.draggingGhost.type === GHOST_TYPES.GHOST_TYPE_COMPONENT) {
                const parentRect = (event.currentTarget as Element).parentElement.getBoundingClientRect();
                this.containerLeftOffset = parentRect.left;
                this.containerTopOffset = parentRect.top;
                const rect = (event.currentTarget as Element).getBoundingClientRect();
                this.leftOffsetRelativeToSelectedGhost = event.clientX - rect.left; //x position within the element.
                this.topOffsetRelativeToSelectedGhost = event.clientY - rect.top;  //y position within the element.
            }
            if (this.draggingGhost.type == GHOST_TYPES.GHOST_TYPE_CONFIGURATION) {
                this.draggingClone = (event.currentTarget as Element).cloneNode(true) as Element;
                this.renderer.setStyle(this.draggingClone, 'background', '#ffbb37');
            }
            if (this.draggingGhost.type === GHOST_TYPES.GHOST_TYPE_COMPONENT) {
                this.frameRect = this.editorContentService.getContent().getBoundingClientRect();
                selection = this.editorSession.getSelection();
                this.draggingGhostComponents = new Array<DraggingGhostInfo>();
                if (selection && selection.length > 1) {
                    for (let i = 0; i < selection.length; i++) {
                        const node = this.editorContentService.querySelector('[svy-id="' + selection[i] + '"]');
                        if (node) {
                            for (const ghost of ghostContainer.ghosts) {
                                if (this.draggingGhost != ghost && ghost.uuid == selection[i] && ghost.type == GHOST_TYPES.GHOST_TYPE_COMPONENT) {
                                    this.draggingGhostComponents.push(new DraggingGhostInfo(ghost, node));
                                }
                            }
                        }
                    }
                }
            }
            if (this.draggingGhost.type == GHOST_TYPES.GHOST_TYPE_PART) {
                this.draggingGhostComponent = this.editorContentService.querySelector('[svy-id="' + this.draggingGhost.uuid + '"]');
                const containerHeight = parseInt(ghostContainer.style.height.replace('px', ''));
                const parentRect = (event.currentTarget as Element).parentElement.getBoundingClientRect();
                this.containerTopOffset = parentRect.top;
                this.topLimit = this.draggingGhostComponent.previousSibling ? (this.draggingGhostComponent.previousElementSibling as HTMLElement).offsetTop + 5 : 5;
                this.bottomLimit = (this.draggingGhostComponent.nextElementSibling as HTMLElement).offsetTop - 5;
                this.partTopPosition = this.draggingGhost.location.y;
                this.lastMouseY = event.pageY;
                this.isLowestPart = false;
                if (this.partTopPosition == containerHeight) {
                    this.isLowestPart = true;
                }
                this.editorSession.registerAutoscroll(this);
            }
        }
    }

    onMouseUp(event: MouseEvent) {
        this.editorSession.getState().ghosthandle = false;
        if (this.draggingGhost) {
            if (this.mousedownpoint.y != event.pageY || this.mousedownpoint.x != event.pageX) {
                if (this.draggingGhost.type == GHOST_TYPES.GHOST_TYPE_CONFIGURATION) {
                    const obj = {};
                    for (const ghost of this.draggingInGhostContainer.ghosts) {
                        obj[ghost.uuid] = { 'x': ghost.location.x, 'y': ghost.location.y };
                    }
                    this.editorSession.sendChanges(obj);
                }
                if (this.draggingGhost.type == GHOST_TYPES.GHOST_TYPE_CONFIGURATION) {
                    this.renderGhostsInternal(this.ghostContainers);
                }
                if (this.draggingGhost.type == GHOST_TYPES.GHOST_TYPE_PART) {
                    const changes = {};
                    if (this.partTopPosition < this.topLimit) {
                        this.partTopPosition = this.topLimit;
                    }
                    if (this.partTopPosition > this.bottomLimit && !this.isLowestPart) {
                        this.partTopPosition = this.bottomLimit;
                    }
                    if (this.isLowestPart) {
                        const id = document.querySelector('.ghost[svy-ghosttype="form"]').getAttribute('svy-id');
                        changes[id] = { 'y': this.partTopPosition };
                        this.editorSession.sendChanges(changes);

                        //todo: correct glasspane size
                        this.glasspane.style.height = Math.max(this.partTopPosition + this.ghostOffset, this.ghostsBottom) + 'px';
                    }
                    changes[this.draggingGhost.uuid] = { 'y': this.partTopPosition };
                    this.editorSession.sendChanges(changes);
                }
            }
            // this is just to re-render the decorators
            this.editorSession.updateSelection(this.editorSession.getSelection(), true);
            this.editorSession.getState().dragging = false;
            this.editorSession.unregisterAutoscroll(this);
        }
        if (this.draggingClone) {
            this.draggingClone.remove();
            this.draggingClone = null;
        }
        this.editorContentService.getGlassPane().style.cursor = 'default';
        this.draggingGhost = null;
        this.draggingInGhostContainer = null;
        this.draggingGhostComponent = null;
        this.draggingGhostComponents = null;
        this.renderGhosts();
    }

    onMouseMove(event: MouseEvent) {
        if (this.draggingGhost && (this.mousedownpoint.y != event.pageY || this.mousedownpoint.x != event.pageX)) {
            this.editorContentService.getGlassPane().style.cursor = 'pointer';
            if (this.draggingGhost.type == GHOST_TYPES.GHOST_TYPE_CONFIGURATION) {
                if (!this.draggingClone.parentNode) {
                    this.editorContentService.getBodyElement().appendChild(this.draggingClone);
                }
                this.renderer.setStyle(this.draggingClone, 'left', (event.pageX - this.leftOffsetRelativeToSelectedGhost) + 'px');
                this.renderer.setStyle(this.draggingClone, 'top', (event.pageY - this.topOffsetRelativeToSelectedGhost) + 'px');
                const initialIndex = this.draggingInGhostContainer.ghosts.indexOf(this.draggingGhost);
                let newIndex = -1;
                const ghostWidth = this.draggingGhost.size.width;
                for (let index = 0; index < this.draggingInGhostContainer.ghosts.length; index++) {
                    const ghostStart = this.draggingInGhostContainer.ghosts[index].location.x + this.ghostOffset;
                    const ghostEnd = ghostStart + ghostWidth;
                    const currentPosition = event.pageX - this.containerLeftOffset;
                    if (currentPosition == ghostStart || currentPosition == ghostEnd) {
                        // on the border, do nothing
                        break;
                    }
                    if (newIndex < 0 && (currentPosition > ghostStart || index == 0) && (currentPosition < ghostEnd || index == this.draggingInGhostContainer.ghosts.length - 1)) {
                        // found its place, is it changed ?
                        if (index == initialIndex) {
                            // everything is fine, do not touch it
                            break;
                        }
                        newIndex = index;
                        this.draggingGhost.location.x = this.draggingInGhostContainer.ghosts[newIndex].location.x;
                        this.draggingGhost.style.left = this.draggingGhost.location.x + this.ghostOffset + 'px';
                    }
                    if (index < initialIndex && newIndex >= 0) {
                        // the newindex was already found, move current ghost to right
                        this.draggingInGhostContainer.ghosts[index].location.x += ghostWidth;
                        this.draggingInGhostContainer.ghosts[index].style.left = this.draggingInGhostContainer.ghosts[index].location.x + this.ghostOffset + 'px';
                    }
                    if (index > initialIndex) {
                        if (newIndex < 0 || newIndex == index) {
                            //move current ghost to left
                            this.draggingInGhostContainer.ghosts[index].location.x -= ghostWidth;
                            this.draggingInGhostContainer.ghosts[index].style.left = this.draggingInGhostContainer.ghosts[index].location.x + this.ghostOffset + 'px';

                        }
                    }
                }
                if (newIndex >= 0) {
                    // now all styling is fixed, add ghost in new position so that position is ordered
                    this.draggingInGhostContainer.ghosts.splice(initialIndex, 1);
                    this.draggingInGhostContainer.ghosts.splice(newIndex, 0, this.draggingGhost);
                }
            }
            if (this.draggingGhost.type === GHOST_TYPES.GHOST_TYPE_COMPONENT) {
                if (this.draggingGhostComponent === null) {
                    this.draggingGhostComponent = this.editorContentService.querySelector('[svy-id="' + this.draggingGhost.uuid + '"]');
                }
                this.renderer.setStyle(this.draggingGhostComponent, 'left', (event.pageX - this.containerLeftOffset - this.leftOffsetRelativeToSelectedGhost) + 'px');
                this.renderer.setStyle(this.draggingGhostComponent, 'top', (event.pageY - this.containerTopOffset - this.topOffsetRelativeToSelectedGhost) + 'px');
                if (this.draggingGhostComponents) {
                    for (const draggingInfo of this.draggingGhostComponents) {
                        this.renderer.setStyle(draggingInfo.ghostComponent, 'left', (event.pageX - this.mousedownpoint.x + draggingInfo.originalLeft - this.containerLeftOffset) + 'px');
                        this.renderer.setStyle(draggingInfo.ghostComponent, 'top', (event.pageY - this.mousedownpoint.y + draggingInfo.originalTop - this.containerTopOffset) + 'px');
                    }
                }
                if (this.frameRect) {
                    let visibilityStyle = 'visible';
                    if (this.frameRect.left <= event.pageX && this.frameRect.right >= event.pageX && this.frameRect.top <= event.pageY && this.frameRect.bottom >= event.pageY) {
                        visibilityStyle = 'hidden';
                    }
                    this.draggingGhostComponent.style.visibility = visibilityStyle;
                    if (this.draggingGhostComponents) {
                        for (const draggingInfo of this.draggingGhostComponents) {
                            draggingInfo.ghostComponent.style.visibility = visibilityStyle;
                        }
                    }
                }
            }
            if (this.draggingGhost.type === GHOST_TYPES.GHOST_TYPE_PART) {
                let step = event.pageY - this.lastMouseY;
                if (step != 0) {
                    this.partTopPosition += step;
                    if (this.partTopPosition > this.topLimit) {
                        if (!this.isLowestPart && this.partTopPosition < this.bottomLimit) {
                            this.renderer.setStyle(this.draggingGhostComponent, 'top', this.partTopPosition + 'px');
                        } else if (this.isLowestPart) {
                            this.formHeight = this.partTopPosition;
                            for (let index = 0; index < this.ghostContainers.length; index++) {
                                this.ghostContainers[0].style.height = this.partTopPosition + 'px';
                            }
                            this.renderer.setStyle(this.editorContent, 'height', this.partTopPosition + 'px');
                            if (this.partTopPosition + this.ghostOffset > this.glasspane.offsetHeight) {
                                this.glasspane.style.height = this.partTopPosition + this.ghostOffset + 'px';
                            }
                            this.renderer.setStyle(this.draggingGhostComponent, 'top', this.partTopPosition + 'px');
                        }
                    }
                    this.lastMouseY = event.pageY;
                }
            }
        }
    }

    selectionChanged(ids: Array<string>, redrawDecorators?: boolean, designerChange?: boolean): void {
        // this is an overkill but sometimes we need the server side data for the ghosts (for example when element was dragged out of form bounds and is shown as ghost)
        // not sure how to detect when we really need to redraw
        if (designerChange) this.renderGhosts();
        else {
            this.renderGhostsInternal(this.ghostContainers);
        }
    }

    getAutoscrollLockId(): string {
        return 'ghost-container';
    }

    updateLocationCallback(changeX: number, changeY: number) {
        if (this.partTopPosition <= this.topLimit) {
            return;
        } else if (!this.isLowestPart && this.partTopPosition >= this.bottomLimit) {
            return;
        } //else partTopPosition > this.topLimit && (this.isLowestPart || partTopPosition <= bottomLimit)
        if (this.isLowestPart) {
            this.formHeight = this.partTopPosition;
            for (let index = 0; index < this.ghostContainers.length; index++) {
                this.ghostContainers[0].style.height = this.partTopPosition + 'px';
            }
            this.renderer.setStyle(this.editorContent, 'height', this.partTopPosition + 'px');
            if (this.partTopPosition + this.ghostOffset > this.glasspane.offsetHeight) {
                this.glasspane.style.height = this.partTopPosition + this.ghostOffset + 'px';
            }
            this.renderer.setStyle(this.draggingGhostComponent, 'top', this.partTopPosition + 'px');
        } else {//!lowestpart && partTopPosition < bottomlimit
            this.renderer.setStyle(this.draggingGhostComponent, 'top', this.partTopPosition + 'px');
        }

        this.partTopPosition += changeY;
        this.contentArea.scrollTop += changeY;

    }
}

class GhostContainer {
    propertyName: string;
    uuid: string;
    class: string;
    style: CSSStyleDeclaration;
    parentCompBounds: { left?: number, top?: number; width?: number; height?: number };
    containerPositionInComp: number;
    totalGhostContainersOfComp: number;
    ghosts: Array<Ghost>;
}

class Ghost {
    uuid: string;
    text: string;
    type: GHOST_TYPES;
    class: string;
    propertyType: string;
    style: { left?: string, top?: string; right?: string; width?: string; height?: string };
    hrstyle: CSSStyleDeclaration;
    location: { x: number, y: number };
    size: { width: number, height: number };
}

export enum GHOST_TYPES {
    GHOST_TYPE_CONFIGURATION = 'config',
    GHOST_TYPE_COMPONENT = 'comp',
    GHOST_TYPE_PART = 'part',
    GHOST_TYPE_FORM = 'form',
    GHOST_TYPE_INVISIBLE = 'invisible',
    GHOST_TYPE_GROUP = 'group'
}

class DraggingGhostInfo {

    ghost: Ghost;
    ghostComponent: HTMLElement;
    originalLeft: number;
    originalTop: number;

    constructor(ghost: Ghost, ghostComponent: HTMLElement) {
        this.ghost = ghost;
        this.ghostComponent = ghostComponent;
        const parentRect = ghostComponent.getBoundingClientRect();
        this.originalLeft = parentRect.left;
        this.originalTop = parentRect.top;
    }
}