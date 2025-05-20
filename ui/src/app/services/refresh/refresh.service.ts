import { Injectable } from '@angular/core';
import { BehaviorSubject, concatMap, forkJoin, map, Observable, of } from 'rxjs';
import { finalize, tap } from 'rxjs/operators';
import { RepositoryService } from '../repository/repository.service';
import { BackendService } from '../backend/backend.service';
import { ServiceConfigurationReference } from '../../models/service-configuration-reference';
import { ServiceConfiguration } from '../../models/service-configuration';
import { RefinedStatus } from '../../models/refined-status';

const ERROR_RACING_REFRESH = 'ERROR: Requested refresh while refreshPending';

enum GraphDirection {
  DEPENDENCIES = 'DEPENDENCIES',
  DEPENDENTS = 'DEPENDENTS',
}

@Injectable({
  providedIn: 'root',
})
export class RefreshService {
  private readonly refreshPending = new BehaviorSubject<boolean>(false);

  constructor(
    private backend: BackendService,
    private repository: RepositoryService
  ) {}

  get refreshPending$(): Observable<boolean> {
    return this.refreshPending.asObservable();
  }

  initialRefresh(): Observable<void> {
    if (this.refreshPending.value) throw new Error(ERROR_RACING_REFRESH);

    this.refreshPending.next(true);
    return this.refreshVisibleConfigurationsWithHistoryInit().pipe(finalize(() => this.refreshPending.next(false)));
  }

  incrementalRefresh(): Observable<void> {
    if (this.refreshPending.value) throw new Error(ERROR_RACING_REFRESH);

    this.refreshPending.next(true);
    return this.refreshVisibleConfigurationsWithHistoryInit().pipe(
      concatMap(() => this.refreshLatestStatuses()),
      finalize(() => this.refreshPending.next(false))
    );
  }

  refreshForGraph(env: string, serviceName: string): Observable<void> {
    if (this.refreshPending.value) throw new Error(ERROR_RACING_REFRESH);

    this.refreshPending.next(true);
    const dependenciesReq = this.refreshForGraphTailRec(
      [{ environment: env, service: serviceName }],
      new Set<string>(),
      GraphDirection.DEPENDENCIES
    );
    const dependentsReq = this.refreshForGraphTailRec(
      [{ environment: env, service: serviceName }],
      new Set<string>(),
      GraphDirection.DEPENDENTS
    );
    return forkJoin([dependenciesReq, dependentsReq]).pipe(
      map(() => void 0),
      finalize(() => this.refreshPending.next(false))
    );
  }

  private refreshVisibleConfigurationsWithHistoryInit(): Observable<void> {
    return this.backend.getConfigurations(false).pipe(
      concatMap(visibleConfigurations => {
        this.repository.setVisibleConfigurations(visibleConfigurations);
        const historyInits = visibleConfigurations.map(visibleConfiguration =>
          this.initConfigurationHistory(visibleConfiguration.env, visibleConfiguration.name)
        );
        return forkJoin(historyInits);
      }),
      map(() => void 0)
    );
  }

  private refreshLatestStatuses(): Observable<void> {
    return this.backend.getLatestStatuses().pipe(
      tap(latestStatuses => latestStatuses.forEach(latestStatus => this.repository.storeStatus(latestStatus))),
      map(() => void 0)
    );
  }

  private refreshForGraphTailRec(
    stack: ServiceConfigurationReference[],
    processed: Set<string>,
    direction: GraphDirection
  ): Observable<void> {
    if (stack.length == 0) return of(void 0);

    const item = stack.pop()!;
    const id = `${item.environment}_${item.service}`;
    if (processed.has(id)) return this.refreshForGraphTailRec(stack, processed, direction);
    processed.add(id);

    const confReq = this.backend.getConfiguration(item.environment, item.service);
    const historyReq = this.repository.isHistoryInitialized(item.environment, item.service)
      ? of(undefined)
      : this.backend.getServiceHistory(item.environment, item.service);
    const graphEdgesReq =
      direction == GraphDirection.DEPENDENCIES
        ? this.backend.getDependencies(item.environment, item.service)
        : this.backend.getDependents(item.environment, item.service);

    return forkJoin([confReq, historyReq, graphEdgesReq]).pipe(
      concatMap(reqResults => {
        const configuration = reqResults[0] as ServiceConfiguration;
        const maybeHistory = reqResults[1] as RefinedStatus[];
        const graphEdges = reqResults[2] as ServiceConfigurationReference[];

        this.repository.setConfiguration(configuration);

        if (!this.repository.isHistoryInitialized(item.environment, item.service))
          this.repository.storeHistory(item.environment, item.service, maybeHistory);

        if (direction == GraphDirection.DEPENDENCIES) this.repository.setDependencies(item.environment, item.service, graphEdges);
        else this.repository.setDependents(item.environment, item.service, graphEdges);

        stack.push(...graphEdges);
        return this.refreshForGraphTailRec(stack, processed, direction);
      })
    );
  }

  private initConfigurationHistory(env: string, serviceName: string): Observable<void> {
    if (this.repository.isHistoryInitialized(env, serviceName)) return of(void 0);

    return this.backend.getServiceHistory(env, serviceName).pipe(
      tap(history => this.repository.storeHistory(env, serviceName, history)),
      map(() => void 0)
    );
  }
}
