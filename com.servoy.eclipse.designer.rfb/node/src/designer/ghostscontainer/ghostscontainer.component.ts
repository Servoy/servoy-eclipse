import { Component, OnInit, Inject, Renderer2, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { EditorSessionService, ISelectionChangedListener } from '../services/editorsession.service';
import { URLParserService } from '../services/urlparser.service';
import { WindowRefService } from '@servoy/public';
import { Point } from '../mouseselection/mouseselection.component';

@Component({
    selector: 'designer-ghostscontainer',
    templateUrl: './ghostscontainer.component.html',
    styleUrls: ['./ghostscontainer.component.css']
})
export class GhostsContainerComponent implements OnInit, ISelectionChangedListener, OnDestroy {
    
    @ViewChild('element', { static: false }) elementRef: ElementRef;
    
    ghosts: Array<GhostContainer>;
    removeSelectionChangedListener: () => void;
    mousedownpoint: Point;
    draggingGhost: Ghost;

    constructor(protected readonly editorSession: EditorSessionService, @Inject(DOCUMENT) private doc: Document, protected readonly renderer: Renderer2,
        protected urlParser: URLParserService, private windowRefService: WindowRefService) {
        this.windowRefService.nativeWindow.addEventListener("message", (event) => {
            if (event.data.id === 'renderGhosts') {
                this.renderGhosts();
            }
        });
        this.removeSelectionChangedListener = this.editorSession.addSelectionChangedListener(this);
    }

    ngOnInit(): void {
        this.renderGhosts();
        let content = this.doc.querySelector('.content-area') as HTMLElement;
        content.addEventListener('mouseup', (event) => this.onMouseUp(event));
    }

    ngOnDestroy(): void {
        this.removeSelectionChangedListener();
    }

    renderGhosts() {
        this.editorSession.getGhostComponents().then((result) => {
            this.renderGhostsInternal(result.ghostContainers);
        });
    }

    private renderGhostsInternal(ghostContainers) {
        if (ghostContainers) {
            // set the ghosts
            for (let ghostContainer of ghostContainers) {
                if (!ghostContainer.style) ghostContainer.style = {};
                if (ghostContainer.containerPositionInComp != undefined) {
                    let odd = (ghostContainer.containerPositionInComp % 2);
                    ghostContainer.style['background-color'] = odd ? "rgba(150, 150, 150, 0.05)" : "rgba(0, 100, 80, 0.05)";
                    ghostContainer.style['color'] = odd ? "rgb(150, 150, 150)" : "rgb(0, 100, 80)";
                    if (odd) {
                        ghostContainer.style['border-top'] = ghostContainer.style['border-bottom'] = 'dashed 1px';
                    }
                }
                if (ghostContainer.parentCompBounds) {
                    let spaceForEachContainer = 62 /*(basicWebComponent.getSize().height / totalGhostContainersOfComp)*/;
                    let emptySpaceTopBeforGhosts = 0; // see if we have room to add some extra pixels to top location - it shows nicer on large components when space is available
                    if (ghostContainer.parentCompBounds.height > ghostContainer.totalGhostContainersOfComp * spaceForEachContainer + 30) emptySpaceTopBeforGhosts = 30;

                    ghostContainer.style.left = (ghostContainer.parentCompBounds.left + 20) + "px";
                    ghostContainer.style.top = (ghostContainer.parentCompBounds.top + ghostContainer.containerPositionInComp * spaceForEachContainer + emptySpaceTopBeforGhosts + 20) + "px";
                    ghostContainer.style.width = ghostContainer.parentCompBounds.width + "px";
                    ghostContainer.style.height = spaceForEachContainer + "px";
                }
                else if (this.urlParser.isAbsoluteFormLayout()) {
                    ghostContainer.style.left = "20px";
                    ghostContainer.style.top = "20px";
                    ghostContainer.style.width = this.urlParser.getFormWidth() + "px";
                    ghostContainer.style.height = this.urlParser.getFormHeight() + "px";
                }

                for (let ghost of ghostContainer.ghosts) {
                    let style = {};
                    ghost.hrstyle = { display: 'none' };
                    if (ghost.type == GHOST_TYPES.GHOST_TYPE_PART) { // parts
                        style = {
                            background: "#d0d0d0",
                            top: ghost.location.y + "px",
                            right: "-90px",
                            width: "90px",
                            height: "20px",
                            cursor: "s-resize",
                            overflow: "visible"
                        };
                        ghost.hrstyle = {
                            marginTop: "-1px",
                            borderTop: "1px dashed #000",
                            height: "0px",
                            width: (this.urlParser.getFormWidth() + 90) + "px",
                            float: "right"
                        }
                    } else if (ghost.type == GHOST_TYPES.GHOST_TYPE_FORM) { // the form
                        // why should we need this, it interferes a lot with the events
                        style = {
                            display: 'none'/*
                            left: 0,
                            top: 0,
                            width: ghost.size.width + "px",
                            height: ghost.size.height + "px",
                            padding: "3px"*/
                        };
                    } else {
                        let xOffset = 20;
                        let yOffset = 20;
                        if (ghost.type == GHOST_TYPES.GHOST_TYPE_COMPONENT) {
                            // show outside component at exact location
                            xOffset = 0;
                            yOffset = 0;
                        }
                        // SOME of these are set directly in editor.css for .ghost-dnd-placeholder; if you change things that should affect
                        // the placeholder used when moving droppable config custom objects via drag&drop, please change them in editor.css as well
                        style = {
                            opacity: 0.7,
                            padding: "3px",
                            left: ghost.location.x + xOffset + 'px',
                            top: ghost.location.y + yOffset + 'px',
                            width: ghost.size.width + 'px',
                            height: ghost.size.height + 'px'
                        };
                        if (ghost.type == GHOST_TYPES.GHOST_TYPE_INVISIBLE) {
                            style['background'] = "#d0d0d0";
                        }
                        else if (ghost.type == GHOST_TYPES.GHOST_TYPE_CONFIGURATION) {
                            style['background'] = "#ffbb37";
                        }
                        else if (ghost.type != GHOST_TYPES.GHOST_TYPE_GROUP) {
                            style['background'] = "#e4844a";
                        }
                    }
                    if (this.editorSession.getSelection().indexOf(ghost.uuid) >= 0) {
                        style['background'] = "#07f";
                        style['color'] = "#fff";
                    }
                    ghost.style = style;
                }
            }
        }
        this.ghosts = ghostContainers;
    }

    openContainedForm(ghost) {
        if (ghost.type != GHOST_TYPES.GHOST_TYPE_PART) {
            this.editorSession.openContainedForm(ghost);
        }
    }

    onMouseDown(event, ghost) {
        event.stopPropagation();
        this.editorSession.setSelection([ghost.uuid]);
        if (event.button == 0) {
            this.mousedownpoint = { x: event.pageX, y: event.pageY };
            this.draggingGhost = ghost;
        }
        return false
    }

    private onMouseUp(event: MouseEvent) {
        if (this.draggingGhost && this.mousedownpoint.y != event.pageY) {
            if (this.draggingGhost.type == GHOST_TYPES.GHOST_TYPE_PART) {
                const obj = {};
                obj[this.draggingGhost.uuid] = { 'y': event.pageY - this.elementRef.nativeElement.getBoundingClientRect().top};
                this.editorSession.sendChanges(obj);
            }
        }
        this.draggingGhost = null;
    }

    selectionChanged(selection: Array<string>): void {
        this.renderGhostsInternal(this.ghosts);
    }
}

class GhostContainer {
    propertyName: string;
    class: string;
    style: any;
    parentCompBounds: any;
    containerPositionInComp: number;
    ghosts: Array<Ghost>;
}

class Ghost {
    uuid: string;
    text: string;
    type: string;
    class: string;
    propertyType: string;
    style: any;
    hrstyle: any;
}

enum GHOST_TYPES {
    GHOST_TYPE_CONFIGURATION = "config",
    GHOST_TYPE_COMPONENT = "comp",
    GHOST_TYPE_PART = "part",
    GHOST_TYPE_FORM = "form",
    GHOST_TYPE_INVISIBLE = "invisible",
    GHOST_TYPE_GROUP = "group"
}