import { IConverter } from '../../sablo/converter.service';
import { WindowRefService } from '@servoy/public';

export class ClientFunctionConverter implements IConverter {

    constructor( private windowRef: WindowRefService) {
    }

    fromClientToServer(): string {
       return null; // client functions can be send back to the server.
    }
    fromServerToClient(serverSentData: string): () => any {
        if (serverSentData) {
            return (...args: any[]) => {
                const func = this.windowRef.nativeWindow['svyClientSideFunctions'][serverSentData] as (...argss: any[]) => any;
                if (func) return func( ...args);
            };
        }
        return null;
    }
}
