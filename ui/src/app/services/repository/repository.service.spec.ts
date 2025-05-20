import { TestBed } from '@angular/core/testing';
import { RepositoryService } from './repository.service';
import { makeTestConfiguration, makeTestStatus } from '../../test.data';

describe('RepositoryServiceService', () => {
  let service: RepositoryService;

  const testSvcA = makeTestConfiguration('TestSvcAlpha');
  const testSvcB = makeTestConfiguration('TestSvcBravo');
  const testStatusA10 = makeTestStatus('TestSvcAlpha', undefined, '1989-05-29T10:00:00Z', '1989-05-29T11:00:00Z');
  const testStatusB11 = makeTestStatus('TestSvcBravo', undefined, '1989-05-29T11:00:00Z', '1989-05-29T12:00:00Z');
  const testStatusA12 = makeTestStatus('TestSvcAlpha', undefined, '1989-05-29T12:00:00Z', '1989-05-29T13:00:00Z');

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [RepositoryService],
    });
    service = TestBed.inject(RepositoryService);
  });

  it('should provide configuration as received', () => {
    service.setConfiguration(testSvcA);
    expect(service.getConfiguration(testSvcA.env, testSvcA.name)).toEqual(testSvcA);
  });

  it('should report configuration history not_to_be_set for unknown configuration', () => {
    expect(service.isHistoryInitialized(testSvcA.env, testSvcA.name)).toEqual(false);
  });

  it('should report configuration history not_to_be_set initially', () => {
    service.setConfiguration(testSvcA);
    expect(service.isHistoryInitialized(testSvcA.env, testSvcA.name)).toEqual(false);
  });

  it('should report configuration history not_to_be_set after storing individual state', () => {
    service.setConfiguration(testSvcA);
    service.storeStatus(testStatusA10);
    expect(service.isHistoryInitialized(testSvcA.env, testSvcA.name)).toEqual(false);
  });

  it('should report configuration history yes_to_be_set after storing history', () => {
    service.setConfiguration(testSvcA);
    service.storeHistory(testSvcA.env, testSvcA.name, [testStatusA10, testStatusA12]);
    expect(service.isHistoryInitialized(testSvcA.env, testSvcA.name)).toEqual(true);
  });

  it('should provide visibleConfigurations as received', () => {
    service.setVisibleConfigurations([testSvcA, testSvcB]);
    expect(service.getVisibleConfigurations()).toEqual([testSvcA, testSvcB]);
  });

  it('should provide dependencies as received', () => {
    service.setConfiguration(testSvcA);
    service.setDependencies(testSvcA.env, testSvcA.name, [{ environment: 'testEnv', service: 'dependOnTestSvc' }]);
    expect(service.getDependencies(testSvcA.env, testSvcA.name)).toEqual([{ environment: 'testEnv', service: 'dependOnTestSvc' }]);
  });

  it('should provide dependents as received', () => {
    service.setConfiguration(testSvcA);
    service.setDependents(testSvcA.env, testSvcA.name, [{ environment: 'testEnv', service: 'testSvcDependOnMe' }]);
    expect(service.getDependents(testSvcA.env, testSvcA.name)).toEqual([{ environment: 'testEnv', service: 'testSvcDependOnMe' }]);
  });

  it('should provide statuses for given service - statuses stored individually', () => {
    service.setConfiguration(testSvcA);
    service.setConfiguration(testSvcB);
    service.storeStatus(testStatusA10);
    service.storeStatus(testStatusB11);
    service.storeStatus(testStatusA12);
    expect(service.getStatuses(testSvcA.env, testSvcA.name)).toEqual(expect.arrayContaining([testStatusA10, testStatusA12]));
  });

  it('should provide statuses for given service - statuses via history', () => {
    service.setConfiguration(testSvcA);
    service.storeHistory(testSvcA.env, testSvcA.name, [testStatusA10, testStatusA12]);
    expect(service.getStatuses(testSvcA.env, testSvcA.name)).toEqual(expect.arrayContaining([testStatusA10, testStatusA12]));
  });
});
