import { Injectable } from '@angular/core';
import { WebsocketService } from './websocket.service';
import { Subject, Observable, Observer } from 'rxjs';
import { map, share } from "rxjs/operators";
import * as Rx from 'rxjs'

export const PACKAGE_TYPE_WEB_COMPONENT = "Web-Component";
export const PACKAGE_TYPE_WEB_SERVICE = "Web-Service";
export const PACKAGE_TYPE_WEB_LAYOUT = "Web-Layout";
export const PACKAGE_TYPE_MODULE = "Solution";
export const PACKAGE_TYPE_SOLUTION = "Solution-Main";

export const PACKAGE_TYPE_TO_TITLE_MAP = {
  "Web-Component": "Components",
  "Web-Service": "Services",
  "Web-Layout": "Layouts",
  "Solution": "Modules",
  "Solution-Main": "Solutions"
}

export const ALL_PACKAGE_TYPES = [ PACKAGE_TYPE_WEB_COMPONENT, PACKAGE_TYPE_WEB_SERVICE, PACKAGE_TYPE_WEB_LAYOUT, PACKAGE_TYPE_MODULE, PACKAGE_TYPE_SOLUTION ];

interface Message {
  method: string;
  data?: any;
  package?: Package;
  url?: string;
  solution?: string;
  name?: string;
  values?: Repository
}

export interface Release {
  servoyVersion?: string;
  url: string;
  version: string;
}

export interface Package {
  activeSolution: string;
  description: string;
  displayName: string;
  icon: string;
  installed: string;
  installedIsWPA: boolean;
  installing: boolean;
  name: string;
  packageType: string;
  releases: Release[];
  removing: boolean;
  selected: string;
  sourceUrl: string;
  top: boolean;
  wikiUrl: string;
}

export interface PackagesInfo {
  packageType: string;
  packages: Package[];
}

export interface Repository {
  name: string;
  selected?: boolean;
  url?: string;
}

interface PackagesAndRepositories {
  packages: Package[];
  repositories: Repository[];
}

@Injectable()
export class WpmService {

  messages: Subject<Message>;

  packagesObservable: Observable<PackagesInfo>;
  packagesObserver: Observer<PackagesInfo>;

  solutions: string[];

  repositoriesObservable: Observable<Repository[]>;
  repositoriesObserver: Observer<Repository[]>;

  needRefresh: boolean = false;

  contentAvailable: boolean = true;

  constructor(wsService: WebsocketService) {
    const loc = window.location;
    const uri = "ws://"+loc.host+"/wpm/angular2/websocket";
    //const uri = "ws://localhost:8080/wpm/angular2/websocket";
    const webSocketConnection = wsService.connect(uri);
    webSocketConnection.open.subscribe(() => {
      this.onConnectionOpen();
    });
    this.messages = <Subject<Message>>webSocketConnection.messages.pipe(map(
      (response: MessageEvent): Message => {
        const data = JSON.parse(response.data);
        return {
          method: data.method,
          data: data.result
        };
      }));

    this.messages.subscribe(m => {
      this[m.method](m.data);
    });


    this.packagesObservable = Rx.Observable.create((obs: Rx.Observer<PackagesInfo>) => {
      this.packagesObserver = obs;
    }).pipe(share());

    this.repositoriesObservable = Rx.Observable.create((obs: Rx.Observer<Repository[]>) => {
      this.repositoriesObserver = obs;
    }).pipe(share());
  }

  /**
   * Callback for when connection is established
   */
  onConnectionOpen() {

    const requestAllInstalledPackagesCommand: Message = { method: "requestAllInstalledPackages" };
    // get solution name
    const solutionName = decodeURIComponent((new RegExp('[?|&]solution=' + '([^&;]+?)(&|#|;|$)').exec(window.location.search)||[,""])[1].replace(/\+/g, '%20'))||null;
    if(solutionName) {
      requestAllInstalledPackagesCommand.solution = solutionName;
    }
    this.messages.next(requestAllInstalledPackagesCommand);

    this.callRemoteMethod("getSolutionList");
    this.callRemoteMethod("getRepositories");
  }

