import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { of } from 'rxjs';
import { CardService } from './card.service';
import { RepositoryService } from '../repository/repository.service';
import { makeTestConfiguration, makeTestStatus } from '../../test.data';
import { ServiceConfigurationReference } from '../../models/service-configuration-reference';

describe('CardService', () => {
  let service: CardService;
  let repositoryServiceMock: jest.Mocked<RepositoryService>;

  const testMarks = ['0', '1', '2', '3', '4', '5', '6'];
  const testSvcs = testMarks.map(mark => makeTestConfiguration(`TestSvc${mark}`));
  const testSvcRefs: ServiceConfigurationReference[] = testSvcs.map(testSvc => {
    return { environment: testSvc.env, service: testSvc.name };
  });
  const testStatuses10_0 = testMarks.map(mark =>
    makeTestStatus(`TestSvc${mark}`, undefined, `1989-05-29T10:0${mark}:00Z`, `1989-05-29T11:0${mark}:00Z`)
  );
  const testStatuses11_0 = testMarks.map(mark =>
    makeTestStatus(`TestSvc${mark}`, undefined, `1989-05-29T11:0${mark}:00Z`, `1989-05-29T12:0${mark}:00Z`)
  );

  beforeEach(() => {
    repositoryServiceMock = {
      getConfiguration: jest.fn(),
      getVisibleConfigurations: jest.fn(),
      getDependencies: jest.fn(),
      getDependents: jest.fn(),
      getStatuses: jest.fn(),
    } as never;
    Object.defineProperty(repositoryServiceMock, 'dataChanged$', {
      get: jest.fn(() => of(void 0)),
    });

    TestBed.configureTestingModule({
      providers: [CardService, { provide: RepositoryService, useValue: repositoryServiceMock }],
    });
    service = TestBed.inject(CardService);
  });

  it('getLatestVisibleCards should get alphabetically ordered cards with latest status for visible configurations', fakeAsync(() => {
    repositoryServiceMock.getVisibleConfigurations.mockImplementation(() => [testSvcs[1], testSvcs[0]]); // deliberately bad order
    repositoryServiceMock.getStatuses.mockImplementation((env: string, serviceName: string) =>
      serviceName == testSvcs[0].name ? [testStatuses11_0[0]] : [testStatuses11_0[1]]
    );

    service.getLatestVisibleCards().subscribe(latestCards => {
      expect(latestCards.length).toBe(2);
      expect(latestCards[0].configuration).toBe(testSvcs[0]);
      expect(latestCards[0].status).toBe(testStatuses11_0[0]);
      expect(latestCards[1].configuration).toBe(testSvcs[1]);
      expect(latestCards[1].status).toBe(testStatuses11_0[1]);
    });
    tick();
  }));

  it('getHistoryVisibleCards should get chronologically ordered cards with all/history statuses for visible configurations', fakeAsync(() => {
    repositoryServiceMock.getVisibleConfigurations.mockImplementation(() => [testSvcs[0], testSvcs[1]]);
    repositoryServiceMock.getStatuses.mockImplementation(
      (env: string, serviceName: string) =>
        serviceName == testSvcs[0].name ? [testStatuses10_0[0], testStatuses11_0[0]] : [testStatuses11_0[1], testStatuses10_0[1]] // deliberately swapped order
    );

    service.getHistoryVisibleCards().subscribe(historyCards => {
      expect(historyCards.length).toBe(4);
      expect(historyCards[0].configuration).toBe(testSvcs[1]);
      expect(historyCards[0].status).toBe(testStatuses11_0[1]);
      expect(historyCards[1].configuration).toBe(testSvcs[0]);
      expect(historyCards[1].status).toBe(testStatuses11_0[0]);
      expect(historyCards[2].configuration).toBe(testSvcs[1]);
      expect(historyCards[2].status).toBe(testStatuses10_0[1]);
      expect(historyCards[3].configuration).toBe(testSvcs[0]);
      expect(historyCards[3].status).toBe(testStatuses10_0[0]);
    });
    tick();
  }));

  it('getDependenciesGraphCards should get DAG ordered cards with latest status for dependencies', () => {
    repositoryServiceMock.getConfiguration.mockImplementation(
      (env: string, serviceName: string) => testSvcs.find(svc => svc.name === serviceName)!
    );
    repositoryServiceMock.getDependencies.mockImplementation((env: string, serviceName: string) =>
      serviceName == testSvcs[0].name
        ? [testSvcRefs[1], testSvcRefs[2]]
        : serviceName == testSvcs[1].name
          ? [testSvcRefs[3], testSvcRefs[2]]
          : []
    );
    repositoryServiceMock.getStatuses.mockImplementation((env: string, serviceName: string) =>
      serviceName == testSvcs[0].name
        ? [testStatuses11_0[0]]
        : serviceName == testSvcs[1].name
          ? [testStatuses11_0[1]]
          : serviceName == testSvcs[2].name
            ? [testStatuses11_0[2]]
            : [testStatuses11_0[3]]
    );

    const graphCards = service.getDependenciesGraphCards(testSvcs[0].env, testSvcs[0].name);
    expect(graphCards.length).toBe(5);
    expect(graphCards[0].configuration).toBe(testSvcs[0]);
    expect(graphCards[0].status).toBe(testStatuses11_0[0]);
    expect(graphCards[0].depth).toBe(0);
    expect(graphCards[1].configuration).toBe(testSvcs[1]);
    expect(graphCards[1].status).toBe(testStatuses11_0[1]);
    expect(graphCards[1].depth).toBe(1);
    expect(graphCards[2].configuration).toBe(testSvcs[2]);
    expect(graphCards[2].status).toBe(testStatuses11_0[2]);
    expect(graphCards[2].depth).toBe(2);
    expect(graphCards[3].configuration).toBe(testSvcs[3]);
    expect(graphCards[3].status).toBe(testStatuses11_0[3]);
    expect(graphCards[3].depth).toBe(2);
    expect(graphCards[4].configuration).toBe(testSvcs[2]);
    expect(graphCards[4].status).toBe(testStatuses11_0[2]);
    expect(graphCards[4].depth).toBe(1);
  });

  it('getDependentsGraphCards should get DAG ordered cards with latest status for dependents', () => {
    repositoryServiceMock.getConfiguration.mockImplementation(
      (env: string, serviceName: string) => testSvcs.find(svc => svc.name === serviceName)!
    );
    repositoryServiceMock.getDependents.mockImplementation((env: string, serviceName: string) =>
      serviceName == testSvcs[0].name
        ? [testSvcRefs[1], testSvcRefs[2]]
        : serviceName == testSvcs[1].name
          ? [testSvcRefs[3], testSvcRefs[2]] // deliberately swapped order
          : []
    );
    repositoryServiceMock.getStatuses.mockImplementation((env: string, serviceName: string) =>
      serviceName == testSvcs[0].name
        ? [testStatuses11_0[0]]
        : serviceName == testSvcs[1].name
          ? [testStatuses11_0[1]]
          : serviceName == testSvcs[2].name
            ? [testStatuses11_0[2]]
            : [testStatuses11_0[3]]
    );

    const graphCards = service.getDependentsGraphCards(testSvcs[0].env, testSvcs[0].name);
    expect(graphCards.length).toBe(5);
    expect(graphCards[0].configuration).toBe(testSvcs[0]);
    expect(graphCards[0].status).toBe(testStatuses11_0[0]);
    expect(graphCards[0].depth).toBe(0);
    expect(graphCards[1].configuration).toBe(testSvcs[1]);
    expect(graphCards[1].status).toBe(testStatuses11_0[1]);
    expect(graphCards[1].depth).toBe(1);
    expect(graphCards[2].configuration).toBe(testSvcs[2]);
    expect(graphCards[2].status).toBe(testStatuses11_0[2]);
    expect(graphCards[2].depth).toBe(2);
    expect(graphCards[3].configuration).toBe(testSvcs[3]);
    expect(graphCards[3].status).toBe(testStatuses11_0[3]);
    expect(graphCards[3].depth).toBe(2);
    expect(graphCards[4].configuration).toBe(testSvcs[2]);
    expect(graphCards[4].status).toBe(testStatuses11_0[2]);
    expect(graphCards[4].depth).toBe(1);
  });

  it('getHistoryGraphCards should get chronologically ordered cards with all/history status for DAG nodes', () => {
    repositoryServiceMock.getConfiguration.mockImplementation(
      (env: string, serviceName: string) => testSvcs.find(svc => svc.name === serviceName)!
    );
    repositoryServiceMock.getDependencies.mockImplementation((env: string, serviceName: string) =>
      serviceName == testSvcs[0].name
        ? [testSvcRefs[1], testSvcRefs[2]]
        : serviceName == testSvcs[1].name
          ? [testSvcRefs[3], testSvcRefs[2]] // deliberately swapped order
          : []
    );
    repositoryServiceMock.getDependents.mockImplementation((env: string, serviceName: string) =>
      serviceName == testSvcs[0].name
        ? [testSvcRefs[3], testSvcRefs[4]]
        : serviceName == testSvcs[1].name
          ? [testSvcRefs[5], testSvcRefs[3]]
          : []
    );
    repositoryServiceMock.getStatuses.mockImplementation((env: string, serviceName: string) =>
      serviceName == testSvcs[0].name
        ? [testStatuses10_0[0], testStatuses11_0[0]]
        : serviceName == testSvcs[1].name
          ? [testStatuses10_0[1], testStatuses11_0[1]]
          : serviceName == testSvcs[2].name
            ? [testStatuses11_0[2], testStatuses10_0[2]] // deliberately swapped order
            : serviceName == testSvcs[3].name
              ? [testStatuses10_0[3], testStatuses11_0[3]]
              : serviceName == testSvcs[4].name
                ? [testStatuses10_0[4], testStatuses11_0[4]]
                : [testStatuses11_0[5], testStatuses10_0[5]]
    );

    const graphCards = service.getHistoryGraphCards(testSvcs[0].env, testSvcs[0].name);
    expect(graphCards.length).toBe(12);
    expect(graphCards[0].configuration).toBe(testSvcs[5]);
    expect(graphCards[0].status).toBe(testStatuses11_0[5]);
    expect(graphCards[1].configuration).toBe(testSvcs[4]);
    expect(graphCards[1].status).toBe(testStatuses11_0[4]);
    expect(graphCards[2].configuration).toBe(testSvcs[3]);
    expect(graphCards[2].status).toBe(testStatuses11_0[3]);
    expect(graphCards[3].configuration).toBe(testSvcs[2]);
    expect(graphCards[3].status).toBe(testStatuses11_0[2]);
    expect(graphCards[4].configuration).toBe(testSvcs[1]);
    expect(graphCards[4].status).toBe(testStatuses11_0[1]);
    expect(graphCards[5].configuration).toBe(testSvcs[0]);
    expect(graphCards[5].status).toBe(testStatuses11_0[0]);
    expect(graphCards[6].configuration).toBe(testSvcs[5]);
    expect(graphCards[6].status).toBe(testStatuses10_0[5]);
    expect(graphCards[7].configuration).toBe(testSvcs[4]);
    expect(graphCards[7].status).toBe(testStatuses10_0[4]);
    expect(graphCards[8].configuration).toBe(testSvcs[3]);
    expect(graphCards[8].status).toBe(testStatuses10_0[3]);
    expect(graphCards[9].configuration).toBe(testSvcs[2]);
    expect(graphCards[9].status).toBe(testStatuses10_0[2]);
    expect(graphCards[10].configuration).toBe(testSvcs[1]);
    expect(graphCards[10].status).toBe(testStatuses10_0[1]);
    expect(graphCards[11].configuration).toBe(testSvcs[0]);
    expect(graphCards[11].status).toBe(testStatuses10_0[0]);
  });
});
