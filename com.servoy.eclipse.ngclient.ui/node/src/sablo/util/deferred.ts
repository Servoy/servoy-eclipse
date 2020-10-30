export interface IDeferred<T> {
    readonly promise: Promise<T>;
    reject( reason );
    resolve( value: T );
}

export class Deferred <T> implements IDeferred<T> {
    public readonly promise: Promise<T>;
    private _resolve: ( value ) => void;
    private _reject: ( reason ) => void;
    constructor() {
        this.promise = new Promise<T>(( resolve, reject ) => {
            this._reject = reject;
            this._resolve = resolve;
        } );
    }

    public reject( reason ) {
        this._reject( reason );
    }

    public resolve( value: T ) {
        this._resolve( value );
    }
}
