import { Injectable,EventEmitter } from '@angular/core';

import {Observable,} from 'rxjs/Observable';
import "rxjs/add/operator/filter"

@Injectable()
export class WebsocketService {
    public readonly  messages: EventEmitter<{[property:string]:Object}>;
    
    constructor() {
      this.messages  =  new EventEmitter()
      
      // test data
      var me = this;
    var timer =  function() {
         me.messages.next({formname:"test", componentname:"text1",property:"dataprovider",value:Math.random()*100})
           me.messages.next({service:"testService",property:"avalue",value:Math.random()*100})
            me.messages.next({service:"testService",call:"myfunc",args:["rob","johan"]})
         setTimeout(timer, 3000);
     }
     setTimeout(timer, 3000);

  }
  
  public sendChanges(formname:string,componentname:string,property:string,value:object) {
      console.log(formname + "," + componentname + "," + property + "," +  value);
  }

  public executeEvent(formname:string, componentname:string, handler:string,args:IArguments) {
      console.log(formname + "," + componentname + ", executing: " + handler + " with values: " + JSON.stringify(args));
  }
}

