import { Component, OnInit } from '@angular/core';
import { WindowRefService } from '@servoy/public';
import { WebsocketSession, WebsocketService } from '../sablo/websocket.service';
import {FormService} from '../ngclient/form.service';

@Component({
    selector: 'servoy-designer',
    templateUrl: './servoydesigner.component.html'
})
export class ServoyDesignerComponent implements OnInit {

    mainForm : string;
    solutionName: string;
    private wsSession: WebsocketSession;
     
    constructor(private windowRef: WindowRefService, private websocketService: WebsocketService, private formService: FormService) { }

    ngOnInit() {
        let path : string= this.windowRef.nativeWindow.location.pathname;
        let formStart = path.indexOf('/form/') + 6;
        let formName = path.substring(formStart, path.indexOf('/', formStart));
        this.solutionName = path.substring(path.indexOf('/solution/') + 10, path.indexOf('/form/'));
        let clientnr =  path.substring(path.indexOf('/clientnr/') + 10);
        this.websocketService.setPathname('/rfb/angular/content/');
        this.wsSession = this.websocketService.connect('', [clientnr, formName, '1'], {solution : this.solutionName});
        this.wsSession.callService("$editor", "getData", {
          form: formName,
          solution: this.solutionName,
          ng2 : true
        }, false).then((data) => {
            const formState = JSON.parse(data)[ formName];
            this.formService.createFormCache( formName, formState, null);
            this.mainForm = formName;
        });
    }

}