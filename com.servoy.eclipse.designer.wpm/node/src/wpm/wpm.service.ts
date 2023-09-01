import { Injectable } from '@angular/core';
import { Message, Package, PackagesAndRepositories, PackagesInfo, Repository, WebsocketService } from './websocket.service';
import { Observable, Observer, BehaviorSubject } from 'rxjs';
import { map, share } from 'rxjs/operators';
import { PackageList } from './content/content.component';

export const PACKAGE_TYPE_WEB_COMPONENT = 'Web-Component';
export const PACKAGE_TYPE_WEB_SERVICE = 'Web-Service';
export const PACKAGE_TYPE_WEB_LAYOUT = 'Web-Layout';
export const PACKAGE_TYPE_MODULE = 'Solution';
export const PACKAGE_TYPE_SOLUTION = 'Solution-Main';

export const PACKAGE_TYPE_TO_TITLE_MAP: {[key:string]:string;} = {
  'Web-Component': 'Components',
  'Web-Service': 'Services',
  'Web-Layout': 'Layouts',
  'Solution': 'Modules',
  'Solution-Main': 'Solutions'
}

export const ALL_PACKAGE_TYPES = [ PACKAGE_TYPE_WEB_COMPONENT, PACKAGE_TYPE_WEB_SERVICE, PACKAGE_TYPE_WEB_LAYOUT, PACKAGE_TYPE_MODULE, PACKAGE_TYPE_SOLUTION ];


@Injectable()
export class WpmService {

  messages: Observable<Message>;
  messageSender: Observer<Message>;

  packagesObservable: Observable<PackagesInfo>;
  packagesObserver: Observer<PackagesInfo>;

  solutions: string[];

  repositoriesObservable: Observable<Repository[]>;
  repositoriesObserver: Observer<Repository[]>;

  packageLists: BehaviorSubject<PackageList[]>;
  packageToBeRemoved: BehaviorSubject<Package>;
 url: URL;

  needRefresh = false;

  contentAvailable = true;

  constructor(wsService: WebsocketService) {
    const loc = window.location;
    this.url = new URL(loc.href);
    const uri = 'ws://'+loc.host+'/wpm/angular2/websocket';
    //const uri = "ws://localhost:8080/wpm/angular2/websocket";
    const webSocketConnection = wsService.connect(uri);
    webSocketConnection.open.subscribe(() => {
      this.onConnectionOpen();
    });
    this.messages = webSocketConnection.messageObservable.pipe(map(
      (response: MessageEvent): Message => {
        const data = JSON.parse(response.data as string) as {method: string; result: unknown}
        return {
          method: data.method,
          data: data.result
        };
      }));
    this.messageSender = webSocketConnection.messageSender;
    this.messages.subscribe(m => {
      // eslint-disable-next-line @typescript-eslint/no-unsafe-call
      this[m.method](m.data);
    });
    
    this.packagesObservable = new Observable((obs: Observer<PackagesInfo>) => {
      this.packagesObserver = obs;
    }).pipe(share());

    this.repositoriesObservable = new Observable((obs: Observer<Repository[]>) => {
      this.repositoriesObserver = obs;
    }).pipe(share());
    
    this.packageLists = new BehaviorSubject([] as Array<PackageList>);
    this.packageToBeRemoved = new BehaviorSubject({} as Package);
  }

  /**
   * Callback for when connection is established
   */
  onConnectionOpen() {

    const requestAllInstalledPackagesCommand: Message = { method: 'requestAllInstalledPackages' };
    // get solution name
    // eslint-disable-next-line no-sparse-arrays
    const solutionName = decodeURIComponent((new RegExp('[?|&]solution=' + '([^&;]+?)(&|#|;|$)').exec(window.location.search)||[,''])[1].replace(/\+/g, '%20'))||null;
    if(solutionName) {
      requestAllInstalledPackagesCommand.solution = solutionName;
    }
    this.messageSender.next(requestAllInstalledPackagesCommand);

    this.callRemoteMethod('getSolutionList');
    this.callRemoteMethod('getRepositories');
  }

  /**
   * Call remote method
   * 
   * @param method name of the method
   */
  callRemoteMethod(method: string) {
    const command: Message = { method: method };
    this.messageSender.next(command);
  }

  getPackages(): Observable<PackagesInfo> {
    return this.packagesObservable;
  }

  setPackageLists(packageLists: PackageList[]) {
    this.packageLists.next(packageLists);
  }

  install(p: Package) {
    p.installing = true;
    const command: Message = { method: 'install', package: p };
    this.messageSender.next(command);
  }

  uninstall(p: Package) {
    this.packageToBeRemoved.next(p);
    p.removing = true;
    const command: Message = { method: 'remove', package: p };
    this.messageSender.next(command);
  }

  showUrl(url: string) {
    const command: Message = { method: 'showurl', url: url };
    this.messageSender.next(command);
  }

  getAllSolutions(): string[] {
    // TODO: should this return an observable?
    return this.solutions;
  }

  getActiveSolution(): string {
    if (this.solutions && this.solutions.length) {
      return this.solutions[this.solutions.length - 1];
    }
    return '';
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
    const command: Message = { method: 'setSelectedRepository', name: repositoryName };
    this.clearPackages();
    this.messageSender.next(command);
  }

  addNewRepository(repository: Repository) {
    const command: Message = { method: 'addRepository', values: repository };
    this.messageSender.next(command);
  }

  removeRepositoryWithName(repositoryName: string) {
    const command: Message = { method: 'removeRepository', name: repositoryName };
    this.messageSender.next(command);
  }

  clearPackages() {
    if(this.packagesObserver) {
      for(const packageType of ALL_PACKAGE_TYPES) {
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
        for(const packageType of ALL_PACKAGE_TYPES) {
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

  refreshRemotePackages = () =>{
		this.needRefresh = true;
  }
  
  contentNotAvailable = () => {
    this.contentAvailable = false;
  }

  installError = () => {
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

  versionCompare(v1: string, v2: string): number {
	const av1 = v1.split('.');
	const av2 = v2.split('.');

    const sizeDiff = av2.length - av1.length;
    if (sizeDiff) {
		for (let i = 0; i < Math.abs(sizeDiff); i++) {
			if (sizeDiff > 0) av1.push('0');
			else av2.push('0');
		}
	}

	for (let i = 0; i < av1.length; i++) {
		if (!isNaN(Number(av1[i])) && !isNaN(Number(av2[i]))) {
			const ival1 = parseInt(av1[i]);
			const ival2 = parseInt(av2[i]);
			if (ival1 != ival2) return ival1 - ival2;
		}
		else if (av1[i] < av2[i]) return -1;
		else if (av1[i] > av2[i]) return 1;
	}

	return 0;
  }
  
  isDarkTheme(): boolean {
      return this.url.searchParams.get('darkTheme') === 'true';
  }
  
}
