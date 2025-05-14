import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ServiceConfiguration } from '../../models/service-configuration';
import { of } from 'rxjs';
import { RepositoryService } from '../repository/repository.service';
import { RefreshService } from './refresh.service';
import { BackendService } from '../backend/backend.service';
import { makeTestConfiguration, makeTestStatus } from '../../test.data';
import { ServiceConfigurationReference } from '../../models/service-configuration-reference';

describe('CardService', () => {
  let service: RefreshService;
  let backendServiceMock: jest.Mocked<BackendService>;
  let repositoryServiceMock: jest.Mocked<RepositoryService>;

  const testSvcs = ['0', '1', '2', '3', '4', '5', '6'].map(mark => makeTestConfiguration(`TestSvc${mark}`));
  const testSvcRefs: ServiceConfigurationReference[] = testSvcs.map(testSvc => {
    return { environment: testSvc.env, service: testSvc.name };
  });
  const testStatusesA = testSvcs.map(testSvc => makeTestStatus(testSvc.name, `GREEN(A-${testSvc.name})`));
  const testStatusesB = testSvcs.map(testSvc => makeTestStatus(testSvc.name, `GREEN(B-${testSvc.name})`));

  beforeEach(() => {
    backendServiceMock = {
      getConfiguration: jest.fn(),
      getConfigurations: jest.fn(),
      getDependencies: jest.fn(),
      getDependents: jest.fn(),
      getLatestStatuses: jest.fn(),
      getServiceHistory: jest.fn(),
    } as never;

    let mockStorage: ServiceConfiguration[] = [];
    repositoryServiceMock = {
      isHistoryInitialized: jest.fn(),
      getVisibleConfigurations: jest.fn(() => mockStorage),
      setVisibleConfigurations: jest.fn((data: ServiceConfiguration[]) => (mockStorage = data)),
      setConfiguration: jest.fn(),
      setDependencies: jest.fn(),
      setDependents: jest.fn(),
      storeStatus: jest.fn(),
      storeHistory: jest.fn(),
    } as never;

    TestBed.configureTestingModule({
      providers: [
        RefreshService,
        { provide: BackendService, useValue: backendServiceMock },
        { provide: RepositoryService, useValue: repositoryServiceMock },
      ],
    });
    service = TestBed.inject(RefreshService);
  });

  it('initialRefresh should set visible configurations', fakeAsync(() => {
    backendServiceMock.getConfigurations.mockImplementation(() => of([testSvcs[0], testSvcs[1]]));
    backendServiceMock.getServiceHistory.mockImplementation(() => of([]));
    repositoryServiceMock.isHistoryInitialized.mockImplementation(() => false);

    service.initialRefresh().subscribe(() => {
      expect(repositoryServiceMock.setVisibleConfigurations).toHaveBeenCalledWith([testSvcs[0], testSvcs[1]]);
    });
    tick();
  }));

  it('initialRefresh should not fetch latest nor store individual states', fakeAsync(() => {
    backendServiceMock.getConfigurations.mockImplementation(() => of([testSvcs[0], testSvcs[1]]));
    backendServiceMock.getServiceHistory.mockImplementation(() => of([]));
    repositoryServiceMock.isHistoryInitialized.mockImplementation(() => false);

    service.initialRefresh().subscribe(() => {
      expect(backendServiceMock.getLatestStatuses).not.toHaveBeenCalled();
      expect(repositoryServiceMock.storeStatus).not.toHaveBeenCalled();
    });
    tick();
  }));

  it('initialRefresh should fetch and store histories', fakeAsync(() => {
    backendServiceMock.getConfigurations.mockImplementation(() => of([testSvcs[0], testSvcs[1]]));
    backendServiceMock.getServiceHistory.mockImplementation((env: string, serviceName: string) =>
      serviceName == testSvcs[0].name ? of([testStatusesA[0], testStatusesB[0]]) : of([testStatusesA[1], testStatusesB[1]])
    );
    repositoryServiceMock.isHistoryInitialized.mockImplementation(() => false);

    service.initialRefresh().subscribe(() => {
      expect(backendServiceMock.getServiceHistory).toHaveBeenCalledTimes(2);
      expect(backendServiceMock.getServiceHistory).toHaveBeenCalledWith(testSvcs[0].env, testSvcs[0].name);
      expect(backendServiceMock.getServiceHistory).toHaveBeenCalledWith(testSvcs[1].env, testSvcs[1].name);
      expect(repositoryServiceMock.storeHistory).toHaveBeenCalledTimes(2);
      expect(repositoryServiceMock.storeHistory).toHaveBeenCalledWith(testSvcs[0].env, testSvcs[0].name, [
        testStatusesA[0],
        testStatusesB[0],
      ]);
      expect(repositoryServiceMock.storeHistory).toHaveBeenCalledWith(testSvcs[1].env, testSvcs[1].name, [
        testStatusesA[1],
        testStatusesB[1],
      ]);
    });
    tick();
  }));

  it('incrementalRefresh should set visible configurations', fakeAsync(() => {
    backendServiceMock.getConfigurations.mockImplementation(() => of([testSvcs[0], testSvcs[1]]));
    backendServiceMock.getLatestStatuses.mockImplementation(() => of([]));
    repositoryServiceMock.isHistoryInitialized.mockImplementation(() => true);

    service.incrementalRefresh().subscribe(() => {
      expect(repositoryServiceMock.setVisibleConfigurations).toHaveBeenCalledWith([testSvcs[0], testSvcs[1]]);
    });
    tick();
  }));

  it('incrementalRefresh should fetch and store latest', fakeAsync(() => {
    backendServiceMock.getConfigurations.mockImplementation(() => of([testSvcs[0], testSvcs[1]]));
    backendServiceMock.getLatestStatuses.mockImplementation(() => of([testStatusesA[0], testStatusesB[1]]));
    repositoryServiceMock.isHistoryInitialized.mockImplementation(() => true);

    service.incrementalRefresh().subscribe(() => {
      expect(backendServiceMock.getLatestStatuses).toHaveBeenCalled();
      expect(repositoryServiceMock.storeStatus).toHaveBeenCalledTimes(2);
      expect(repositoryServiceMock.storeStatus).toHaveBeenCalledWith(testStatusesA[0]);
      expect(repositoryServiceMock.storeStatus).toHaveBeenCalledWith(testStatusesB[1]);
    });
    tick();
  }));

  it('incrementalRefresh should not fetch nor store histories (when all is known)', fakeAsync(() => {
    backendServiceMock.getConfigurations.mockImplementation(() => of([testSvcs[0], testSvcs[1]]));
    backendServiceMock.getLatestStatuses.mockImplementation(() => of([testStatusesA[0], testStatusesB[1]]));
    repositoryServiceMock.isHistoryInitialized.mockImplementation(() => true);

    service.incrementalRefresh().subscribe(() => {
      expect(backendServiceMock.getServiceHistory).not.toHaveBeenCalled();
      expect(repositoryServiceMock.storeHistory).not.toHaveBeenCalled();
    });
    tick();
  }));

  it('incrementalRefresh should fetch and store history for newly appeared services', fakeAsync(() => {
    backendServiceMock.getConfigurations.mockImplementation(() => of([testSvcs[0], testSvcs[1]]));
    backendServiceMock.getLatestStatuses.mockImplementation(() => of([testStatusesA[0], testStatusesB[1]]));
    backendServiceMock.getServiceHistory.mockImplementation(() => of([testStatusesA[0], testStatusesB[0]]));
    repositoryServiceMock.isHistoryInitialized.mockImplementation((env: string, serviceName: string) => serviceName == testSvcs[1].name);

    service.incrementalRefresh().subscribe(() => {
      // FETCH history ONLY for the missing one
      expect(backendServiceMock.getServiceHistory).toHaveBeenCalledTimes(1);
      expect(backendServiceMock.getServiceHistory).toHaveBeenCalledWith(testSvcs[0].env, testSvcs[0].name);
      // STORE history ONLY for the missing one
      expect(repositoryServiceMock.storeHistory).toHaveBeenCalledTimes(1);
      expect(repositoryServiceMock.storeHistory).toHaveBeenCalledWith(testSvcs[0].env, testSvcs[0].name, [
        testStatusesA[0],
        testStatusesB[0],
      ]);
    });
    tick();
  }));

  it('refreshForGraph should fetch and store nodes recursively (evaluate only once on common nodes)', fakeAsync(() => {
    backendServiceMock.getConfiguration.mockImplementation((env: string, serviceName: string) =>
      of(testSvcs.find(svc => svc.name === serviceName)!)
    );
    backendServiceMock.getDependencies.mockImplementation((env: string, serviceName: string) =>
      serviceName == testSvcs[0].name
        ? of([testSvcRefs[1], testSvcRefs[2]])
        : serviceName == testSvcs[1].name
          ? of([testSvcRefs[2], testSvcRefs[3]])
          : of([])
    );
    backendServiceMock.getDependents.mockImplementation((env: string, serviceName: string) =>
      serviceName == testSvcs[0].name
        ? of([testSvcRefs[4], testSvcRefs[5]])
        : serviceName == testSvcs[4].name
          ? of([testSvcRefs[5], testSvcRefs[6]])
          : of([])
    );
    repositoryServiceMock.isHistoryInitialized.mockImplementation(() => true);

    service.refreshForGraph(testSvcs[0].env, testSvcs[0].name).subscribe(() => {
      expect(backendServiceMock.getDependencies).toHaveBeenCalledTimes(4);
      expect(backendServiceMock.getDependencies).toHaveBeenCalledWith(testSvcs[0].env, testSvcs[0].name);
      expect(backendServiceMock.getDependencies).toHaveBeenCalledWith(testSvcs[1].env, testSvcs[1].name);
      expect(backendServiceMock.getDependencies).toHaveBeenCalledWith(testSvcs[2].env, testSvcs[2].name);
      expect(backendServiceMock.getDependencies).toHaveBeenCalledWith(testSvcs[3].env, testSvcs[3].name);
      expect(repositoryServiceMock.setDependencies).toHaveBeenCalledTimes(4);
      expect(repositoryServiceMock.setDependencies).toHaveBeenCalledWith(testSvcs[0].env, testSvcs[0].name, [
        testSvcRefs[1],
        testSvcRefs[2],
      ]);
      expect(repositoryServiceMock.setDependencies).toHaveBeenCalledWith(testSvcs[1].env, testSvcs[1].name, [
        testSvcRefs[2],
        testSvcRefs[3],
      ]);
      expect(repositoryServiceMock.setDependencies).toHaveBeenCalledWith(testSvcs[2].env, testSvcs[2].name, []);
      expect(repositoryServiceMock.setDependencies).toHaveBeenCalledWith(testSvcs[3].env, testSvcs[3].name, []);
      expect(backendServiceMock.getDependents).toHaveBeenCalledTimes(4);
      expect(backendServiceMock.getDependents).toHaveBeenCalledWith(testSvcs[0].env, testSvcs[0].name);
      expect(backendServiceMock.getDependents).toHaveBeenCalledWith(testSvcs[4].env, testSvcs[4].name);
      expect(backendServiceMock.getDependents).toHaveBeenCalledWith(testSvcs[5].env, testSvcs[5].name);
      expect(backendServiceMock.getDependents).toHaveBeenCalledWith(testSvcs[6].env, testSvcs[6].name);
      expect(repositoryServiceMock.setDependents).toHaveBeenCalledTimes(4);
      expect(repositoryServiceMock.setDependents).toHaveBeenCalledWith(testSvcs[0].env, testSvcs[0].name, [testSvcRefs[4], testSvcRefs[5]]);
      expect(repositoryServiceMock.setDependents).toHaveBeenCalledWith(testSvcs[4].env, testSvcs[4].name, [testSvcRefs[5], testSvcRefs[6]]);
      expect(repositoryServiceMock.setDependents).toHaveBeenCalledWith(testSvcs[5].env, testSvcs[5].name, []);
      expect(repositoryServiceMock.setDependents).toHaveBeenCalledWith(testSvcs[6].env, testSvcs[6].name, []);
    });
    tick();
  }));

  it('refreshForGraph should fetch and store history for newly discovered services (init history only once)', fakeAsync(() => {
    backendServiceMock.getConfiguration.mockImplementation((env: string, serviceName: string) =>
      of(testSvcs.find(svc => svc.name === serviceName)!)
    );
    backendServiceMock.getDependencies.mockImplementation((env: string, serviceName: string) =>
      serviceName == testSvcs[0].name
        ? of([testSvcRefs[1], testSvcRefs[2]])
        : serviceName == testSvcs[1].name
          ? of([testSvcRefs[2], testSvcRefs[3]])
          : of([])
    );
    backendServiceMock.getDependents.mockImplementation((env: string, serviceName: string) =>
      serviceName == testSvcs[0].name
        ? of([testSvcRefs[4], testSvcRefs[5]])
        : serviceName == testSvcs[4].name
          ? of([testSvcRefs[5], testSvcRefs[6]])
          : of([])
    );
    backendServiceMock.getServiceHistory.mockImplementation((env: string, serviceName: string) =>
      serviceName == testSvcs[2].name ? of([testStatusesA[2], testStatusesB[2]]) : of([testStatusesA[5], testStatusesB[5]])
    );
    repositoryServiceMock.isHistoryInitialized.mockImplementation(
      (env: string, serviceName: string) => serviceName != testSvcs[2].name && serviceName != testSvcs[5].name
    );

    service.refreshForGraph(testSvcs[0].env, testSvcs[0].name).subscribe(() => {
      expect(backendServiceMock.getServiceHistory).toHaveBeenCalledTimes(2);
      expect(backendServiceMock.getServiceHistory).toHaveBeenCalledWith(testSvcs[2].env, testSvcs[2].name);
      expect(backendServiceMock.getServiceHistory).toHaveBeenCalledWith(testSvcs[5].env, testSvcs[5].name);
      expect(repositoryServiceMock.storeHistory).toHaveBeenCalledTimes(2);
      expect(repositoryServiceMock.storeHistory).toHaveBeenCalledWith(testSvcs[2].env, testSvcs[2].name, [
        testStatusesA[2],
        testStatusesB[2],
      ]);
      expect(repositoryServiceMock.storeHistory).toHaveBeenCalledWith(testSvcs[5].env, testSvcs[5].name, [
        testStatusesA[5],
        testStatusesB[5],
      ]);
    });
    tick();
  }));

  it('refreshForGraph should fetch and store fresh configuration for traversed services', fakeAsync(() => {
    backendServiceMock.getConfiguration.mockImplementation((env: string, serviceName: string) =>
      of(testSvcs.find(svc => svc.name === serviceName)!)
    );
    backendServiceMock.getDependencies.mockImplementation((env: string, serviceName: string) =>
      serviceName == testSvcs[0].name
        ? of([testSvcRefs[1], testSvcRefs[2]])
        : serviceName == testSvcs[1].name
          ? of([testSvcRefs[2], testSvcRefs[3]])
          : of([])
    );
    backendServiceMock.getDependents.mockImplementation((env: string, serviceName: string) =>
      serviceName == testSvcs[0].name
        ? of([testSvcRefs[4], testSvcRefs[5]])
        : serviceName == testSvcs[4].name
          ? of([testSvcRefs[5], testSvcRefs[6]])
          : of([])
    );
    repositoryServiceMock.isHistoryInitialized.mockImplementation(() => true);

    service.refreshForGraph(testSvcs[0].env, testSvcs[0].name).subscribe(() => {
      expect(backendServiceMock.getConfiguration).toHaveBeenCalledTimes(8); // +1 for root (testSvc0) as it appears in both, dependency/dependent branch
      expect(backendServiceMock.getConfiguration).toHaveBeenCalledWith(testSvcs[0].env, testSvcs[0].name);
      expect(backendServiceMock.getConfiguration).toHaveBeenCalledWith(testSvcs[1].env, testSvcs[1].name);
      expect(backendServiceMock.getConfiguration).toHaveBeenCalledWith(testSvcs[2].env, testSvcs[2].name);
      expect(backendServiceMock.getConfiguration).toHaveBeenCalledWith(testSvcs[3].env, testSvcs[3].name);
      expect(backendServiceMock.getConfiguration).toHaveBeenCalledWith(testSvcs[4].env, testSvcs[4].name);
      expect(backendServiceMock.getConfiguration).toHaveBeenCalledWith(testSvcs[5].env, testSvcs[5].name);
      expect(backendServiceMock.getConfiguration).toHaveBeenCalledWith(testSvcs[6].env, testSvcs[6].name);
      expect(repositoryServiceMock.setConfiguration).toHaveBeenCalledTimes(8); // +1 for root (testSvc0) as it appears in both, dependency/dependent branch
      expect(repositoryServiceMock.setConfiguration).toHaveBeenCalledWith(testSvcs[0]);
      expect(repositoryServiceMock.setConfiguration).toHaveBeenCalledWith(testSvcs[1]);
      expect(repositoryServiceMock.setConfiguration).toHaveBeenCalledWith(testSvcs[2]);
      expect(repositoryServiceMock.setConfiguration).toHaveBeenCalledWith(testSvcs[3]);
      expect(repositoryServiceMock.setConfiguration).toHaveBeenCalledWith(testSvcs[4]);
      expect(repositoryServiceMock.setConfiguration).toHaveBeenCalledWith(testSvcs[5]);
      expect(repositoryServiceMock.setConfiguration).toHaveBeenCalledWith(testSvcs[6]);
    });
    tick();
  }));
});
