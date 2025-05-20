import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { BackendService } from './backend.service';
import { ServiceConfiguration } from '../../models/service-configuration';
import { RefinedStatus } from '../../models/refined-status';
import { RawStatus } from '../../models/raw-status';
import { MultiApiResponse, SingleApiResponse } from '../../models/api-response';
import { makeTestConfiguration, makeTestStatus } from '../../test.data';
import { ServiceConfigurationReference } from '../../models/service-configuration-reference';

describe('BackendService', () => {
  let service: BackendService;
  let httpMock: HttpTestingController;

  const testSvcA = makeTestConfiguration('TestSvcAlpha');
  const testSvcB = makeTestConfiguration('TestSvcBravo');
  const testStatusA1 = makeTestStatus('TestSvcAlpha', 'GREEN(1)');
  const testStatusB1 = makeTestStatus('TestSvcBravo', 'AMBER(1)');
  const testStatusA2 = makeTestStatus('TestSvcAlpha', 'GREEN(2)');
  const testStatusA1_raw = makeTestStatus('TestSvcAlpha');
  testStatusA1_raw.status = 'GREEN(1)' as unknown as RawStatus; // before refined parsing
  const testStatusB1_raw = makeTestStatus('TestSvcBravo');
  testStatusB1_raw.status = 'AMBER(1)' as unknown as RawStatus; // before refined parsing
  const testStatusA2_raw = makeTestStatus('TestSvcAlpha');
  testStatusA2_raw.status = 'GREEN(2)' as unknown as RawStatus; // before refined parsing

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [BackendService],
    });
    service = TestBed.inject(BackendService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getConfiguration should pass response from backend', fakeAsync(() => {
    service.getConfiguration(testSvcA.env, testSvcA.name).subscribe(configuration => {
      expect(configuration).toEqual(testSvcA);
    });

    const request = httpMock.expectOne(`${environment.apiUrl}/api/v1/configurations/${testSvcA.env}/${testSvcA.name}`);
    expect(request.request.method).toBe('GET');
    request.flush({ record: testSvcA } as SingleApiResponse<ServiceConfiguration>);

    tick();
  }));

  it('getConfigurations should pass response from backend', fakeAsync(() => {
    service.getConfigurations().subscribe(configurations => {
      expect(configurations).toEqual([testSvcA, testSvcB]);
    });

    const request = httpMock.expectOne(`${environment.apiUrl}/api/v1/configurations?include-hidden=false`);
    expect(request.request.method).toBe('GET');
    request.flush({ records: [testSvcA, testSvcB] } as MultiApiResponse<ServiceConfiguration>);

    tick();
  }));

  it('getLatestStatuses should pass response from backend with properly parsed RawStatus', fakeAsync(() => {
    service.getLatestStatuses().subscribe(statuses => {
      expect(statuses).toEqual([testStatusA1, testStatusB1]);
    });

    const request = httpMock.expectOne(`${environment.apiUrl}/api/v1/statuses`);
    expect(request.request.method).toBe('GET');
    request.flush({ records: [testStatusA1_raw, testStatusB1_raw] } as MultiApiResponse<RefinedStatus>);

    tick();
  }));

  it('getServiceHistory should pass response from backend with properly parsed RawStatus', fakeAsync(() => {
    service.getServiceHistory(testSvcA.env, testSvcA.name).subscribe(statuses => {
      expect(statuses).toEqual([testStatusA1, testStatusA2]);
    });

    const request = httpMock.expectOne(`${environment.apiUrl}/api/v1/statuses/${testSvcA.env}/${testSvcA.name}`);
    expect(request.request.method).toBe('GET');
    request.flush({
      records: [testStatusA1_raw, testStatusA2_raw],
    } as MultiApiResponse<RefinedStatus>);

    tick();
  }));

  it('getDependencies should pass response from backend', fakeAsync(() => {
    const references: ServiceConfigurationReference[] = [{ environment: 'testEnv', service: 'testServiceDependsOnA' }];
    service.getDependencies(testSvcA.env, testSvcA.name).subscribe(dependencies => {
      expect(dependencies).toEqual(references);
    });

    const request = httpMock.expectOne(`${environment.apiUrl}/api/v1/configurations/${testSvcA.env}/${testSvcA.name}/dependencies`);
    expect(request.request.method).toBe('GET');
    request.flush({ records: references } as MultiApiResponse<ServiceConfigurationReference>);

    tick();
  }));

  it('getDependents should pass response from backend', fakeAsync(() => {
    const references: ServiceConfigurationReference[] = [{ environment: 'testEnv', service: 'testServiceADependsOn' }];
    service.getDependents(testSvcA.env, testSvcA.name).subscribe(dependents => {
      expect(dependents).toEqual(references);
    });

    const request = httpMock.expectOne(`${environment.apiUrl}/api/v1/configurations/${testSvcA.env}/${testSvcA.name}/dependents`);
    expect(request.request.method).toBe('GET');
    request.flush({ records: references } as MultiApiResponse<ServiceConfigurationReference>);
  }));
});
