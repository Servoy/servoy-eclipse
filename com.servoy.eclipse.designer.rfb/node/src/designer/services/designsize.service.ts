import { Injectable } from "@angular/core";
import { EditorContentComponent } from "../editorcontent/editorcontent.component";
import { ToolbarComponent, ToolbarItem, TOOLBAR_CATEGORIES } from "../toolbar/toolbar.component";
import { EditorSessionService } from "./editorsession.service";

@Injectable()
export class DesignSizeService {

    lastWidth: string;
    lastHeight = "auto";
    isPortrait = true;
	lastClicked: string;

    btnDesktopSize: ToolbarItem;
    btnTabletSize: ToolbarItem;
    btnMobileSize: ToolbarItem;
    btnCustomHeight: ToolbarItem;
    btnCustomWidth: ToolbarItem;

    editor: EditorContentComponent;

    constructor(protected readonly editorSession: EditorSessionService) {
    }

    setEditor(editor: EditorContentComponent) {
        this.editor = editor;
    }

    createItems(toolbar: ToolbarComponent) {
        this.btnDesktopSize = new ToolbarItem(
            "Desktop",
            "toolbar/icons/designsize/desktop_preview.png",
            true,
            () => { 
                this.editor.setContentSizeFull(true);
                this.lastWidth = this.btnCustomWidth.text = this.editor.getFormInitialWidth();
                this.lastHeight = this.btnCustomHeight.text = "auto";
                this.editorSession.setFormFixedSize({"width": this.lastWidth, "height" : this.lastHeight});
            }
        );
  
        this.btnTabletSize = new ToolbarItem(
            "Tablet",
            "toolbar/icons/designsize/tablet_preview.png",
            true,
            () => {
                if(this.lastClicked == "Tablet") this.isPortrait = !this.isPortrait;
                if(this.isPortrait)  {
                    this.setSize("768px", "1024px",true);
                }
                else {
                    this.setSize("1024px", "768px",true);
                }
                this.lastClicked = "Tablet";
            }
        );	
  
        this.btnMobileSize = new ToolbarItem(
            "Phone",
            "toolbar/icons/designsize/mobile_preview.png",
            true,
            () => {
                if(this.lastClicked == "Phone") this.isPortrait = !this.isPortrait;
                if(this.isPortrait) {
                	this.setSize("320px", "568px",false);
                }
                else {
                	this.setSize("568px", "320px",false);
                }
                this.lastClicked = "Phone";
            }
        );
      
        this.btnCustomHeight = new ToolbarItem(
            "auto",
            null,
            true,
            (selection) => {
                this.setSize(this.lastWidth, selection,true);
            }
      );
      this.btnCustomHeight.tooltip = "Fixed design height",
      this.btnCustomHeight.list = [{"text": "auto"}, {"text": "480px"}, {"text": "640px"}, {"text": "1024px"}, {"text": "2048px"}];
      this.btnCustomHeight.onselection = (selection) => {
        this.btnCustomHeight.onclick(selection);
        return selection;
      };
  
      this.btnCustomWidth = new ToolbarItem(
        "",
        null,
        true,
        (selection) => {
          this.editor.setContentSize(selection, this.lastHeight, true);
          this.lastWidth = selection;
          this.editorSession.setFormFixedSize({"width" : this.lastWidth});
        }
      );
      
      this.btnCustomWidth.tooltip = "Fixed design width";
      this.btnCustomWidth.onselection = (selection) => {
        this.btnCustomWidth.onclick(selection);
        return selection;
      };
      this.btnCustomWidth.faIcon = "fas fa-times fa-sm";
  
      toolbar.add(this.btnDesktopSize, TOOLBAR_CATEGORIES.STICKY);
      toolbar.add(this.btnTabletSize, TOOLBAR_CATEGORIES.STICKY);
      toolbar.add(this.btnMobileSize, TOOLBAR_CATEGORIES.STICKY);
      toolbar.add(this.btnCustomWidth, TOOLBAR_CATEGORIES.STICKY);
      toolbar.add(this.btnCustomHeight, TOOLBAR_CATEGORIES.STICKY);
    }

    setupItems(toolbar: ToolbarComponent) {
        this.lastWidth = this.editor.getFormInitialWidth();
        this.btnCustomWidth.text = this.lastWidth;
        this.btnCustomWidth.list = [{"text": "320px"}, {"text": "568px"}, {"text": "640px"}, {"text": "768px"}, {"text" : this.lastWidth}];

        const formSizePromise = this.editorSession.getFormFixedSize();
        formSizePromise.then((result) => {
            this.lastHeight = result.height ? result.height : this.lastHeight;
            this.btnCustomHeight.text = this.lastHeight;
            this.lastWidth = result.width ? result.width : this.lastWidth;
            this.btnCustomWidth.text = this.lastWidth;
            if (result.height || result.width) {
//              editor.contentLoaded.then(function() {
                this.setSize(this.lastWidth, this.lastHeight, true);
//              });
            }
        });
    }

    setSize(width: string , height: string, fixedSize: boolean) {
		this.editor.setContentSize(width, height, fixedSize);
		this.lastWidth = this.btnCustomWidth.text =  width;
		this.lastHeight = this.btnCustomHeight.text = height;
		this.editorSession.setFormFixedSize({"width": this.lastWidth, "height" : this.lastHeight});
	}
}