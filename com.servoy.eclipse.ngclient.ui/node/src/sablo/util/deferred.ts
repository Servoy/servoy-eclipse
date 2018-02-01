export class Deferred {
    public readonly promise: Promise<any>;
    private _resolve: ( value ) => void
    private _reject: ( reason ) => void;
    constructor() {
        this.promise = new Promise(( resolve, reject ) => {
            this._reject = reject;
            this._resolve = resolve;
        } )
    }

    public reject( reason ) {
        this._reject( reason );
    }

    public resolve( value ) {
        this._resolve( value );
    }
}