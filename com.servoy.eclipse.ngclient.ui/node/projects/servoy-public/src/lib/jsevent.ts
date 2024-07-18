export class JSEvent {
    public formName?: string;
    public elementName?: string;
    public svyType: string;
    public eventType: string;
    public modifiers?: number;
    public x?: number;
    public y?: number;
    public timestamp: number;
    public data? : unknown;
}

export interface EventLike {
    target: EventTarget;
    altKey?: boolean;
    shiftKey?: boolean;
    ctrlKey?: boolean;
    metaKey?: boolean;
    pageX? : number;
    pageY? : number;
}
