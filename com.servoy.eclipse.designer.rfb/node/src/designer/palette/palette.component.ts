import { Component } from '@angular/core';
import { EditorSessionService } from '../services/editorsession.service';

@Component({
  selector: 'designer-palette',
  templateUrl: './palette.component.html',
  styleUrls: ['./palette.component.css']
})
export class PaletteComponent {
    
    public searchText : string;
    
     constructor(protected readonly editorSession: EditorSessionService) {
    }


    openPackageManager(){
        this.editorSession.openPackageManager();
    }
}
