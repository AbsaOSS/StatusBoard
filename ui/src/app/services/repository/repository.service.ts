import { Injectable } from '@angular/core';
import { RefinedStatus } from '../../models/refined-status';
import { ServiceConfiguration } from '../../models/service-configuration';
import { Observable, Subject } from 'rxjs';
import { ServiceConfigurationReference } from '../../models/service-configuration-reference';

interface MemItem {
  configuration: ServiceConfiguration;
  dependencies: ServiceConfigurationReference[];
  dependents: ServiceConfigurationReference[];
  statuses: { [firstSeen: string]: RefinedStatus };
  historyInitialized: boolean;
}

@Injectable({ providedIn: 'root' })
export class RepositoryService {
  private visibleConfigurations: ServiceConfigurationReference[] = [];
  private readonly memory: { [env: string]: { [serviceName: string]: MemItem } } = {};
  private dataChanged = new Subject<void>();

  constructor() {}

  get dataChanged$(): Observable<void> {
    return this.dataChanged.asObservable();
  }

  getConfiguration(env: string, serviceName: string): ServiceConfiguration {
    return this.memory[env][serviceName].configuration;
  }

  isHistoryInitialized(env: string, serviceName: string): boolean {
    return this.memory[env]?.[serviceName]?.historyInitialized ?? false;
  }

  getVisibleConfigurations(): ServiceConfiguration[] {
    return this.visibleConfigurations.map(reference => this.memory[reference.environment][reference.service].configuration);
  }

  getDependencies(env: string, serviceName: string): ServiceConfigurationReference[] {
    return this.memory[env][serviceName].dependencies;
  }

  getDependents(env: string, serviceName: string): ServiceConfigurationReference[] {
    return this.memory[env][serviceName].dependents;
  }

  // Service configuration right after its creation might be temporarily status-less
  // For UI purposes: ignore it completely as we are racing with it being born
  getStatuses(env: string, serviceName: string): RefinedStatus[] {
    return Object.values(this.memory[env][serviceName].statuses);
  }

  // Servers as registration point for configuration into memory
  setConfiguration(configuration: ServiceConfiguration): void {
    if (!this.memory[configuration.env]) this.memory[configuration.env] = {};
    if (!this.memory[configuration.env][configuration.name]) {
      this.memory[configuration.env][configuration.name] = {
        configuration: configuration,
        dependencies: [],
        dependents: [],
        statuses: {},
        historyInitialized: false,
      };
    } else {
      this.memory[configuration.env][configuration.name].configuration = configuration;
    }
    this.dataChanged.next();
  }

  setVisibleConfigurations(visibleConfigurations: ServiceConfiguration[]): void {
    // Set (register) each individual configuration BEFORE exposing them as "visible" configurations
    visibleConfigurations.forEach(configuration => this.setConfiguration(configuration));
    this.visibleConfigurations = visibleConfigurations.map(configuration => {
      return { environment: configuration.env, service: configuration.name };
    });
    this.dataChanged.next();
  }

  setDependencies(env: string, serviceName: string, dependencies: ServiceConfigurationReference[]): void {
    this.memory[env][serviceName].dependencies = dependencies;
  }

  setDependents(env: string, serviceName: string, dependents: ServiceConfigurationReference[]): void {
    this.memory[env][serviceName].dependents = dependents;
  }

  // We might get status for unknown configuration from latest-statuses
  // just ignore it, as if it ever gets known, history would be initialized at that point
  storeStatus(status: RefinedStatus): void {
    const statusMap = this.memory[status.env]?.[status.serviceName]?.statuses;
    if (!statusMap) return;
    statusMap[status.firstSeen] = status;
    this.dataChanged.next();
  }

  storeHistory(env: string, serviceName: string, history: RefinedStatus[]): void {
    const memory = this.memory[env][serviceName];
    history.forEach(status => (memory.statuses[status.firstSeen] = status));
    memory.historyInitialized = true;
    this.dataChanged.next();
  }
}
