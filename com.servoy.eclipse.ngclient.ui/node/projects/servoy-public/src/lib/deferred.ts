export interface IDeferred<T> {
    readonly promise: Promise<T>;
    reject( reason: unknown ): void;
    resolve( value: T ): void;
}

export class Deferred <T> implements IDeferred<T> {
    public readonly promise: Promise<T>;
    private _resolve: ( value: T ) => void;
    private _reject: ( reason: unknown ) => void;
    constructor() {
        this.promise = new Promise<T>(( resolve, reject ) => {
            this._reject = reject;
            this._resolve = resolve;
        } );
    }

    public reject( reason: unknown ) {
        this._reject( reason );
    }

    public resolve( value: T ) {
        this._resolve( value );
    }
}
