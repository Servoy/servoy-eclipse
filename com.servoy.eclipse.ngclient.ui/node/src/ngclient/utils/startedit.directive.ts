import { Directive , Input , HostListener, ViewContainerRef} from '@angular/core';
import {LoggerService, LoggerFactory} from '../../sablo/logger.service'

@Directive({
  selector: '[svyStartEdit]'
})
export class StartEditDirective {

  @Input('svyStartEdit') dataProviderID : string;

  private log: LoggerService;
  private field: any;
  
  public constructor(private logFactory:LoggerFactory, private viewContainer: ViewContainerRef) {
    this.log = logFactory.getLogger("StartEditDirective");
    if(this.viewContainer['_view'] != undefined && this.viewContainer['_view']['component'] != undefined) {
      this.field = this.viewContainer['_view']['component'];
    }
    else {
      this.log.error("Can't find field for startEdit");
    }
  }

  @HostListener('focus',['$event']) onFocus(e:FocusEvent) {
    if(this.field != undefined && this.field.servoyApi && this.dataProviderID != undefined) {
      this.field.servoyApi.startEdit(this.dataProviderID);
    }
    else {
      this.log.error("Can't call startEdit, missing servoyApi and dataProviderID for field " + this.field);
    }
  }
}
