import { Subject } from 'rxjs/Subject';
import "rxjs/add/operator/filter"

export class CustomEventEmitter {
    private subject = new Subject<CustomEvent>();
    
    public addEventListener(name:string, listener:(event:CustomEvent)=>any) {
        this.subject.filter(event=> event.name == name).subscribe(listener);
    }
    
    public dispatchEvent(event:CustomEvent) {
        this.subject.next(event);
    }
}

export class CustomEvent {
    constructor(public readonly name:string) {
    }
}