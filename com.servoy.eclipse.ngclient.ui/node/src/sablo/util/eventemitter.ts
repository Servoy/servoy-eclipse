import { Subject } from 'rxjs';
import { filter } from 'rxjs/operators';

export class CustomEventEmitter {
    private subject = new Subject<CustomEvent>();

    public addEventListener(name: string, listener: (event: CustomEvent) => any) {
        this.subject.pipe(filter(event=> event.name == name)).subscribe(listener);
    }

    public dispatchEvent(event: CustomEvent) {
        this.subject.next(event);
    }
}

export class CustomEvent {
    constructor(public readonly name: string) {
    }
}
