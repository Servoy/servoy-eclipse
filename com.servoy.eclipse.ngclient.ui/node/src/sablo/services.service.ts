import { Injectable } from '@angular/core';

import { ConverterService } from './converter.service'

@Injectable()
export class ServicesService {
    private serviceProvider: ServiceProvider = new VoidServiceProvider();

    constructor(private converterService:ConverterService) {
    }

    private serviceScopesConversionInfo = {};

    public setServiceProvider( serviceProvider: ServiceProvider ) {
        if ( serviceProvider == null ) this.serviceProvider = new VoidServiceProvider();
        else this.serviceProvider = serviceProvider;
    }
    public getServiceProvider(): ServiceProvider {
        return this.serviceProvider;
    }

    public callServiceApi( service ) {
        var serviceInstance = this.getServiceProvider().getService( service.name );
        if ( serviceInstance
            && serviceInstance[service.call] ) {
            // responseValue keeps last services call return value
            return serviceInstance[service.call].apply( serviceInstance, service.args );
        }
    }

    public updateServiceScopes( services, conversionInfo ) {
        for ( var servicename in services ) {
            // current model
            var service = this.serviceProvider.getService( servicename );
            if ( service ) {
                var serviceData = services[servicename];


                for ( var key in serviceData ) {
                    if ( conversionInfo && conversionInfo[servicename] && conversionInfo[servicename][key] ) {
                        // convert property, remember type for when a client-server conversion will be needed
                        if ( !this.serviceScopesConversionInfo[servicename] ) this.serviceScopesConversionInfo[servicename] = {};
                        serviceData[key] =this. converterService.convertFromServerToClient( serviceData[key], conversionInfo[servicename][key], service[key], service,function() { return service})

                        // TODO datachange should be through EventEmitter...
                        //                        if ((serviceData[key] !== serviceScope.model[key] || this.serviceScopesConversionInfo[servicename][key] !== conversionInfo[servicename][key]) && serviceData[key]
                        //                        && serviceData[key][$sabloConverters.INTERNAL_IMPL] && serviceData[key][$sabloConverters.INTERNAL_IMPL].setChangeNotifier) {
                        //                            serviceData[key][$sabloConverters.INTERNAL_IMPL].setChangeNotifier(getChangeNotifier(servicename, key));
                        //                        }
                        this.serviceScopesConversionInfo[servicename][key] = conversionInfo[servicename][key];
                    } else if ( this.serviceScopesConversionInfo[servicename] && this.serviceScopesConversionInfo[servicename][key] ) {
                        delete this.serviceScopesConversionInfo[servicename][key];
                    }

                    service.model[key] = serviceData[key];
                }
            }
        }
    }
}


export interface ServiceProvider {
    getService( name: string );
}

class VoidServiceProvider implements ServiceProvider {
    getService( name ) {
        return null;
    }
}