  /**
   * Call remote method
   * 
   * @param method name of the method
   */
  callRemoteMethod(method: string) {
    const command: Message = { method: method };
    this.messages.next(command);
  }

  getPackages(): Observable<PackagesInfo> {
    return this.packagesObservable;
  }

  install(p: Package) {
    p.installing = true;
    const command: Message = { method: "install", package: p };
    this.messages.next(command);
  }

  uninstall(p: Package) {
    p.removing = true;
    const command: Message = { method: "remove", package: p };
    this.messages.next(command);
  }

  showUrl(url: string) {
    const command: Message = { method: "showurl", url: url };
    this.messages.next(command);
  }

  getAllSolutions(): string[] {
    // TODO: should this return an observable?
    return this.solutions;
  }

  getActiveSolution(): string {
    if (this.solutions && this.solutions.length) {
      return this.solutions[this.solutions.length - 1];
    }
    return "";
  }

  getAllRepositories(): Observable<Repository[]> {
    return this.repositoriesObservable;
  }

  isNeedRefresh(): boolean {
    return this.needRefresh;
  }

  isContentAvailable(): boolean {
    return this.contentAvailable;
  }

  setNewSelectedRepository(repositoryName: string) {
    const command: Message = { method: "setSelectedRepository", name: repositoryName };
    this.clearPackages();
    this.messages.next(command);
  }

  addNewRepository(repository: Repository) {
    const command: Message = { method: "addRepository", values: repository };
    this.messages.next(command);
  }

  removeRepositoryWithName(repositoryName: string) {
    const command: Message = { method: "removeRepository", name: repositoryName };
    this.messages.next(command);
  }

  clearPackages() {
    if(this.packagesObserver) {
      for(let packageType of ALL_PACKAGE_TYPES) {
        this.packagesObserver.next({ packageType: packageType, packages: [] });  
      }
    }
  }

  /**
   * Remote method responses
   */

  requestAllInstalledPackages(packagesArray: Package[]) {
    const typeOfPackages: Map<string, Package[]> = new Map();

		for(let i = 0; i < packagesArray.length; i++) {
      if(!typeOfPackages.has(packagesArray[i].packageType)) {
        typeOfPackages.set(packagesArray[i].packageType, []);
      }
      const packages: Package[] = typeOfPackages.get(packagesArray[i].packageType);
      packages.push(packagesArray[i]);
    }

    if(typeOfPackages.size > 0) {
      if(this.packagesObserver) {
        // fill missing package types so they are refreshed in the view
        for(let packageType of ALL_PACKAGE_TYPES) {
          if(!typeOfPackages.has(packageType)) {
            typeOfPackages.set(packageType, []);
          }
        }
        typeOfPackages.forEach((pks, typ) => {
          this.packagesObserver.next({ packageType: typ, packages: pks });  
        });
      }
    }
  }

  getSolutionList(solutionsArray: string[]) {
    this.solutions = solutionsArray;
  }

  getRepositories(repositoriesArray: Repository[]) {
    this.repositoriesObserver.next(repositoriesArray);
  }

  refreshRemotePackages = function() {
		this.needRefresh = true;
  }
  
  contentNotAvailable = function() {
    this.contentAvailable = false;
  }

  installError = function() {
    this.contentAvailable = false;
  }

  addRepository(newPackagesAndRepositories: PackagesAndRepositories) {
    this.getRepositories(newPackagesAndRepositories.repositories)
    this.requestAllInstalledPackages(newPackagesAndRepositories.packages);
  }
  
  removeRepository(repositories: Repository[]) {
    this.getRepositories(repositories);
  }

  setSelectedRepository(newPackages: Package[]) {
    this.requestAllInstalledPackages(newPackages);
  }
  
}
