import { Directive, HostListener } from "@angular/core";
import { EditorSessionService } from "../services/editorsession.service";
import { EditorContentService } from '../services/editorcontent.service';
import { URLParserService } from "../services/urlparser.service";
import { ElementInfo } from "./resizeknob.directive";

@Directive({
    selector: '[keyboardlayout]'
})
export class KeyboardLayoutDirective {

    isSendChanges = true;
    boundsUpdating = false;

    constructor(private editorSession: EditorSessionService, private urlParser: URLParserService, private editorContentService : EditorContentService) {}

    @HostListener('document:keydown', ['$event'])
    onKeyDown(event: KeyboardEvent) {
        const fixedKeyEvent = this.editorSession.getFixedKeyEvent(event);
        if (!this.editorSession.isInlineEditMode() && fixedKeyEvent.keyCode > 36 && fixedKeyEvent.keyCode < 41) {
            const selection = this.editorSession.getSelection();
            if (selection.length > 0) {
				// var ghost = editorScope.getGhost(selection[0].getAttribute("svy-id"));
				// if (selection.length == 1 && (ghost && ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_FORM)) {
				// 	isSendChanges = false;
				// 	return true;
				// }
                if (!this.urlParser.isAbsoluteFormLayout()) {
                    this.isSendChanges = false;
                    return true;
                }
                this.boundsUpdating = true;
                let changeX = 0, changeY = 0, changeW = 0, changeH = 0;
                let magnitude = 1;

                if ((fixedKeyEvent.metaKey || fixedKeyEvent.ctrlKey) && fixedKeyEvent.altKey) {
                    this.isSendChanges = false;
                    return true;
                } else if (fixedKeyEvent.altKey) {
                    magnitude = 20;
                } else if (fixedKeyEvent.ctrlKey || fixedKeyEvent.metaKey) {
                    magnitude = 10;
                }
                const isResize = fixedKeyEvent.shiftKey;

				switch (fixedKeyEvent.keyCode) {
                    case 37:
                        if (isResize) {
                            changeW = -magnitude;
                        } else {
                            changeX = -magnitude;
                        }
                        break;
                    case 38:
                        if (isResize) {
                            changeH = -magnitude;
                        } else {
                            changeY = -magnitude;
                        }
                        break;
                    case 39:
                        if (isResize) {
                            changeW = magnitude;
                        } else {
                            changeX = magnitude;
                        }
                        break;
                    case 40:
                        if (isResize) {
                            changeH = magnitude;
                        } else {
                            changeY = magnitude;
                        }
                        break;
                }
                // selection = utils.addGhostsToSelection(selection);
				// if (selection.length > 0)
				// 	highlightDiv.style.display = 'none';

                for (let i = 0; i < selection.length; i++) {
					const node = selection[i];
                    let element = this.editorContentService.getContentElement(node);
                    while(element && !element.classList.contains('svy-wrapper')) {
                        element = element.parentElement;
                    }
                    const elementInfo = new ElementInfo(element);
					if (elementInfo) {
						if (isResize) {
							if (elementInfo.width + changeW > 0) elementInfo.width = elementInfo.width + changeW;
							if (elementInfo.height + changeH > 0) elementInfo.height = elementInfo.height + changeH;
							element.style.width = elementInfo.width + 'px';
							element.style.height = elementInfo.height + 'px';
						} else {
							if (elementInfo.y + changeY > -1) elementInfo.y = elementInfo.y + changeY;
							if (elementInfo.x + changeX > -1) elementInfo.x = elementInfo.x + changeX;
							if (elementInfo.element.style.left.length) {
								element.style.left = elementInfo.x + 'px';
							}
							if (elementInfo.element.style.top.length) {
								element.style.top = elementInfo.y + 'px';
							}
							if (elementInfo.element.style.right.length) {
								elementInfo.element.style.right = (parseInt(this.editorContentService.getValueInPixel(elementInfo.element.style.right, 'x').replace('px', ''))|| 0)  - changeX + 'px';
							}
							if (elementInfo.element.style.bottom.length) {
								elementInfo.element.style.bottom = (parseInt(this.editorContentService.getValueInPixel(elementInfo.element.style.bottom, 'y').replace('px', ''))|| 0)  - changeY + 'px';
							}
						}
                    }
                    else if (!isResize) {
						// var ghostObject = editorScope.getGhost(node.getAttribute("svy-id"));
						// editorScope.updateGhostLocation(ghostObject, ghostObject.location.x + changeX, ghostObject.location.y + changeY)

					}
				}
                this.editorSession.updateSelection(this.editorSession.getSelection(), true, true);
                this.isSendChanges = true; 
                return false; 
            } else {
                this.isSendChanges = false;
                return true;
            }
        }
    }

    @HostListener('document:keyup', ['$event'])
    onKeyup(event: KeyboardEvent) {
        const changes = {};
        if (this.boundsUpdating) {
            this.boundsUpdating = false;
            const selection = this.editorSession.getSelection();
            // if (selection.length > 0)
		    // highlightDiv.style.display = 'none';
            // selection = utils.addGhostsToSelection(selection);

            for (var i = 0; i < selection.length; i++) {
                const node = selection[i];
                let element = this.editorContentService.getContentElement(node);
                while(element && !element.classList.contains('svy-wrapper')) {
                    element = element.parentElement;
                }
                const elementInfo = new ElementInfo(element);
                if (elementInfo) {
                    changes[node] = {
                        x : elementInfo.x,
                        y : elementInfo.y,
                        width : elementInfo.width,
                        height : elementInfo.height,
                        move: !event.shiftKey
                    }
                } else {
                    // var ghostObject = editorScope.getGhost(node.getAttribute("svy-id"));
                    // obj[node.getAttribute("svy-id")] = {
                    //     x : ghostObject.location.x,
                    //     y : ghostObject.location.y
                    //     }
                    // }
                }
            }
        }
        if (this.isSendChanges && Object.keys(changes).length) {
            this.editorSession.sendChanges(changes);
        } else {
            if (event.keyCode > 36 && event.keyCode < 41) {
                this.editorSession.keyPressed(event);
            }
        }
    }
}