import { Injectable } from '@angular/core';

@Injectable()
export class TestService {
    public avalue:number;

    constructor() {
    }
    
    public myfunc(name1:string,name2:string) {
        console.log(name1 + ", " + name2 + " , " + this.avalue);
    }
}