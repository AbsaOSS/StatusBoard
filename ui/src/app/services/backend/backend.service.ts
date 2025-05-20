import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MultiApiResponse, SingleApiResponse } from '../../models/api-response';
import { RawStatus } from '../../models/raw-status';
import { RefinedStatus } from '../../models/refined-status';
import { ServiceConfiguration } from '../../models/service-configuration';
import { ServiceConfigurationReference } from '../../models/service-configuration-reference';

@Injectable({
  providedIn: 'root',
})
export class BackendService {
  private readonly apiStatusesUrl = `${environment.apiUrl}/api/v1/statuses`;
  private readonly apiConfigurationsUrl = `${environment.apiUrl}/api/v1/configurations`;

  constructor(private http: HttpClient) {}

  getConfiguration(env: string, serviceName: string): Observable<ServiceConfiguration> {
    return this.http
      .get<SingleApiResponse<ServiceConfiguration>>(`${this.apiConfigurationsUrl}/${env}/${serviceName}`)
      .pipe(map(response => response.record));
  }

  getConfigurations(includeHidden: boolean = false): Observable<ServiceConfiguration[]> {
    return this.http
      .get<MultiApiResponse<ServiceConfiguration>>(`${this.apiConfigurationsUrl}?include-hidden=${includeHidden}`)
      .pipe(map(response => response.records));
  }

  getDependencies(env: string, serviceName: string): Observable<ServiceConfigurationReference[]> {
    return this.http
      .get<MultiApiResponse<ServiceConfigurationReference>>(`${this.apiConfigurationsUrl}/${env}/${serviceName}/dependencies`)
      .pipe(map(apiResponse => apiResponse.records));
  }

  getDependents(env: string, serviceName: string): Observable<ServiceConfigurationReference[]> {
    return this.http
      .get<MultiApiResponse<ServiceConfigurationReference>>(`${this.apiConfigurationsUrl}/${env}/${serviceName}/dependents`)
      .pipe(map(apiResponse => apiResponse.records));
  }

  getLatestStatuses(): Observable<RefinedStatus[]> {
    return this.http
      .get<MultiApiResponse<RefinedStatus>>(this.apiStatusesUrl)
      .pipe(map(apiResponse => apiResponse.records.map(record => this.fixRawStatus(record))));
  }

  getServiceHistory(env: string, serviceName: string): Observable<RefinedStatus[]> {
    return this.http
      .get<MultiApiResponse<RefinedStatus>>(`${this.apiStatusesUrl}/${env}/${serviceName}`)
      .pipe(map(apiResponse => apiResponse.records.map(record => this.fixRawStatus(record))));
  }

  // Parse from JSON to object is partial, RawStatus has custom parsing rules and in JSON appears as a string
  private fixRawStatus(status: RefinedStatus): RefinedStatus {
    return Object.assign({}, status, { status: new RawStatus(status.status as unknown as string) });
  }
}
