import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { HierarchicalServiceCardWithHistory, ServiceCard, ServiceCardWithHistory } from '../../models/service-card';
import { RepositoryService } from '../repository/repository.service';
import { Utils } from '../utils/utils';
import { ServiceConfigurationReference } from '../../models/service-configuration-reference';
import { ServiceConfiguration } from '../../models/service-configuration';

interface StackItem extends ServiceConfigurationReference {
  depth: number;
}

enum GraphDirection {
  DEPENDENCIES = 'DEPENDENCIES',
  DEPENDENTS = 'DEPENDENTS',
}

@Injectable({
  providedIn: 'root',
})
export class CardService {
  constructor(
    private utils: Utils,
    private repository: RepositoryService
  ) {}

  // Cards for latest statuses of visible ServiceConfigurations
  // -> Ordered by Environment × ServiceName alphabetically
  getLatestVisibleCards(): Observable<ServiceCard[]> {
    return this.repository.dataChanged$.pipe(
      switchMap(() => {
        const cards = this.repository.getVisibleConfigurations().flatMap(visibleConfiguration => {
          const maybeCardWithHistory = this.makeCardWithHistory(visibleConfiguration);
          return maybeCardWithHistory ? [maybeCardWithHistory] : [];
        });
        return of(this.sortedAlphabetically(cards));
      })
    );
  }

  // Cards for full history of statuses of visible ServiceConfigurations
  // -> Ordered by FirstSeen chronologically (newest first)
  getHistoryVisibleCards(): Observable<ServiceCard[]> {
    return this.repository.dataChanged$.pipe(
      switchMap(() => {
        const cards = this.repository
          .getVisibleConfigurations()
          .flatMap(visibleConfiguration =>
            this.repository
              .getStatuses(visibleConfiguration.env, visibleConfiguration.name)
              .map(status => this.utils.makeCard(visibleConfiguration, status))
          );
        return of(this.sortedChronologically(cards));
      })
    );
  }

  // Cards for on-demand DAG for single service - dependencies
  // -> Ordered by DAG relationship × Environment × ServiceName, repetition allowed
  getDependenciesGraphCards(env: string, serviceName: string): HierarchicalServiceCardWithHistory[] {
    return this.getGraphNodeCardsTailRec([{ environment: env, service: serviceName, depth: 0 }], [], GraphDirection.DEPENDENCIES);
  }

  // Cards for on-demand DAG for single service - dependents
  // -> Ordered by DAG relationship × Environment × ServiceName, repetition allowed
  getDependentsGraphCards(env: string, serviceName: string): HierarchicalServiceCardWithHistory[] {
    return this.getGraphNodeCardsTailRec([{ environment: env, service: serviceName, depth: 0 }], [], GraphDirection.DEPENDENTS);
  }

  // Cards for on-demand DAG for single service - history of all configurations in current DAG
  // -> Ordered by FirstSeen chronologically (newest first)
  getHistoryGraphCards(env: string, serviceName: string): ServiceCard[] {
    const cards = this.getGraphHistoryCardsTailRec([{ environment: env, service: serviceName }], new Set<string>(), []);
    return this.sortedChronologically(cards);
  }

  private getGraphNodeCardsTailRec(
    stack: StackItem[],
    interimResult: HierarchicalServiceCardWithHistory[],
    direction: GraphDirection
  ): HierarchicalServiceCardWithHistory[] {
    if (stack.length == 0) return interimResult;

    const item = stack.pop()!;

    const configuration = this.repository.getConfiguration(item.environment, item.service);
    const maybeCardWithHistory = this.makeCardWithHistory(configuration);
    if (maybeCardWithHistory) {
      const nodes =
        direction == GraphDirection.DEPENDENCIES
          ? this.repository.getDependencies(item.environment, item.service)
          : this.repository.getDependents(item.environment, item.service);

      interimResult.push({ ...maybeCardWithHistory, depth: item.depth });
      this.sortedRefsAlphabeticallyDescending(nodes).forEach(node => {
        stack.push({ ...node, depth: item.depth + 1 });
      });
    }
    return this.getGraphNodeCardsTailRec(stack, interimResult, direction);
  }

  private getGraphHistoryCardsTailRec(
    stack: ServiceConfigurationReference[],
    processed: Set<string>,
    interimResult: ServiceCard[]
  ): ServiceCard[] {
    if (stack.length == 0) return interimResult;

    const item = stack.pop()!;
    const id = `${item.environment}_${item.service}`;
    if (processed.has(id)) return this.getGraphHistoryCardsTailRec(stack, processed, interimResult);
    processed.add(id);

    const configuration = this.repository.getConfiguration(item.environment, item.service);
    const statuses = this.repository.getStatuses(item.environment, item.service);
    const dependencyNodes = this.repository.getDependencies(item.environment, item.service);
    const dependentNodes = this.repository.getDependents(item.environment, item.service);
    interimResult.push(...statuses.map(status => this.utils.makeCard(configuration, status)));
    stack.push(...dependencyNodes);
    stack.push(...dependentNodes);
    return this.getGraphHistoryCardsTailRec(stack, processed, interimResult);
  }

  // Service with no status yet is to be completely ignored (configuration being born)
  private makeCardWithHistory(configuration: ServiceConfiguration): ServiceCardWithHistory | null {
    const cards = this.sortedChronologically(
      this.repository.getStatuses(configuration.env, configuration.name).map(status => this.utils.makeCard(configuration, status))
    );
    return cards.length > 0 ? { ...this.utils.makeCard(configuration, cards[0].status), history: cards } : null;
  }

  private sortedRefsAlphabeticallyDescending(cards: ServiceConfigurationReference[]): ServiceConfigurationReference[] {
    return cards.sort((left, right) => (`${left.environment}_${left.service}` < `${right.environment}_${right.service}` ? 1 : -1));
  }

  private sortedAlphabetically<TCard extends ServiceCard>(cards: TCard[]): TCard[] {
    return cards.sort((left, right) => (left.id < right.id ? -1 : 1));
  }

  private sortedChronologically<TCard extends ServiceCard>(cards: TCard[]): TCard[] {
    return cards.sort((left, right) => (left.status.firstSeen < right.status.firstSeen ? 1 : -1));
  }
}
